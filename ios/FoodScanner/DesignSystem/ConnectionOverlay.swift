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
            // Статус-кружок справа от Dynamic Island + расширение при информировании.
            .overlay(alignment: .topTrailing) {
                IslandCompanion(state: monitor.state, message: monitor.islandMessage)
                    .allowsHitTesting(false)
            }
            .overlay { if monitor.state == .offline { OfflineBlocker(monitor: monitor) } }
            .animation(.easeInOut(duration: 0.35), value: monitor.state)
    }
}

// MARK: - Спутник Dynamic Island (кружок справа + расширение, стиль Самоката)

private struct IslandCompanion: View {
    let state: ConnectionMonitor.State
    let message: String?
    @State private var pulse = false

    private var color: Color {
        switch state {
        case .online:     return Theme.accent
        case .degraded:   return Theme.warning
        case .offline:    return Theme.danger
        case .connecting: return .gray
        }
    }

    var body: some View {
        let di = DeviceInfo.hasDynamicIsland
        let expanded = message != nil

        HStack(spacing: 8) {
            Circle()
                .fill(color)
                .frame(width: 10, height: 10)
                .overlay(
                    Circle().stroke(color.opacity(0.5), lineWidth: 5)
                        .scaleEffect(pulse ? 1.9 : 1).opacity(pulse ? 0 : 0.5)
                )
            if expanded {
                Text(message ?? "")
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .fixedSize()
                    .transition(.opacity.combined(with: .move(edge: .trailing)))
            }
        }
        .padding(.horizontal, expanded ? 12 : 8)
        .padding(.vertical, 7)
        .background(
            Capsule(style: .continuous)
                .fill(.black)
                .shadow(color: color.opacity(0.5), radius: expanded ? 8 : 3)
        )
        .overlay(Capsule().stroke(.white.opacity(0.12), lineWidth: 0.5))
        // Прижимаем к верху рядом с «островом» (справа). Адаптив под DI/ notch / плоский.
        .padding(.top, di ? 14 : 8)
        .padding(.trailing, di ? 16 : 12)
        .onAppear {
            withAnimation(.easeOut(duration: 1.2).repeatForever(autoreverses: false)) { pulse = true }
        }
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
