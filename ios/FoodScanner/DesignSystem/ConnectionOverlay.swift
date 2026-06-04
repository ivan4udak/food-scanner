import SwiftUI

// MARK: - Применение оверлея

extension View {
    /// Накладывает индикаторы соединения (Блок 6, дизайн-ревизия).
    func connectionOverlay(_ monitor: ConnectionMonitor) -> some View {
        modifier(ConnectionOverlayModifier(monitor: monitor))
    }
}

private struct ConnectionOverlayModifier: ViewModifier {
    @ObservedObject var monitor: ConnectionMonitor

    func body(content: Content) -> some View {
        content
            // Флюидное кольцо вокруг Dynamic Island / камеры. Без текста.
            .overlay(alignment: .top) {
                IslandRing(state: monitor.state, connectedFlash: monitor.showConnectedBanner)
                    .ignoresSafeArea(edges: .top)
                    .allowsHitTesting(false)
            }
            .overlay { if monitor.state == .offline { OfflineBlocker(monitor: monitor) } }
            .animation(.easeInOut(duration: 0.35), value: monitor.state)
    }
}

// MARK: - Флюидное кольцо вокруг Dynamic Island

private struct IslandRing: View {
    let state: ConnectionMonitor.State
    let connectedFlash: Bool
    @State private var breathe = false

    private var color: Color {
        switch state {
        case .online:     return Theme.accent
        case .degraded:   return Theme.warning
        case .offline:    return Theme.danger
        case .connecting: return .gray
        }
    }

    /// Кольцо видно при проблеме всегда; при online — только короткой вспышкой при подключении.
    private var visible: Bool { state != .online || connectedFlash }

    var body: some View {
        let di = DeviceInfo.hasDynamicIsland
        // Габариты «острова» (iPhone с Dynamic Island) либо компактная капсула сверху.
        let w: CGFloat = di ? 134 : 66
        let h: CGFloat = di ? 40  : 12
        let topPad: CGFloat = di ? 9 : 6

        Capsule(style: .continuous)
            .stroke(color, lineWidth: 3.5)
            .frame(width: w + (breathe ? 7 : 2), height: h + (breathe ? 7 : 2))
            .shadow(color: color.opacity(0.85), radius: breathe ? 12 : 5)
            .opacity(visible ? (breathe ? 0.95 : 0.6) : 0)
            .padding(.top, topPad)
            .animation(.easeInOut(duration: 1.1).repeatForever(autoreverses: true), value: breathe)
            .animation(.easeInOut(duration: 0.5), value: visible)
            .animation(.easeInOut(duration: 0.4), value: color)
            .onAppear { breathe = true }
    }
}

// MARK: - OFFLINE: помутнение + блокировка + редактируемый адрес сервера

private struct OfflineBlocker: View {
    @ObservedObject var monitor: ConnectionMonitor
    @EnvironmentObject private var state: AppState

    @State private var editing = false
    @State private var draft = ""
    @State private var retryIn: Int?       // обратный отсчёт до повторного коннекта
    @FocusState private var focused: Bool

    var body: some View {
        ZStack {
            Rectangle().fill(.ultraThinMaterial).ignoresSafeArea()
            // Поглощает касания — приложение недоступно.
            Color.black.opacity(0.5).ignoresSafeArea()
                .contentShape(Rectangle())
                .onTapGesture { focused = false }

            VStack(spacing: 16) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 46)).foregroundStyle(Theme.danger)
                Text("Нет соединения с сервером")
                    .font(.headline).foregroundStyle(.white)
            }

            VStack(spacing: 10) {
                Spacer()
                RotatingBars(color: .gray, length: 30, thickness: 7)
                    .frame(width: 34, height: 34)
                Text(retryIn.map { "повтор через \($0) с" } ?? "подключение...")
                    .font(.footnote).foregroundStyle(.white.opacity(0.85))

                // Текущий адрес сервера — нажми, чтобы изменить.
                serverAddressBar
                    .padding(.bottom, 26)
            }
            .padding(.horizontal, Theme.pad)
        }
    }

    private var serverAddressBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "server.rack").foregroundStyle(.white.opacity(0.8))
            if editing {
                TextField("http://host:port", text: $draft)
                    .focused($focused)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                    .foregroundStyle(.white)
                    .submitLabel(.go)
                    .onSubmit { applyNewAddress() }
                Button("ОК") { applyNewAddress() }
                    .font(.subheadline.weight(.semibold)).foregroundStyle(Theme.accent)
            } else {
                Text(state.baseURLString)
                    .font(.subheadline.weight(.medium)).foregroundStyle(.white)
                    .lineLimit(1).truncationMode(.middle)
                Image(systemName: "pencil").font(.footnote).foregroundStyle(.white.opacity(0.7))
            }
        }
        .padding(.horizontal, 18).padding(.vertical, 12)
        .frame(maxWidth: .infinity)
        .background(Capsule().fill(.black.opacity(0.55)))
        .overlay(Capsule().stroke(.white.opacity(0.18)))
        .contentShape(Capsule())
        .onTapGesture {
            if !editing {
                draft = state.baseURLString
                editing = true
                focused = true
            }
        }
    }

    /// Применяет новый адрес и через 5 секунд пытается подключиться.
    private func applyNewAddress() {
        let value = draft.trimmingCharacters(in: .whitespaces)
        editing = false
        focused = false
        guard !value.isEmpty, value != state.baseURLString else { return }
        state.baseURLString = value
        startRetryCountdown()
    }

    private func startRetryCountdown() {
        retryIn = 5
        Task {
            for s in stride(from: 5, through: 1, by: -1) {
                retryIn = s
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
            retryIn = nil
            await monitor.tick()      // попытка коннекта по новому адресу
        }
    }
}

// MARK: - Две вращающиеся полоски

private struct RotatingBars: View {
    var color: Color
    var length: CGFloat = 26
    var thickness: CGFloat = 6
    @State private var angle: Double = 0

    var body: some View {
        ZStack {
            Capsule().fill(color).frame(width: length, height: thickness)
            Capsule().fill(color.opacity(0.55)).frame(width: length, height: thickness)
                .rotationEffect(.degrees(90))
        }
        .rotationEffect(.degrees(angle))
        .onAppear {
            withAnimation(.linear(duration: 0.9).repeatForever(autoreverses: false)) { angle = 360 }
        }
    }
}
