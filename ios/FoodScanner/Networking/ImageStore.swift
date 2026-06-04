import UIKit
import CryptoKit

/// Блок 9: двухуровневый кэш изображений.
/// 1) memory (NSCache)  2) disk (Caches)  3) сеть.
/// Повторное открытие не качает заново.
actor ImageStore {
    static let shared = ImageStore()

    private let memory = NSCache<NSString, UIImage>()
    private let fileManager = FileManager.default
    private let dir: URL

    init() {
        let caches = fileManager.urls(for: .cachesDirectory, in: .userDomainMask)[0]
        dir = caches.appendingPathComponent("fs-images", isDirectory: true)
        try? fileManager.createDirectory(at: dir, withIntermediateDirectories: true)
    }

    /// Загружает изображение: память → диск → сеть. Кладёт во все уровни.
    func image(storageKey: String, thumbnail: Bool, api: APIClient) async -> UIImage? {
        let key = cacheKey(storageKey, thumbnail)

        if let cached = memory.object(forKey: key as NSString) {
            return cached
        }
        let file = dir.appendingPathComponent(key)
        if let data = try? Data(contentsOf: file), let img = UIImage(data: data) {
            memory.setObject(img, forKey: key as NSString)
            return img
        }
        guard let data = try? await api.photoData(storageKey: storageKey, thumbnail: thumbnail),
              let img = UIImage(data: data) else {
            return nil
        }
        memory.setObject(img, forKey: key as NSString)
        try? data.write(to: file, options: .atomic)
        return img
    }

    /// Полностью очищает кэш изображений (память + диск). Возвращает освобождённый размер (байт).
    func clear() -> Int64 {
        memory.removeAllObjects()
        let size = diskSize()
        try? fileManager.removeItem(at: dir)
        try? fileManager.createDirectory(at: dir, withIntermediateDirectories: true)
        return size
    }

    /// Текущий размер дискового кэша (байт).
    func diskSize() -> Int64 {
        guard let files = try? fileManager.contentsOfDirectory(at: dir, includingPropertiesForKeys: [.fileSizeKey]) else { return 0 }
        return files.reduce(0) { acc, url in
            acc + Int64((try? url.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? 0)
        }
    }

    private func cacheKey(_ storageKey: String, _ thumbnail: Bool) -> String {
        let raw = (thumbnail ? "thumb:" : "full:") + storageKey
        let digest = SHA256.hash(data: Data(raw.utf8))
        return digest.map { String(format: "%02x", $0) }.joined() + ".img"
    }
}
