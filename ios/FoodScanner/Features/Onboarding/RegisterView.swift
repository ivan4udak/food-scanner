import SwiftUI

struct RegisterView: View {
    @EnvironmentObject private var state: AppState
    @State private var nickname = ""
    @State private var loading = false
    @State private var error: String?
    @State private var showServerSheet = false
    @FocusState private var focused: Bool

    private var canSubmit: Bool {
        let t = nickname.trimmingCharacters(in: .whitespaces)
        return t.count >= 2 && t.count <= 100
    }

    var body: some View {
        Screen {
            VStack(spacing: 28) {
                Spacer()

                VStack(spacing: 14) {
                    Image(systemName: "barcode.viewfinder")
                        .font(.system(size: 56, weight: .regular))
                        .foregroundStyle(Theme.accent)
                    Text("Food Scanner")
                        .font(.system(size: 30, weight: .bold, design: .rounded))
                        .foregroundStyle(Theme.textPrimary)
                    Text("Каталогизируйте продукты по штрихкоду")
                        .font(.subheadline)
                        .foregroundStyle(Theme.textSecondary)
                        .multilineTextAlignment(.center)
                }

                VStack(alignment: .leading, spacing: 14) {
                    Text("Ваш никнейм").font(.subheadline.weight(.medium))
                        .foregroundStyle(Theme.textSecondary)
                    TextField("например, ivan", text: $nickname)
                        .focused($focused)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .submitLabel(.go)
                        .onSubmit { if canSubmit { submit() } }
                        .padding(.horizontal, 16).frame(height: 54)
                        .background(Theme.background, in: RoundedRectangle(cornerRadius: Theme.radiusSm))
                        .overlay(RoundedRectangle(cornerRadius: Theme.radiusSm).stroke(Theme.stroke))

                    if let error { ErrorBanner(message: error) }

                    PrimaryButton(title: "Начать", systemImage: "arrow.right",
                                  loading: loading, enabled: canSubmit) { submit() }
                }
                .card()

                Spacer()

                Button { showServerSheet = true } label: {
                    Label(state.baseURLString, systemImage: "network")
                        .font(.footnote).foregroundStyle(Theme.textSecondary)
                }
            }
            .padding(Theme.pad)
        }
        .onAppear { focused = true }
        .sheet(isPresented: $showServerSheet) { ServerSettingsView() }
    }

    private func submit() {
        let name = nickname.trimmingCharacters(in: .whitespaces)
        loading = true; error = nil
        Task {
            do {
                let res = try await state.api.register(nickname: name)
                state.save(contributorId: res.contributorId, nickname: res.nickname)
            } catch {
                self.error = (error as? APIError)?.errorDescription ?? error.localizedDescription
            }
            loading = false
        }
    }
}

/// Настройка адреса сервера (полезно для запуска на устройстве).
struct ServerSettingsView: View {
    @EnvironmentObject private var state: AppState
    @Environment(\.dismiss) private var dismiss
    @State private var draft = ""

    var body: some View {
        NavigationStack {
            Screen {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Базовый адрес backend").font(.subheadline.weight(.medium))
                        .foregroundStyle(Theme.textSecondary)
                    TextField("http://localhost:8080", text: $draft)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .padding(.horizontal, 16).frame(height: 54)
                        .background(Theme.surface, in: RoundedRectangle(cornerRadius: Theme.radiusSm))
                        .overlay(RoundedRectangle(cornerRadius: Theme.radiusSm).stroke(Theme.stroke))
                    Text("Симулятор видит хост-машину как localhost. Для реального устройства укажите IP в сети, напр. http://192.168.1.10:8080")
                        .font(.footnote).foregroundStyle(Theme.textSecondary)
                    Spacer()
                    PrimaryButton(title: "Сохранить") {
                        let t = draft.trimmingCharacters(in: .whitespaces)
                        if !t.isEmpty { state.baseURLString = t }
                        dismiss()
                    }
                }
                .padding(Theme.pad)
            }
            .navigationTitle("Сервер")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) {
                Button("Отмена") { dismiss() } } }
        }
        .onAppear { draft = state.baseURLString }
    }
}
