import SwiftUI

// MARK: - Применение оверлея

extension View {
    /// Накладывает индикаторы соединения (Блок 6) поверх контента.
    func connectionOverlay(_ monitor: ConnectionMonitor) -> some View {
        modifier(ConnectionOverlayModifier(monitor: monitor))
    }
}

private struct ConnectionOverlayModifier: ViewModifier {
    @ObservedObject var monitor: ConnectionMonitor

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .top) { topLayer }
            .overlay { if monitor.state == .offline { OfflineBlocker() } }
            .animation(.easeInOut(duration: 0.3), value: monitor.state)
            .animation(.easeInOut(duration: 0.3), value: monitor.showConnectedBanner)
    }

    @ViewBuilder private var topLayer: some View {
        ZStack(alignment: .top) {
            if monitor.showConnectedBanner {
                ConnectedBanner().transition(.move(edge: .top).combined(with: .opacity))
            } else if monitor.state == .degraded {
                DegradedHeader().transition(.move(edge: .top).combined(with: .opacity))
            }
            IslandStatus(state: monitor.state)
        }
        .ignoresSafeArea(edges: .top)
    }
}

// MARK: - Dynamic Island статус (кружок состояния)

private struct IslandStatus: View {
    let state: ConnectionMonitor.State
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
        HStack(spacing: 7) {
            Circle()
                .fill(color)
                .frame(width: 9, height: 9)
                .overlay(Circle().stroke(color.opacity(0.5), lineWidth: 6)
                    .scaleEffect(pulse ? 1.8 : 1).opacity(pulse ? 0 : 0.6))
            if state != .online {
                Text(label).font(.caption2.weight(.semibold)).foregroundStyle(.white)
            }
        }
        .padding(.horizontal, state == .online ? 10 : 12)
        .padding(.vertical, 7)
        .background(Capsule().fill(.black))
        .overlay(Capsule().stroke(.white.opacity(0.12), lineWidth: 0.5))
        // На устройствах с Dynamic Island прижимаем к «острову», иначе чуть ниже статус-бара.
        .padding(.top, di ? 4 : 14)
        .onAppear {
            withAnimation(.easeOut(duration: 1.1).repeatForever(autoreverses: false)) { pulse = true }
        }
    }

    private var label: String {
        switch state {
        case .degraded:   return "Нестабильно"
        case .offline:    return "Нет сети"
        case .connecting: return "Подключение"
        case .online:     return ""
        }
    }
}

// MARK: - Зелёный баннер «Подключение установлено»

private struct ConnectedBanner: View {
    var body: some View {
        Text("Подключение установлено")
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(Color(hex: 0x0E7A3B))
            .frame(maxWidth: .infinity)
            .padding(.top, 54).padding(.bottom, 12)
            .background(Theme.accent.opacity(0.22))
    }
}

// MARK: - Жёлтая шапка «нестабильное соединение»

private struct DegradedHeader: View {
    var body: some View {
        HStack(spacing: 10) {
            RotatingBars(color: Theme.warning, length: 16, thickness: 4)
                .frame(width: 18, height: 18)
            Text("Нестабильное соединение")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Color(hex: 0x8A5800))
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 54).padding(.bottom, 12)
        .background(Theme.warning.opacity(0.22))
    }
}

// MARK: - OFFLINE: помутнение + блокировка + спиннер

private struct OfflineBlocker: View {
    var body: some View {
        ZStack {
            Rectangle().fill(.ultraThinMaterial).ignoresSafeArea()
            Color.black.opacity(0.5).ignoresSafeArea()

            VStack(spacing: 16) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 46)).foregroundStyle(Theme.danger)
                Text("Нет соединения с сервером")
                    .font(.headline).foregroundStyle(.white)
            }

            VStack {
                Spacer()
                RotatingBars(color: .gray, length: 30, thickness: 7)
                    .frame(width: 34, height: 34)
                Text("подключение...")
                    .font(.footnote).foregroundStyle(.white.opacity(0.8))
                    .padding(.top, 8)
                HStack(spacing: 8) {
                    ProgressView().tint(.white)
                    Text("Подключение к серверу")
                        .font(.subheadline.weight(.medium)).foregroundStyle(.white)
                }
                .padding(.horizontal, 22).padding(.vertical, 12)
                .background(Capsule().fill(.black.opacity(0.55)))
                .overlay(Capsule().stroke(.white.opacity(0.15)))
                .padding(.bottom, 28)
            }
        }
        // Полностью перехватывает касания — приложение недоступно.
        .contentShape(Rectangle())
        .onTapGesture {}
        .transition(.opacity)
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
