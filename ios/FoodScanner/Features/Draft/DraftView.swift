import SwiftUI
import PhotosUI

struct DraftView: View {
    let draftId: UUID
    let barcode: String
    @Binding var path: NavigationPath

    @EnvironmentObject private var state: AppState
    @StateObject private var model = DraftViewModel()

    // Режим источника живёт в AppState (память сессии). Блоки 11–12.
    @State private var activeSlot: PhotoSlot?
    @State private var showCamera = false
    @State private var showLibrary = false
    @State private var pickerItem: PhotosPickerItem?

    private let columns = [GridItem(.flexible(), spacing: 14),
                           GridItem(.flexible(), spacing: 14)]

    var body: some View {
        Screen {
            ScrollView {
                VStack(spacing: 22) {
                    summary

                    section(title: "Обязательные", slots: PhotoSlot.required)
                    section(title: "Дополнительно · по желанию", slots: PhotoSlot.optional)

                    if let error = model.error { ErrorBanner(message: error) }

                    PrimaryButton(title: model.complete ? "Загрузить в каталог" : "Сделайте 4 обязательных фото",
                                  systemImage: model.complete ? "checkmark.seal.fill" : "camera.fill",
                                  loading: model.completing,
                                  enabled: model.complete) { complete() }
                }
                .padding(Theme.pad)
            }
        }
        .navigationTitle("Новый продукт")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Section("Режим загрузки фото") {
                        Button { state.photoSource = .camera } label: {
                            Label("Камера", systemImage: state.photoSource == .camera ? "checkmark" : "camera")
                        }
                        Button { state.photoSource = .library } label: {
                            Label("Галерея", systemImage: state.photoSource == .library ? "checkmark" : "photo.on.rectangle")
                        }
                    }
                } label: {
                    Image(systemName: "gearshape")
                }
            }
        }
        .fullScreenCover(isPresented: $showCamera) {
            CameraCapture { image in
                if let slot = activeSlot {
                    Task { await model.upload(image: image, for: slot,
                                              draftId: draftId,
                                              contributorId: state.contributorId,
                                              api: state.api) }
                }
            }
            .ignoresSafeArea()
        }
        .photosPicker(isPresented: $showLibrary, selection: $pickerItem, matching: .images)
        .onChange(of: pickerItem) { _, newItem in
            guard let newItem, let slot = activeSlot else { return }
            Task {
                await model.upload(item: newItem, for: slot,
                                   draftId: draftId,
                                   contributorId: state.contributorId,
                                   api: state.api)
                pickerItem = nil
            }
        }
    }

    // MARK: Sections

    private func section(title: String, slots: [PhotoSlot]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title.uppercased())
                .font(.caption.weight(.semibold))
                .foregroundStyle(Theme.textSecondary)
                .tracking(0.5)
            LazyVGrid(columns: columns, spacing: 14) {
                ForEach(slots) { slot in
                    PhotoSlotCell(slot: slot,
                                  image: model.images[slot],
                                  state: model.cellState(for: slot),
                                  hasSource: state.photoSource != nil,
                                  onChoose: { src in choose(src, for: slot) },
                                  onOpen:   { openExisting(for: slot) })
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var summary: some View {
        HStack(spacing: 20) {
            ProgressRing(value: model.uploadedCount, total: max(model.requiredCount, 1))
            VStack(alignment: .leading, spacing: 6) {
                Text("Штрихкод").font(.caption).foregroundStyle(Theme.textSecondary)
                Text(barcode).font(.system(.title3, design: .rounded).weight(.bold))
                    .foregroundStyle(Theme.textPrimary)
                if let src = state.photoSource {
                    Label(src == .camera ? "Режим: камера" : "Режим: галерея",
                          systemImage: src == .camera ? "camera" : "photo.on.rectangle")
                        .font(.caption.weight(.medium)).foregroundStyle(Theme.accent)
                } else {
                    Text("Нажмите на плитку — выберите способ")
                        .font(.footnote).foregroundStyle(Theme.textSecondary)
                }
            }
            Spacer(minLength: 0)
        }
        .card()
    }

    // MARK: Actions

    /// Выбор источника из меню у плитки (Блок 11): запоминаем на сессию и открываем.
    private func choose(_ src: PhotoSource, for slot: PhotoSlot) {
        state.photoSource = src
        activeSlot = slot
        open(src)
    }

    /// Режим уже задан (Блок 12) — открываем сразу.
    private func openExisting(for slot: PhotoSlot) {
        guard let src = state.photoSource else { return }
        activeSlot = slot
        open(src)
    }

    private func open(_ src: PhotoSource) {
        switch src {
        case .camera:
            if CameraCapture.isAvailable { showCamera = true }
            else { showLibrary = true } // симулятор без камеры
        case .library:
            showLibrary = true
        }
    }

    private func complete() {
        guard let contributorId = state.contributorId else { return }
        Task {
            if let count = await model.complete(draftId: draftId,
                                                contributorId: contributorId,
                                                api: state.api) {
                path.append(Route.completed(count: count))
            }
        }
    }
}

// MARK: - Cell

private struct PhotoSlotCell: View {
    let slot: PhotoSlot
    let image: UIImage?
    let state: DraftViewModel.CellState
    let hasSource: Bool
    let onChoose: (PhotoSource) -> Void
    let onOpen: () -> Void

    private var borderColor: Color {
        if state == .uploaded { return Theme.accent.opacity(0.6) }
        return slot.isRequired ? Theme.accent.opacity(0.30) : Theme.stroke
    }

    var body: some View {
        Group {
            if hasSource {
                // Режим задан → открываем источник сразу (Блок 12).
                Button(action: onOpen) { tile }.buttonStyle(.plain)
            } else {
                // Первый выбор → меню прямо у плитки/места касания (Блок 11).
                Menu {
                    Button { onChoose(.camera) }  label: { Label("Сделать фото", systemImage: "camera") }
                    Button { onChoose(.library) } label: { Label("Выбрать из галереи", systemImage: "photo.on.rectangle") }
                } label: { tile }
                .buttonStyle(.plain)
            }
        }
        .disabled(state == .uploading)
    }

    private var tile: some View {
        ZStack(alignment: .topTrailing) {
            content
            badge
        }
        .frame(maxWidth: .infinity)
        .aspectRatio(1, contentMode: .fit)
        .background(Theme.surface)
        .clipShape(RoundedRectangle(cornerRadius: Theme.radius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: Theme.radius, style: .continuous)
                .stroke(borderColor, lineWidth: state == .uploaded ? 2 : 1)
        )
    }

    @ViewBuilder private var content: some View {
        if let image {
            // Фото вписано в квадрат без деформации (scaledToFill + clip),
            // поверх — те же иконка/название/подсказка «под стеклом».
            ZStack {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
                // Матовое «стекло»: лёгкий скрим для читаемости подписей.
                Rectangle().fill(.ultraThinMaterial).opacity(0.42)
                LinearGradient(colors: [.white.opacity(0.18), .clear, .black.opacity(0.22)],
                               startPoint: .top, endPoint: .bottom)
                labels(onPhoto: true)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .clipped()
        } else {
            labels(onPhoto: false)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    /// Иконка + название + подсказка. Над фото — слегка прозрачные («под стеклом»).
    private func labels(onPhoto: Bool) -> some View {
        VStack(spacing: 8) {
            Image(systemName: slot.systemImage)
                .font(.system(size: 26, weight: .regular))
                .foregroundStyle(onPhoto ? Color.white
                                         : (slot.isRequired ? Theme.accent : Theme.textSecondary))
            Text(slot.title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(onPhoto ? Color.white : Theme.textPrimary)
            Text(slot.hint)
                .font(.caption2)
                .foregroundStyle(onPhoto ? Color.white.opacity(0.85) : Theme.textSecondary)
        }
        .multilineTextAlignment(.center)
        .padding(10)
        .opacity(onPhoto ? 0.92 : 1)
        .shadow(color: onPhoto ? .black.opacity(0.35) : .clear, radius: 3, y: 1)
    }

    @ViewBuilder private var badge: some View {
        switch state {
        case .uploaded:
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Theme.accent)
                .background(Circle().fill(.white))
                .padding(10)
        case .uploading:
            ProgressView().padding(12)
        case .empty:
            if slot.isRequired {
                Text("обязательно")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(Theme.accent)
                    .padding(.horizontal, 7).padding(.vertical, 3)
                    .background(Theme.accent.opacity(0.12), in: Capsule())
                    .padding(8)
            }
        }
    }
}
