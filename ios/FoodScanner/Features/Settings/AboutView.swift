import SwiftUI
import UIKit

/// Блок 20: экран «О приложении» + диагностический пакет.
/// Версии, адрес сервера, состояние связи/backend/MinIO, размер кэша
/// и кнопка «Скопировать диагностику» (для баг-репортов / TestFlight).
struct AboutView: View {
    @EnvironmentObject private var state: AppState
    @EnvironmentObject private var connection: ConnectionMonitor

    @State private var health: HealthResponse?
    @State private var loadingHealth = true
    @State private var cacheSize: Int64 = 0
    @State private var copied = false

    var body: some View {
        Form {
            Section("Версии") {
                row("iOS", UIDevice.current.systemVersion)
                row("Приложение", AppInfo.version)
                row("Сборка", AppInfo.build)
            }

            Section("Сервер и связь") {
                row("Backend URL", state.baseURLString)
                statusRow("Связь", connectionText, color: connectionColor)
                statusRow("Backend", backendText, color: backendColor)
                statusRow("Хранилище (MinIO)", storageText, color: storageColor)
            }

            Section("Кэш") {
                row("Размер на диске",
                    ByteCountFormatter.string(fromByteCount: cacheSize, countStyle: .file))
            }

            Section {
                Button { copyDiagnostics() } label: {
                    HStack {
                        Spacer()
                        Label(copied ? "Скопировано" : "Скопировать диагностику",
                              systemImage: copied ? "checkmark" : "doc.on.doc")
                        Spacer()
                    }
                }.tint(Theme.accent)
            } footer: {
                Text("Скопирует версии, адрес сервера и состояние связи — приложите к сообщению об ошибке.")
            }
        }
        .navigationTitle("О приложении")
        .navigationBarTitleDisplayMode(.inline)
        .task { await refresh() }
    }

    // MARK: Rows

    private func row(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title)
            Spacer()
            Text(value).foregroundStyle(Theme.textSecondary)
                .lineLimit(1).truncationMode(.middle)
                .textSelection(.enabled)
        }
    }

    private func statusRow(_ title: String, _ value: String, color: Color) -> some View {
        HStack {
            Text(title)
            Spacer()
            HStack(spacing: 6) {
                Circle().fill(color).frame(width: 9, height: 9)
                Text(value).foregroundStyle(Theme.textSecondary)
            }
        }
    }

    // MARK: Status mapping

    private var connectionText: String {
        switch connection.state {
        case .online: return "В сети"
        case .degraded: return "Нестабильно"
        case .offline: return "Нет связи"
        case .connecting: return "Подключение…"
        }
    }
    private var connectionColor: Color {
        switch connection.state {
        case .online: return Theme.accent
        case .degraded: return Theme.warning
        case .offline: return Theme.danger
        case .connecting: return .gray
        }
    }

    private var backendText: String {
        if loadingHealth { return "Проверка…" }
        return health == nil ? "Недоступен" : "Работает"
    }
    private var backendColor: Color {
        if loadingHealth { return .gray }
        return health == nil ? Theme.danger : Theme.accent
    }

    private var storageText: String {
        if loadingHealth { return "Проверка…" }
        guard let h = health else { return "—" }
        return h.storage == "UP" ? "Работает" : "Недоступно"
    }
    private var storageColor: Color {
        if loadingHealth { return .gray }
        guard let h = health else { return .gray }
        return h.storage == "UP" ? Theme.accent : Theme.danger
    }

    // MARK: Actions

    private func refresh() async {
        loadingHealth = true
        cacheSize = await ImageStore.shared.diskSize()
        health = await state.api.health()
        loadingHealth = false
    }

    private func copyDiagnostics() {
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm:ss"
        var lines = [
            "Food Scanner — диагностика",
            "Время: \(df.string(from: Date()))",
            "iOS: \(UIDevice.current.systemVersion)",
            "Приложение: \(AppInfo.version) (\(AppInfo.build))",
            "Backend URL: \(state.baseURLString)",
            "Связь: \(connectionText)",
            "Backend: \(backendText)",
            "Хранилище (MinIO): \(storageText)",
            "Кэш: \(ByteCountFormatter.string(fromByteCount: cacheSize, countStyle: .file))",
        ]
        lines.append("Логин: \(state.nickname ?? "—")")
        if let id = state.contributorId?.uuidString { lines.append("Contributor ID: \(id)") }

        UIPasteboard.general.string = lines.joined(separator: "\n")
        Haptics.tick()
        withAnimation { copied = true }
        Task { try? await Task.sleep(nanoseconds: 1_500_000_000); withAnimation { copied = false } }
    }
}
