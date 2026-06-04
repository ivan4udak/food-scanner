import SwiftUI
import UIKit

struct ScanView: View {
    @EnvironmentObject private var state: AppState
    @EnvironmentObject private var connection: ConnectionMonitor
    @Binding var path: NavigationPath

    /// Сканирование разрешено только при стабильном соединении (Блок 14, клиент).
    private var scanAllowed: Bool { connection.state == .online }

    @State private var access: CameraAccess = .undetermined
    @State private var cameraActive = false
    @State private var torchOn = false
    @State private var processing = false
    @State private var error: String?
    @State private var lastCode: String?
    @State private var showSettings = false

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
        .sheet(isPresented: $showSettings) { SettingsView() }
        .task { await prepareCamera() }
        .onAppear { resumeIfNeeded() }
        .onDisappear { cameraActive = false }
        // Пауза скана при потере/деградации связи; возобновление и снятие статуса при online.
        .onChange(of: connection.state) { _, _ in syncCamera() }
    }

    private func syncCamera() {
        cameraActive = (access == .authorized) && !processing && scanAllowed
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
                    // Минималистичная рамка: четыре угла с загибами. Без анимаций.
                    ScannerCorners()
                        .stroke(.white, style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))
                        .frame(width: side, height: side)
                        .shadow(color: .black.opacity(0.45), radius: 4)
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
                switch connection.state {
                case .offline:
                    statusPill("Нет подключения — сканер выключен", icon: "wifi.slash", tint: Theme.danger)
                case .degraded:
                    statusPill("Ожидание ответа сервера…", icon: "clock.arrow.circlepath", tint: Theme.warning)
                case .connecting:
                    statusPill("Подключение…", icon: "wifi", tint: .gray)
                case .online:
                    statusPill("Наведите камеру на штрихкод", icon: "viewfinder", tint: .white)
                }
            }
        }
    }

    private func statusPill(_ text: String, icon: String, tint: Color) -> some View {
        Label(text, systemImage: icon)
            .font(.subheadline.weight(.medium))
            .foregroundStyle(tint == .white ? Color.white : tint)
            .padding(.horizontal, 16).padding(.vertical, 12)
            .background(.ultraThinMaterial, in: Capsule())
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
            Button { showSettings = true } label: {
                Image(systemName: "gearshape").foregroundStyle(.white)
            }
        }
    }

    // MARK: Logic

    private func prepareCamera() async {
        switch CameraAccess.current {
        case .authorized:
            access = .authorized
        case .undetermined:
            let granted = await CameraAccess.request()
            access = granted ? .authorized : .denied
        case .denied:
            access = .denied
        case .unavailable:
            access = .unavailable
        }
        syncCamera()
    }

    private func resumeIfNeeded() {
        processing = false
        error = nil
        lastCode = nil
        syncCamera()
    }

    private func handle(code: String) {
        // Не сканируем без стабильного соединения (ждём, а не продолжаем).
        guard scanAllowed, !processing, code != lastCode,
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
                syncCamera()
            }
        }
    }
}

/// Четыре угловых уголка с загибами внутрь — поле сканирования.
private struct ScannerCorners: Shape {
    var len: CGFloat = 34
    var radius: CGFloat = 16

    func path(in r: CGRect) -> Path {
        var p = Path()
        // верхний левый
        p.move(to: CGPoint(x: r.minX, y: r.minY + len))
        p.addLine(to: CGPoint(x: r.minX, y: r.minY + radius))
        p.addQuadCurve(to: CGPoint(x: r.minX + radius, y: r.minY), control: CGPoint(x: r.minX, y: r.minY))
        p.addLine(to: CGPoint(x: r.minX + len, y: r.minY))
        // верхний правый
        p.move(to: CGPoint(x: r.maxX - len, y: r.minY))
        p.addLine(to: CGPoint(x: r.maxX - radius, y: r.minY))
        p.addQuadCurve(to: CGPoint(x: r.maxX, y: r.minY + radius), control: CGPoint(x: r.maxX, y: r.minY))
        p.addLine(to: CGPoint(x: r.maxX, y: r.minY + len))
        // нижний правый
        p.move(to: CGPoint(x: r.maxX, y: r.maxY - len))
        p.addLine(to: CGPoint(x: r.maxX, y: r.maxY - radius))
        p.addQuadCurve(to: CGPoint(x: r.maxX - radius, y: r.maxY), control: CGPoint(x: r.maxX, y: r.maxY))
        p.addLine(to: CGPoint(x: r.maxX - len, y: r.maxY))
        // нижний левый
        p.move(to: CGPoint(x: r.minX + len, y: r.maxY))
        p.addLine(to: CGPoint(x: r.minX + radius, y: r.maxY))
        p.addQuadCurve(to: CGPoint(x: r.minX, y: r.maxY - radius), control: CGPoint(x: r.minX, y: r.maxY))
        p.addLine(to: CGPoint(x: r.minX, y: r.maxY - len))
        return p
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
