import SwiftUI
import SwiftData

/// Блок 19: экран настроек (через шестерёнку).
struct SettingsView: View {
    @EnvironmentObject private var state: AppState
    @EnvironmentObject private var connection: ConnectionMonitor
    @Environment(\.modelContext) private var ctx
    @Environment(\.dismiss) private var dismiss

    @State private var showServer = false
    @State private var cacheSize: Int64 = 0
    @State private var clearing = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Сервер") {
                    Button { showServer = true } label: {
                        row("Адрес", state.baseURLString, accessory: "chevron.right")
                    }.tint(Theme.textPrimary)
                    HStack {
                        Text("Статус сервера")
                        Spacer()
                        HStack(spacing: 6) {
                            Circle().fill(statusColor).frame(width: 9, height: 9)
                            Text(statusText).foregroundStyle(Theme.textSecondary)
                        }
                    }
                }

                Section("Аккаунт") {
                    row("Логин", state.nickname ?? "—")
                    row("Contributor ID", state.contributorId?.uuidString ?? "—")
                    Button(role: .destructive) { state.signOut(); dismiss() } label: {
                        Label("Выйти из аккаунта", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }

                Section("Приложение") {
                    row("Версия", AppInfo.version)
                    row("Сборка", AppInfo.build)
                    Button { clearCache() } label: {
                        HStack {
                            Label("Очистить кэш", systemImage: "trash")
                            Spacer()
                            if clearing { ProgressView() }
                            else { Text(ByteCountFormatter.string(fromByteCount: cacheSize, countStyle: .file))
                                    .foregroundStyle(Theme.textSecondary) }
                        }
                    }.tint(Theme.textPrimary)
                }
            }
            .navigationTitle("Настройки")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Готово") { dismiss() } } }
            .sheet(isPresented: $showServer) { ServerSettingsView() }
            .task { cacheSize = await ImageStore.shared.diskSize() }
        }
    }

    private func row(_ title: String, _ value: String, accessory: String? = nil) -> some View {
        HStack {
            Text(title)
            Spacer()
            Text(value).foregroundStyle(Theme.textSecondary).lineLimit(1).truncationMode(.middle)
            if let accessory { Image(systemName: accessory).font(.footnote).foregroundStyle(Theme.textSecondary) }
        }
    }

    private var statusColor: Color {
        switch connection.state {
        case .online: return Theme.accent
        case .degraded: return Theme.warning
        case .offline: return Theme.danger
        case .connecting: return .gray
        }
    }
    private var statusText: String {
        switch connection.state {
        case .online: return "В сети"
        case .degraded: return "Нестабильно"
        case .offline: return "Нет связи"
        case .connecting: return "Подключение…"
        }
    }

    private func clearCache() {
        clearing = true
        Task {
            _ = await ImageStore.shared.clear()
            try? ctx.delete(model: CachedEntry.self)   // офлайн-кэш каталога
            try? ctx.save()
            cacheSize = await ImageStore.shared.diskSize()
            clearing = false
        }
    }
}

/// Версия/сборка приложения из Info.plist.
enum AppInfo {
    static var version: String { Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "—" }
    static var build:   String { Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "—" }
}
