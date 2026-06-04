import Foundation
import SwiftUI

/// Глобальное состояние: профиль контрибьютора и адрес сервера.
/// Сохраняется между запусками в UserDefaults.
@MainActor
final class AppState: ObservableObject {

    @Published var contributorId: UUID?
    @Published var nickname: String?
    @Published var baseURLString: String {
        didSet { defaults.set(baseURLString, forKey: Keys.baseURL) }
    }

    // Токены — в Keychain (не в UserDefaults). В памяти для Bearer-заголовка.
    private(set) var accessToken: String?
    private(set) var refreshToken: String?

    private let defaults = UserDefaults.standard
    private enum Keys {
        static let contributorId = "fs.contributorId"
        static let nickname      = "fs.nickname"
        static let baseURL       = "fs.baseURL"
    }

    init() {
        self.baseURLString = defaults.string(forKey: Keys.baseURL) ?? "http://localhost:8080"
        if let raw = defaults.string(forKey: Keys.contributorId), let id = UUID(uuidString: raw) {
            self.contributorId = id
            self.nickname = defaults.string(forKey: Keys.nickname)
        }
        self.accessToken  = KeychainStore.get(KeychainStore.Key.access)
        self.refreshToken = KeychainStore.get(KeychainStore.Key.refresh)
    }

    /// Режим загрузки фото (камера/галерея). Только в памяти — живёт до конца сессии,
    /// в UserDefaults НЕ сохраняется (Блоки 11–12).
    @Published var photoSource: PhotoSource?

    var isRegistered: Bool { contributorId != nil }

    var api: APIClient {
        APIClient(baseURL: URL(string: baseURLString) ?? URL(string: "http://localhost:8080")!,
                  accessToken: accessToken)
    }

    /// Сохраняет сессию (профиль + токены): профиль в UserDefaults, токены в Keychain.
    func signIn(_ session: Session) {
        contributorId = session.contributorId
        nickname      = session.username
        defaults.set(session.contributorId.uuidString, forKey: Keys.contributorId)
        defaults.set(session.username, forKey: Keys.nickname)
        setTokens(access: session.accessToken, refresh: session.refreshToken)
    }

    /// Обновляет access-токен по refresh; при провале (refresh протух) — logout.
    func refreshSession() async {
        guard let refreshToken else { return }
        do {
            let s = try await api.refresh(refreshToken: refreshToken)
            setTokens(access: s.accessToken, refresh: s.refreshToken)
        } catch {
            signOut()   // refresh истёк → выход
        }
    }

    func signOut() {
        contributorId = nil
        nickname = nil
        defaults.removeObject(forKey: Keys.contributorId)
        defaults.removeObject(forKey: Keys.nickname)
        setTokens(access: nil, refresh: nil)
        photoSource = nil
    }

    private func setTokens(access: String?, refresh: String?) {
        accessToken = access
        refreshToken = refresh
        KeychainStore.set(access, for: KeychainStore.Key.access)
        KeychainStore.set(refresh, for: KeychainStore.Key.refresh)
    }
}
