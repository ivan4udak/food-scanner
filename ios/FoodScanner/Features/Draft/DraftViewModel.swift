import SwiftUI
import PhotosUI
import ImageIO
import UniformTypeIdentifiers

/// Источник кадра. Выбирается один раз и переиспользуется для всех слотов.
enum PhotoSource { case camera, library }

@MainActor
final class DraftViewModel: ObservableObject {

    enum CellState { case empty, uploading, uploaded }

    /// Превью держим только в памяти — на устройство ничего не сохраняем.
    @Published var images: [PhotoSlot: UIImage] = [:]
    @Published var uploaded: Set<PhotoSlot> = []
    @Published var uploading: Set<PhotoSlot> = []
    @Published var uploadedCount = 0
    @Published var requiredCount = PhotoSlot.required.count
    @Published var complete = false
    @Published var completing = false
    @Published var error: String?

    func cellState(for slot: PhotoSlot) -> CellState {
        if uploading.contains(slot) { return .uploading }
        if uploaded.contains(slot)  { return .uploaded }
        return .empty
    }

    /// Кадр с камеры (UIImage в памяти, на устройство не сохраняется) → JPEG → сервер.
    /// capturedAt для камеры = текущее время.
    func upload(image: UIImage, for slot: PhotoSlot,
                draftId: UUID, contributorId: UUID?, api: APIClient) async {
        guard let contributorId else { return }
        images[slot] = image
        let data = image.jpegData(compressionQuality: 0.9) ?? Data()
        await send(slot: slot, data: data, filename: "camera.jpg", mime: "image/jpeg",
                   capturedAt: Date(),
                   draftId: draftId, contributorId: contributorId, api: api)
    }

    /// Кадр из галереи (исходный файл) → читаем в память, исходный формат сохраняется →
    /// дату съёмки берём из EXIF → сервер.
    func upload(item: PhotosPickerItem, for slot: PhotoSlot,
                draftId: UUID, contributorId: UUID?, api: APIClient) async {
        guard let contributorId else { return }
        uploading.insert(slot)
        guard let data = try? await item.loadTransferable(type: Data.self) else {
            uploading.remove(slot)
            error = "Не удалось прочитать фото"
            return
        }
        if let img = UIImage(data: data) { images[slot] = img }

        let mime     = Self.mimeType(of: data)
        let ext      = Self.fileExtension(for: mime)
        let captured = Self.exifCaptureDate(from: data)

        await send(slot: slot, data: data, filename: "gallery.\(ext)", mime: mime,
                   capturedAt: captured,
                   draftId: draftId, contributorId: contributorId, api: api)
    }

    private func send(slot: PhotoSlot, data: Data, filename: String, mime: String,
                      capturedAt: Date?, draftId: UUID, contributorId: UUID, api: APIClient) async {
        error = nil
        uploading.insert(slot)
        defer { uploading.remove(slot) }
        do {
            let res = try await api.addPhoto(draftId: draftId,
                                             contributorId: contributorId,
                                             type: slot.rawValue,
                                             imageData: data,
                                             filename: filename,
                                             mimeType: mime,
                                             capturedAt: capturedAt)
            uploaded.insert(slot)
            uploadedCount = res.uploadedCount
            requiredCount = res.requiredCount
            complete = res.complete
        } catch {
            images[slot] = nil
            self.error = (error as? APIError)?.errorDescription ?? error.localizedDescription
        }
    }

    // MARK: - Метаданные

    /// Дата съёмки из EXIF (DateTimeOriginal). nil — если нет.
    private static func exifCaptureDate(from data: Data) -> Date? {
        guard let src = CGImageSourceCreateWithData(data as CFData, nil),
              let props = CGImageSourceCopyPropertiesAtIndex(src, 0, nil) as? [CFString: Any],
              let exif = props[kCGImagePropertyExifDictionary] as? [CFString: Any],
              let raw = (exif[kCGImagePropertyExifDateTimeOriginal]
                         ?? exif[kCGImagePropertyExifDateTimeDigitized]) as? String
        else { return nil }
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy:MM:dd HH:mm:ss"
        fmt.timeZone = .current
        return fmt.date(from: raw)
    }

    private static func mimeType(of data: Data) -> String {
        if let src = CGImageSourceCreateWithData(data as CFData, nil),
           let uti = CGImageSourceGetType(src),
           let mime = UTType(uti as String)?.preferredMIMEType {
            return mime
        }
        return "image/jpeg"
    }

    private static func fileExtension(for mime: String) -> String {
        UTType(mimeType: mime)?.preferredFilenameExtension ?? "jpg"
    }

    func complete(draftId: UUID, contributorId: UUID, api: APIClient) async -> Int? {
        completing = true; error = nil
        defer { completing = false }
        do {
            let res = try await api.complete(draftId: draftId, contributorId: contributorId)
            return res.contributorCompletedCount
        } catch {
            self.error = (error as? APIError)?.errorDescription ?? error.localizedDescription
            return nil
        }
    }
}
