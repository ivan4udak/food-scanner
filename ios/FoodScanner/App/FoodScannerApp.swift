import SwiftUI

@main
struct FoodScannerApp: App {
    @StateObject private var state = AppState()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(state)
                .tint(Theme.accent)
                .preferredColorScheme(.light)
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
        if state.isRegistered {
            ScanFlowView()
        } else {
            RegisterView()
        }
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
