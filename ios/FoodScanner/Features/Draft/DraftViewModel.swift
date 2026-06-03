import SwiftUI
import PhotosUI

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

    /// Кадр из камеры (UIImage в памяти) → сразу на сервер.
    func upload(image: UIImage, for slot: PhotoSlot,
                draftId: UUID, contributorId: UUID?, api: APIClient) async {
        guard let contributorId else { return }
        images[slot] = image
        await send(slot: slot, draftId: draftId, contributorId: contributorId, api: api)
    }

    /// Кадр из галереи (PhotosPickerItem) → читаем в память → на сервер.
    func upload(item: PhotosPickerItem, for slot: PhotoSlot,
                draftId: UUID, contributorId: UUID?, api: APIClient) async {
        guard let contributorId else { return }
        uploading.insert(slot)
        if let data = try? await item.loadTransferable(type: Data.self),
           let img = UIImage(data: data) {
            images[slot] = img
        }
        await send(slot: slot, draftId: draftId, contributorId: contributorId, api: api)
    }

    private func send(slot: PhotoSlot, draftId: UUID, contributorId: UUID, api: APIClient) async {
        error = nil
        uploading.insert(slot)
        defer { uploading.remove(slot) }

        let storageKey = "drafts/\(draftId.uuidString)/\(slot.rawValue)/\(UUID().uuidString).jpg"
        do {
            let res = try await api.addPhoto(draftId: draftId,
                                             contributorId: contributorId,
                                             type: slot.rawValue,
                                             storageKey: storageKey)
            uploaded.insert(slot)
            uploadedCount = res.uploadedCount
            requiredCount = res.requiredCount
            complete = res.complete
        } catch {
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
}
