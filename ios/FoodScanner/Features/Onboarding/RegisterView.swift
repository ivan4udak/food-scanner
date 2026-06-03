import SwiftUI

/// Экран входа (Блок 1). Шаги: вход → подтверждение создания → восстановление.
struct LoginView: View {
    @EnvironmentObject private var state: AppState
    @EnvironmentObject private var busy: BusyController

    enum Step { case login, createConfirm, recovery }

    @State private var step: Step = .login
    @State private var username = ""
    @State private var password = ""
    @State private var confirm  = ""
    @State private var error: String?
    @State private var showServerSheet = false
    @FocusState private var focus: Field?
    private enum Field { case username, password, confirm }

    private var canLogin: Bool {
        !username.trimmingCharacters(in: .whitespaces).isEmpty && password.count >= 1
    }

    var body: some View {
        Screen {
            VStack(spacing: 26) {
                Spacer()
                header
                card
                Spacer()
                Button { showServerSheet = true } label: {
                    Label(state.baseURLString, systemImage: "network")
                        .font(.footnote).foregroundStyle(Theme.textSecondary)
                }
            }
            .padding(Theme.pad)
            .animation(.easeInOut(duration: 0.45), value: step)
        }
        .sheet(isPresented: $showServerSheet) { ServerSettingsView() }
        .onAppear { focus = .username }
    }

    private var header: some View {
        VStack(spacing: 14) {
            Image(systemName: "barcode.viewfinder")
                .font(.system(size: 54)).foregroundStyle(Theme.accent)
            Text("Food Scanner")
                .font(.system(size: 30, weight: .bold, design: .rounded))
                .foregroundStyle(Theme.textPrimary)
            Text(subtitle).font(.subheadline).foregroundStyle(Theme.textSecondary)
                .multilineTextAlignment(.center)
        }
    }

    private var subtitle: String {
        switch step {
        case .login:         return "Войдите или создайте аккаунт"
        case .createConfirm: return "Аккаунта нет — создадим новый"
        case .recovery:      return "Восстановление: задайте новый пароль"
        }
    }

    @ViewBuilder private var card: some View {
        VStack(spacing: 14) {
            field("Логин", text: $username, field: .username, secure: false)
                .disabled(step != .login)
                .opacity(step == .login ? 1 : 0.6)

            switch step {
            case .login:
                field("Пароль", text: $password, field: .password, secure: true)
            case .createConfirm:
                field("Пароль", text: $password, field: .password, secure: true)
                field("Подтвердите пароль", text: $confirm, field: .confirm, secure: true)
            case .recovery:
                field("Новый пароль", text: $password, field: .password, secure: true)
                field("Подтвердите пароль", text: $confirm, field: .confirm, secure: true)
            }

            if let error { ErrorBanner(message: error) }

            actions
        }
        .card()
    }

    @ViewBuilder private var actions: some View {
        switch step {
        case .login:
            PrimaryButton(title: "Войти", systemImage: "arrow.right", enabled: canLogin) { submitLogin() }
        case .createConfirm:
            PrimaryButton(title: "Создать аккаунт", systemImage: "person.badge.plus",
                          enabled: password.count >= 4 && !confirm.isEmpty) { submitCreate() }
            SecondaryButton(title: "Отмена") { reset() }
        case .recovery:
            PrimaryButton(title: "Сохранить пароль", systemImage: "checkmark",
                          enabled: password.count >= 4 && !confirm.isEmpty) { submitRecover() }
            SecondaryButton(title: "Отмена") { reset() }
        }
    }

    private func field(_ title: String, text: Binding<String>, field: Field, secure: Bool) -> some View {
        Group {
            if secure {
                SecureField(title, text: text)
            } else {
                TextField(title, text: text)
                    .textInputAutocapitalization(.never).autocorrectionDisabled()
            }
        }
        .focused($focus, equals: field)
        .padding(.horizontal, 16).frame(height: 52)
        .background(Theme.background, in: RoundedRectangle(cornerRadius: Theme.radiusSm))
        .overlay(RoundedRectangle(cornerRadius: Theme.radiusSm).stroke(Theme.stroke))
    }

    // MARK: Actions

    private func submitLogin() {
        let user = username.trimmingCharacters(in: .whitespaces)
        focus = nil; error = nil
        Task {
            do {
                let outcome = try await busy.run { try await state.api.login(username: user, password: password) }
                switch outcome {
                case let .ok(id, name):
                    Haptics.success()
                    state.save(contributorId: id, nickname: name)   // RootView сам сменит экран
                case .recovery:
                    confirm = ""; password = ""
                    step = .recovery
                case .notFound:
                    confirm = ""
                    step = .createConfirm
                case let .invalid(msg), let .locked(msg):
                    Haptics.warning(); error = msg
                }
            } catch {
                Haptics.warning()
                self.error = (error as? APIError)?.errorDescription ?? error.localizedDescription
            }
        }
    }

    private func submitCreate() {
        guard password.count >= 4 else { error = "Пароль не короче 4 символов"; Haptics.warning(); return }
        guard password == confirm else { error = "Пароли не совпадают"; Haptics.warning(); return }
        let user = username.trimmingCharacters(in: .whitespaces)
        error = nil
        Task {
            do {
                let (id, name) = try await busy.run { try await state.api.register(username: user, password: password) }
                Haptics.success()
                state.save(contributorId: id, nickname: name)
            } catch {
                Haptics.warning()
                self.error = (error as? APIError)?.errorDescription ?? error.localizedDescription
            }
        }
    }

    private func submitRecover() {
        guard password == confirm else { error = "Пароли не совпадают"; Haptics.warning(); return }
        let user = username.trimmingCharacters(in: .whitespaces)
        error = nil
        Task {
            do {
                let (id, name) = try await busy.run { try await state.api.recover(username: user, password: password) }
                Haptics.success()
                state.save(contributorId: id, nickname: name)
            } catch {
                Haptics.warning()
                self.error = (error as? APIError)?.errorDescription ?? error.localizedDescription
            }
        }
    }

    private func reset() {
        error = nil; password = ""; confirm = ""
        step = .login
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
                    Text("Симулятор видит хост как localhost. Для устройства укажите IP в сети, напр. http://192.168.1.10:8080")
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
