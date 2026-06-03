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
    }

    /// Режим загрузки фото (камера/галерея). Только в памяти — живёт до конца сессии,
    /// в UserDefaults НЕ сохраняется (Блоки 11–12).
    @Published var photoSource: PhotoSource?

    var isRegistered: Bool { contributorId != nil }

    var api: APIClient {
        APIClient(baseURL: URL(string: baseURLString) ?? URL(string: "http://localhost:8080")!)
    }

    func save(contributorId: UUID, nickname: String) {
        self.contributorId = contributorId
        self.nickname = nickname
        defaults.set(contributorId.uuidString, forKey: Keys.contributorId)
        defaults.set(nickname, forKey: Keys.nickname)
    }

    func signOut() {
        contributorId = nil
        nickname = nil
        defaults.removeObject(forKey: Keys.contributorId)
        defaults.removeObject(forKey: Keys.nickname)
    }
}
