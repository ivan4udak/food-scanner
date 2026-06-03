import SwiftUI
import UIKit

struct ScanView: View {
    @EnvironmentObject private var state: AppState
    @Binding var path: NavigationPath

    @State private var access: CameraAccess = .undetermined
    @State private var cameraActive = false
    @State private var torchOn = false
    @State private var processing = false
    @State private var error: String?
    @State private var lastCode: String?

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            switch access {
            case .authorized:    cameraLayer
            case .undetermined:  Color.black.ignoresSafeArea()
            case .denied:        DeniedView()
            case .unavailable:   SimulatorFallbackView { handle(code: $0) }
            }

            overlay
        }
        .toolbarBackground(.hidden, for: .navigationBar)
        .toolbar { toolbar }
        .task { await prepareCamera() }
        .onAppear { resumeIfNeeded() }
        .onDisappear { cameraActive = false }
    }

    // MARK: Camera

    private var cameraLayer: some View {
        BarcodeScannerView(isActive: $cameraActive, torchOn: torchOn) { code in
            handle(code: code)
        }
        .ignoresSafeArea()
    }

    // MARK: Overlay

    private var overlay: some View {
        GeometryReader { geo in
            let side = min(geo.size.width - 80, 280)
            ZStack {
                if access == .authorized {
                    // Мягкое затемнение с прозрачным «окном» по центру.
                    Rectangle()
                        .fill(Color.black.opacity(0.32))
                        .reverseMask {
                            RoundedRectangle(cornerRadius: 30, style: .continuous)
                                .frame(width: side, height: side)
                        }
                        .ignoresSafeArea()

                    RoundedRectangle(cornerRadius: 30, style: .continuous)
                        .strokeBorder(Color.white.opacity(0.55), lineWidth: 1.5)
                        .frame(width: side, height: side)
                }

                VStack {
                    Spacer()
                    captionBar
                        .padding(.horizontal, Theme.pad)
                        .padding(.bottom, 28)
                }

                if processing {
                    ZStack {
                        Color.black.opacity(0.45).ignoresSafeArea()
                        VStack(spacing: 14) {
                            ProgressView().controlSize(.large).tint(.white)
                            Text("Проверяем штрихкод…")
                                .font(.subheadline).foregroundStyle(.white)
                        }
                    }
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
    }

    private var captionBar: some View {
        VStack(spacing: 10) {
            if let error {
                ErrorBanner(message: error)
            }
            if access == .authorized {
                Label("Наведите камеру на штрихкод", systemImage: "viewfinder")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16).padding(.vertical, 12)
                    .background(.ultraThinMaterial, in: Capsule())
            }
        }
    }

    @ToolbarContentBuilder private var toolbar: some ToolbarContent {
        ToolbarItem(placement: .principal) {
            Text("Сканер").font(.headline).foregroundStyle(.white)
        }
        ToolbarItem(placement: .topBarLeading) {
            if access == .authorized {
                Button { torchOn.toggle() } label: {
                    Image(systemName: torchOn ? "bolt.fill" : "bolt.slash")
                        .foregroundStyle(.white)
                }
            }
        }
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                Label(state.nickname ?? "—", systemImage: "person")
                Button(role: .destructive) { state.signOut() } label: {
                    Label("Сменить профиль", systemImage: "arrow.left.arrow.right")
                }
            } label: {
                Image(systemName: "person.crop.circle").foregroundStyle(.white)
            }
        }
    }

    // MARK: Logic

    private func prepareCamera() async {
        switch CameraAccess.current {
        case .authorized:
            access = .authorized; cameraActive = true
        case .undetermined:
            let granted = await CameraAccess.request()
            access = granted ? .authorized : .denied
            cameraActive = granted
        case .denied:
            access = .denied
        case .unavailable:
            access = .unavailable
        }
    }

    private func resumeIfNeeded() {
        processing = false
        error = nil
        lastCode = nil
        if access == .authorized { cameraActive = true }
    }

    private func handle(code: String) {
        guard !processing, code != lastCode,
              let contributorId = state.contributorId else { return }
        lastCode = code
        processing = true
        cameraActive = false
        torchOn = false
        UINotificationFeedbackGenerator().notificationOccurred(.success)

        Task {
            do {
                let res = try await state.api.scan(barcode: code, contributorId: contributorId)
                if res.isNew, let draftId = res.draftId {
                    path.append(Route.draft(draftId: draftId, barcode: code))
                } else {
                    path.append(Route.exists(barcode: code))
                }
            } catch {
                self.error = (error as? APIError)?.errorDescription ?? error.localizedDescription
                processing = false
                lastCode = nil
                cameraActive = (access == .authorized)
            }
        }
    }
}

private extension View {
    /// Вырезает «окно» заданной формы из вьюхи (инвертированная маска).
    func reverseMask<Mask: View>(alignment: Alignment = .center,
                                 @ViewBuilder _ mask: () -> Mask) -> some View {
        self.mask {
            Rectangle()
                .overlay(alignment: alignment) {
                    mask().blendMode(.destinationOut)
                }
                .compositingGroup()
        }
    }
}

// MARK: - Permission denied

private struct DeniedView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "camera.metering.unknown")
                .font(.system(size: 52)).foregroundStyle(.white.opacity(0.8))
            Text("Нет доступа к камере")
                .font(.title3.weight(.semibold)).foregroundStyle(.white)
            Text("Разрешите доступ к камере в Настройках,\nчтобы сканировать штрихкоды")
                .font(.subheadline).foregroundStyle(.white.opacity(0.7))
                .multilineTextAlignment(.center)
            Button {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            } label: {
                Text("Открыть Настройки").fontWeight(.semibold)
                    .padding(.horizontal, 22).padding(.vertical, 12)
                    .background(Theme.accent, in: Capsule()).foregroundStyle(.white)
            }
        }
        .padding(Theme.pad)
    }
}

// MARK: - Simulator fallback (нет камеры)

private struct SimulatorFallbackView: View {
    let onCode: (String) -> Void
    @State private var code = ""

    var body: some View {
        VStack(spacing: 18) {
            Image(systemName: "barcode.viewfinder")
                .font(.system(size: 52)).foregroundStyle(Theme.accent)
            Text("Камера недоступна в симуляторе")
                .font(.headline).foregroundStyle(.white)
            Text("Для отладки введите штрихкод вручную")
                .font(.footnote).foregroundStyle(.white.opacity(0.7))
            TextField("4600000000000", text: $code)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .font(.system(size: 20, weight: .semibold, design: .rounded))
                .foregroundStyle(.white)
                .padding(.horizontal, 16).frame(height: 56)
                .background(.white.opacity(0.08), in: RoundedRectangle(cornerRadius: Theme.radiusSm))
                .overlay(RoundedRectangle(cornerRadius: Theme.radiusSm).stroke(.white.opacity(0.2)))
            Button {
                let c = code.trimmingCharacters(in: .whitespaces)
                if !c.isEmpty { onCode(c) }
            } label: {
                Text("Распознать").fontWeight(.semibold)
                    .frame(maxWidth: .infinity).frame(height: 52)
                    .background(Theme.accent, in: RoundedRectangle(cornerRadius: Theme.radiusSm))
                    .foregroundStyle(.white)
            }
        }
        .padding(Theme.pad)
    }
}
