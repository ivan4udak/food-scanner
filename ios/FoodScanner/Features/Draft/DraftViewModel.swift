import SwiftUI
import PhotosUI
import ImageIO

/// Источник кадра. Выбирается один раз и переиспользуется для всех слотов.
enum PhotoSource { case camera, library }

@MainActor
final class DraftViewModel: ObservableObject {

    /// Состояние слота (Блок 17: прогресс + ожидание сервера + очередь).
    enum SlotPhase: Equatable {
        case idle
        case queued                 // в очереди на загрузку
        case uploading(Double)      // 0…1 — доля отправленных байт
        case waitingServer          // байты отправлены, ждём ответ
        case done
        case failed
    }

    /// Превью держим только в памяти — на устройство ничего не сохраняем.
    @Published var images: [PhotoSlot: UIImage] = [:]
    @Published var phases: [PhotoSlot: SlotPhase] = [:]
    @Published var uploadedCount = 0
    @Published var requiredCount = PhotoSlot.required.count
    @Published var complete = false
    @Published var completing = false
    @Published var error: String?

    /// Сколько загрузок сейчас в работе/очереди (для общего индикатора).
    var activeUploads: Int {
        phases.values.filter {
            if case .done = $0 { return false }
            if case .idle = $0 { return false }
            if case .failed = $0 { return false }
            return true
        }.count
    }

    func phase(for slot: PhotoSlot) -> SlotPhase { phases[slot] ?? .idle }

    // Последовательная очередь загрузок.
    private var chain: Task<Void, Never> = Task {}

    /// Кадр с камеры → сжать → в очередь на загрузку. capturedAt = текущее время.
    func upload(image: UIImage, for slot: PhotoSlot,
                draftId: UUID, contributorId: UUID?, api: APIClient) {
        guard let contributorId else { return }
        images[slot] = image
        let data = ImageCompressor.compress(image)
        enqueue(slot) { [weak self] in
            await self?.send(slot: slot, data: data, filename: "camera.jpg", mime: "image/jpeg",
                             capturedAt: Date(), draftId: draftId, contributorId: contributorId, api: api)
        }
    }

    /// Кадр из галереи → EXIF до сжатия → в очередь.
    func upload(item: PhotosPickerItem, for slot: PhotoSlot,
                draftId: UUID, contributorId: UUID?, api: APIClient) {
        guard let contributorId else { return }
        phases[slot] = .queued
        Task { [weak self] in
            guard let self else { return }
            guard let raw = try? await item.loadTransferable(type: Data.self),
                  let img = UIImage(data: raw) else {
                self.phases[slot] = .failed; self.error = "Не удалось прочитать фото"; return
            }
            self.images[slot] = img
            let captured = Self.exifCaptureDate(from: raw)
            let data = ImageCompressor.compress(img)
            self.enqueue(slot) { [weak self] in
                await self?.send(slot: slot, data: data, filename: "gallery.jpg", mime: "image/jpeg",
                                 capturedAt: captured, draftId: draftId, contributorId: contributorId, api: api)
            }
        }
    }

    private func enqueue(_ slot: PhotoSlot, _ job: @escaping () async -> Void) {
        phases[slot] = .queued
        let previous = chain
        chain = Task { await previous.value; await job() }
    }

    private func send(slot: PhotoSlot, data: Data, filename: String, mime: String,
                      capturedAt: Date?, draftId: UUID, contributorId: UUID, api: APIClient) async {
        error = nil
        phases[slot] = .uploading(0)
        do {
            let res = try await api.addPhoto(
                draftId: draftId, contributorId: contributorId, type: slot.rawValue,
                imageData: data, filename: filename, mimeType: mime, capturedAt: capturedAt,
                onProgress: { [weak self] fraction in
                    Task { @MainActor in
                        guard let self else { return }
                        self.phases[slot] = fraction < 1 ? .uploading(fraction) : .waitingServer
                    }
                })
            phases[slot] = .done
            uploadedCount = res.uploadedCount
            requiredCount = res.requiredCount
            complete = res.complete
        } catch {
            phases[slot] = .failed
            images[slot] = nil
            self.error = (error as? APIError)?.errorDescription ?? error.localizedDescription
        }
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
}
