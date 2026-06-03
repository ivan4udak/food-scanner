import SwiftUI

@main
struct FoodScannerApp: App {
    @StateObject private var state = AppState()
    @StateObject private var connection = ConnectionMonitor()
    @StateObject private var busy = BusyController()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(state)
                .environmentObject(connection)
                .environmentObject(busy)
                .tint(Theme.accent)
                .preferredColorScheme(.light)
                .busyOverlay(busy)
                .connectionOverlay(connection)
                .task {
                    connection.apiProvider = { state.api }
                    connection.start()
                }
                .onChange(of: scenePhase) { _, phase in
                    switch phase {
                    case .active:                connection.start()
                    case .background, .inactive: connection.stop()
                    @unknown default:            break
                    }
                }
        }
    }
}

/// Маршруты внутри основного потока сканирования.
enum Route: Hashable {
    case draft(draftId: UUID, barcode: String)
    case exists(barcode: String)
    case completed(count: Int)
    case lookup(barcode: String)
}

struct RootView: View {
    @EnvironmentObject private var state: AppState

    var body: some View {
        ZStack {
            if state.isRegistered {
                ScanFlowView()
                    .transition(.opacity)
            } else {
                LoginView()
                    .transition(.opacity)
            }
        }
        // Плавная (без рывков) смена экрана после ответа сервера.
        .animation(.easeInOut(duration: 0.5), value: state.isRegistered)
    }
}

struct ScanFlowView: View {
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            ScanView(path: $path)
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case let .draft(draftId, barcode):
                        DraftView(draftId: draftId, barcode: barcode, path: $path)
                    case let .exists(barcode):
                        ExistsView(barcode: barcode, path: $path)
                    case let .completed(count):
                        CompletedView(count: count, path: $path)
                    case let .lookup(barcode):
                        LookupView(barcode: barcode)
                    }
                }
        }
    }
}
