import SwiftUI
import UIKit

/// Блок 5: heartbeat каждые 5с + состояние соединения.
/// ONLINE: последний ping < 10с · DEGRADED: 10–20с · OFFLINE: > 20с.
@MainActor
final class ConnectionMonitor: ObservableObject {

    enum State {
        case connecting, online, degraded, offline
        var isOffline: Bool { self == .offline }
    }

    @Published private(set) var state: State = .connecting
    /// Краткое сообщение для «расширения острова» при информировании (как у Самоката).
    @Published var islandMessage: String?

    /// Поставщик актуального API-клиента (baseURL может меняться в настройках).
    var apiProvider: (() -> APIClient)?

    private var lastSuccess: Date?
    private var startedAt = Date()
    private var loop: Task<Void, Never>?

    func start() {
        guard loop == nil else { return }
        startedAt = Date()
        loop = Task { [weak self] in
            while !Task.isCancelled {
                await self?.tick()
                try? await Task.sleep(nanoseconds: 5_000_000_000)
            }
        }
    }

    func stop() {
        loop?.cancel()
        loop = nil
    }

    func tick() async {
        guard let api = apiProvider?() else { return }
        if await api.ping() { lastSuccess = Date() }
        recompute()
    }

    private func recompute() {
        let now = Date()
        let next: State
        if let last = lastSuccess {
            let e = now.timeIntervalSince(last)
            next = e < 10 ? .online : (e < 20 ? .degraded : .offline)
        } else {
            next = now.timeIntervalSince(startedAt) < 20 ? .connecting : .offline
        }
        transition(to: next)
    }

    private func transition(to next: State) {
        guard next != state else { return }
        let wasDown = state != .online
        state = next
        switch next {
        case .online:
            if wasDown { expandIsland("Связь восстановлена"); Haptics.success() }
        case .degraded:
            expandIsland("Соединение нестабильно")
        case .offline:
            expandIsland("Нет соединения"); Haptics.warning()
        case .connecting:
            break
        }
    }

    /// Раскрывает «остров» с сообщением на ~1.8с, затем сворачивает в кружок.
    private func expandIsland(_ message: String) {
        withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) { islandMessage = message }
        let shown = message
        Task {
            try? await Task.sleep(nanoseconds: 1_800_000_000)
            if islandMessage == shown {
                withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) { islandMessage = nil }
            }
        }
    }
}

/// Лёгкие тактильные отклики.
enum Haptics {
    static func success() { UINotificationFeedbackGenerator().notificationOccurred(.success) }
    static func warning() { UINotificationFeedbackGenerator().notificationOccurred(.warning) }
    static func tick()    { UIImpactFeedbackGenerator(style: .light).impactOccurred() }
}

/// Эвристика наличия Dynamic Island (верхний safe-area inset ≥ 59pt).
enum DeviceInfo {
    @MainActor static var hasDynamicIsland: Bool {
        guard let scene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene }).first,
              let window = scene.windows.first(where: { $0.isKeyWindow }) ?? scene.windows.first
        else { return false }
        return window.safeAreaInsets.top >= 59
    }
}
