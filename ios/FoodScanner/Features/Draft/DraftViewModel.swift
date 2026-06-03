import SwiftUI
import PhotosUI
import ImageIO

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
        // Блок 8: сжать до ≤1920 + JPEG перед отправкой.
        let data = ImageCompressor.compress(image)
        await send(slot: slot, data: data, filename: "camera.jpg", mime: "image/jpeg",
                   capturedAt: Date(),
                   draftId: draftId, contributorId: contributorId, api: api)
    }

    /// Кадр из галереи: дату съёмки берём из EXIF оригинала, затем сжимаем (Блок 8).
    func upload(item: PhotosPickerItem, for slot: PhotoSlot,
                draftId: UUID, contributorId: UUID?, api: APIClient) async {
        guard let contributorId else { return }
        uploading.insert(slot)
        guard let raw = try? await item.loadTransferable(type: Data.self),
              let img = UIImage(data: raw) else {
            uploading.remove(slot)
            error = "Не удалось прочитать фото"
            return
        }
        images[slot] = img

        let captured = Self.exifCaptureDate(from: raw)   // EXIF до сжатия
        let data = ImageCompressor.compress(img)          // ≤1920 + JPEG

        await send(slot: slot, data: data, filename: "gallery.jpg", mime: "image/jpeg",
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
