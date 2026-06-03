import Foundation

enum APIError: LocalizedError {
    case invalidURL
    case transport(Error)
    case decoding(Error)
    case server(status: Int, message: String, details: [String]?)
    case empty

    var errorDescription: String? {
        switch self {
        case .invalidURL:                       return "Некорректный адрес сервера"
        case .transport(let e):                 return "Нет связи с сервером: \(e.localizedDescription)"
        case .decoding:                         return "Не удалось разобрать ответ сервера"
        case .server(_, let message, let d):
            if let d, !d.isEmpty { return "\(message) — \(d.joined(separator: ", "))" }
            return message
        case .empty:                            return "Пустой ответ сервера"
        }
    }
}

/// Тонкий async/await клиент над REST API Food Scanner (`/api/v1`).
struct APIClient {
    var baseURL: URL

    private static let decoder: JSONDecoder = {
        let d = JSONDecoder()
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let isoPlain = ISO8601DateFormatter()
        isoPlain.formatOptions = [.withInternetDateTime]
        d.dateDecodingStrategy = .custom { decoder in
            let s = try decoder.singleValueContainer().decode(String.self)
            if let date = iso.date(from: s) ?? isoPlain.date(from: s) { return date }
            throw DecodingError.dataCorrupted(.init(codingPath: decoder.codingPath,
                                                    debugDescription: "Bad date: \(s)"))
        }
        return d
    }()

    private static let encoder = JSONEncoder()

    // MARK: Endpoints

    func register(nickname: String) async throws -> RegisterContributorResponse {
        try await post("contributors", body: RegisterContributorRequest(nickname: nickname))
    }

    func scan(barcode: String, contributorId: UUID) async throws -> ScanBarcodeResponse {
        try await post("scan", body: ScanBarcodeRequest(barcodeValue: barcode,
                                                         contributorId: contributorId))
    }

    /// Загружает БИНАРНОЕ фото (multipart). Исходный формат сохраняется как есть.
    /// capturedAt — дата съёмки из метаданных (для камеры можно передать текущее время или nil).
    func addPhoto(draftId: UUID, contributorId: UUID, type: String,
                  imageData: Data, filename: String, mimeType: String,
                  capturedAt: Date?) async throws -> AddDraftPhotoResponse {
        guard let url = URL(string: "drafts/\(draftId.uuidString)/photos", relativeTo: apiRoot) else {
            throw APIError.invalidURL
        }
        let boundary = "FSBoundary-\(UUID().uuidString)"
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        req.setValue("application/json", forHTTPHeaderField: "Accept")

        var body = Data()
        func field(_ name: String, _ value: String) {
            body.append("--\(boundary)\r\n")
            body.append("Content-Disposition: form-data; name=\"\(name)\"\r\n\r\n")
            body.append("\(value)\r\n")
        }
        field("contributorId", contributorId.uuidString)
        field("photoType", type)
        if let capturedAt {
            let iso = ISO8601DateFormatter()
            iso.formatOptions = [.withInternetDateTime]
            field("capturedAt", iso.string(from: capturedAt))
        }
        body.append("--\(boundary)\r\n")
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"\(filename)\"\r\n")
        body.append("Content-Type: \(mimeType)\r\n\r\n")
        body.append(imageData)
        body.append("\r\n")
        body.append("--\(boundary)--\r\n")
        req.httpBody = body

        return try await send(req, allow404: false)!
    }

    /// URL фото. thumbnail=true → лёгкое превью (~144px), иначе full (≤1920).
    func photoURL(storageKey: String, thumbnail: Bool = false) -> URL? {
        let path = thumbnail ? "photos/\(storageKey)?size=thumb" : "photos/\(storageKey)"
        return URL(string: path, relativeTo: apiRoot)
    }

    /// Скачивает байты фото (для кэша). thumbnail=true → превью.
    func photoData(storageKey: String, thumbnail: Bool) async throws -> Data {
        guard let url = photoURL(storageKey: storageKey, thumbnail: thumbnail) else {
            throw APIError.invalidURL
        }
        do {
            let (data, response) = try await URLSession.shared.data(from: url)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                throw APIError.server(status: (response as? HTTPURLResponse)?.statusCode ?? -1,
                                      message: "Не удалось загрузить фото", details: nil)
            }
            return data
        } catch let e as APIError {
            throw e
        } catch {
            throw APIError.transport(error)
        }
    }

    func complete(draftId: UUID, contributorId: UUID) async throws -> CompleteCatalogResponse {
        try await post("drafts/\(draftId.uuidString)/complete",
                       body: CompleteCatalogRequest(contributorId: contributorId))
    }

    /// Возвращает nil, если запись не найдена (404).
    func entry(barcode: String) async throws -> CatalogEntryResponse? {
        try await get("entries/\(barcode)", allow404: true)
    }

    /// Heartbeat: true если сервер ответил 2xx за отведённое время.
    func ping(timeout: TimeInterval = 4) async -> Bool {
        guard let url = URL(string: "ping", relativeTo: apiRoot) else { return false }
        var req = URLRequest(url: url)
        req.timeoutInterval = timeout
        req.cachePolicy = .reloadIgnoringLocalCacheData
        do {
            let (_, resp) = try await URLSession.shared.data(for: req)
            return (resp as? HTTPURLResponse).map { (200..<300).contains($0.statusCode) } ?? false
        } catch {
            return false
        }
    }

    // MARK: Core

    private func post<B: Encodable, R: Decodable>(_ path: String, body: B) async throws -> R {
        guard let url = URL(string: path, relativeTo: apiRoot) else { throw APIError.invalidURL }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        req.httpBody = try Self.encoder.encode(body)
        return try await send(req, allow404: false)!
    }

    private func get<R: Decodable>(_ path: String, allow404: Bool) async throws -> R? {
        guard let url = URL(string: path, relativeTo: apiRoot) else { throw APIError.invalidURL }
        var req = URLRequest(url: url)
        req.httpMethod = "GET"
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        return try await send(req, allow404: allow404)
    }

    private func send<R: Decodable>(_ request: URLRequest, allow404: Bool) async throws -> R? {
        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            throw APIError.transport(error)
        }
        guard let http = response as? HTTPURLResponse else { throw APIError.empty }

        if http.statusCode == 404 && allow404 { return nil }

        guard (200..<300).contains(http.statusCode) else {
            if let err = try? Self.decoder.decode(ServerErrorResponse.self, from: data) {
                throw APIError.server(status: err.status, message: err.message, details: err.details)
            }
            let raw = String(data: data, encoding: .utf8) ?? "HTTP \(http.statusCode)"
            throw APIError.server(status: http.statusCode, message: raw, details: nil)
        }

        guard !data.isEmpty else { throw APIError.empty }
        do {
            return try Self.decoder.decode(R.self, from: data)
        } catch {
            throw APIError.decoding(error)
        }
    }

    private var apiRoot: URL {
        baseURL.appendingPathComponent("api/v1/")
    }
}

private extension Data {
    mutating func append(_ string: String) {
        if let d = string.data(using: .utf8) { append(d) }
    }
}
