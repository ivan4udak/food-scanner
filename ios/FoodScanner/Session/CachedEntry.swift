import Foundation
import SwiftData

/// Блок 18: локально сохранённая (просмотренная) карточка каталога.
/// Хранит barcode, название, список фото (тип+ключ) и дату обновления.
@Model
final class CachedEntry {
    @Attribute(.unique) var barcode: String
    var name: String
    var photoTypes: [String]
    var photoKeys: [String]
    var updatedAt: Date

    init(barcode: String, name: String, photoTypes: [String], photoKeys: [String], updatedAt: Date) {
        self.barcode    = barcode
        self.name       = name
        self.photoTypes = photoTypes
        self.photoKeys  = photoKeys
        self.updatedAt  = updatedAt
    }
}
