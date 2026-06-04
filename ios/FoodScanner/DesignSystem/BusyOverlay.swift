import SwiftUI

/// Глобальное состояние ожидания ответа сервера.
/// Пока busy=true — экран замутняется (тем сильнее, чем дольше ждём),
/// анимации замедляются; следующий экран раскрывается после ответа.
@MainActor
final class BusyController: ObservableObject {
    @Published private(set) var busy = false
    @Published private(set) var startedAt = Date()

    func begin() {
        startedAt = Date()
        withAnimation(.easeInOut(duration: 0.35)) { busy = true }
    }

    func end() {
        withAnimation(.easeInOut(duration: 0.45)) { busy = false }
    }

    /// Выполняет асинхронную работу под индикатором ожидания.
    func run<T>(_ work: () async throws -> T) async rethrows -> T {
        begin()
        defer { end() }
        return try await work()
    }
}

extension View {
    func busyOverlay(_ controller: BusyController) -> some View {
        modifier(BusyOverlayModifier(controller: controller))
    }
}

private struct BusyOverlayModifier: ViewModifier {
    @ObservedObject var controller: BusyController

    // Предельные значения и скорость нарастания.
    private let maxBlur: CGFloat = 14
    private let maxDim:  Double  = 0.45
    private let rampSeconds: Double = 4   // за сколько секунд доходит до максимума

    func body(content: Content) -> some View {
        if controller.busy {
            TimelineView(.animation) { ctx in
                let t = max(0, ctx.date.timeIntervalSince(controller.startedAt))
                let p = min(1, t / rampSeconds)                  // 0…1
                let blur = maxBlur * easeOut(p)
                let dim  = maxDim  * easeOut(p)
                ZStack {
                    content
                        .blur(radius: blur)
                        .allowsHitTesting(false)
                    Color.black.opacity(dim).ignoresSafeArea()
                    SlowSpinner(elapsed: t)
                }
            }
            .transition(.opacity)
        } else {
            content
        }
    }

    private func easeOut(_ x: Double) -> Double { 1 - pow(1 - x, 2) }
}

/// Спиннер, который вращается всё медленнее со временем (df/dt убывает).
private struct SlowSpinner: View {
    let elapsed: Double

    var body: some View {
        // угол ~ sqrt(t): скорость вращения падает по мере ожидания.
        let angle = 220 * sqrt(max(0, elapsed))
        Circle()
            .trim(from: 0, to: 0.72)
            .stroke(Theme.accent, style: StrokeStyle(lineWidth: 4, lineCap: .round))
            .frame(width: 46, height: 46)
            .rotationEffect(.degrees(angle))
            .shadow(color: .black.opacity(0.25), radius: 6)
    }
}
