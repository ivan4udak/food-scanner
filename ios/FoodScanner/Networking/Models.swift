import Foundation

// MARK: - Requests

struct RegisterContributorRequest: Encodable {
    let nickname: String
}

struct ScanBarcodeRequest: Encodable {
    let barcodeValue: String
    let contributorId: UUID
}

struct AddDraftPhotoRequest: Encodable {
    let contributorId: UUID
    let photoType: String
    let storageKey: String
}

struct CompleteCatalogRequest: Encodable {
    let contributorId: UUID
}

// MARK: - Responses

struct RegisterContributorResponse: Decodable {
    let contributorId: UUID
    let nickname: String
}

struct ScanBarcodeResponse: Decodable {
    let status: String        // "NEW" | "EXISTS"
    let draftId: UUID?

    var isNew: Bool { status.uppercased() == "NEW" }
}

struct AddDraftPhotoResponse: Decodable {
    let uploadedCount: Int
    let requiredCount: Int
    let missingTypes: Set<String>
    let complete: Bool
}

struct CompleteCatalogResponse: Decodable {
    let catalogEntryId: UUID
    let contributorCompletedCount: Int
}

struct CatalogEntryResponse: Decodable, Identifiable {
    let id: UUID
    let barcode: String
    let contributorId: UUID
    let photos: [Photo]
    let createdAt: Date

    struct Photo: Decodable, Identifiable {
        let id: UUID
        let type: String
        let storageKey: String
    }
}

struct AuthResponse: Decodable {
    let status: String
    let contributorId: UUID?
    let username: String?
    let accessToken: String?
    let refreshToken: String?
    let message: String?
}

/// Успешная аутентификация: профиль + пара токенов.
struct Session {
    let contributorId: UUID
    let username: String
    let accessToken: String
    let refreshToken: String
}

struct ServerErrorResponse: Decodable {
    let status: Int
    let error: String
    let message: String
    let details: [String]?
}
