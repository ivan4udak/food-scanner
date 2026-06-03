import Foundation

/// Шесть обязательных типов фото (зеркало `PhotoType` / `CatalogCompletionPolicy` на бэкенде).
enum PhotoSlot: String, CaseIterable, Identifiable {
    case BARCODE
    case FRONT
    case BACK
    case INGREDIENTS
    case NUTRITION
    case EXTRA

    var id: String { rawValue }

    /// Обязательные для завершения каталога (зеркало `CatalogCompletionPolicy.REQUIRED_TYPES`).
    var isRequired: Bool {
        switch self {
        case .BARCODE, .FRONT, .INGREDIENTS, .NUTRITION: return true
        case .BACK, .EXTRA:                              return false
        }
    }

    static let required: [PhotoSlot] = allCases.filter { $0.isRequired }
    static let optional: [PhotoSlot] = allCases.filter { !$0.isRequired }

    var title: String {
        switch self {
        case .BARCODE:     return "Штрихкод"
        case .FRONT:       return "Лицевая сторона"
        case .BACK:        return "Оборот"
        case .INGREDIENTS: return "Состав"
        case .NUTRITION:   return "КБЖУ"
        case .EXTRA:       return "Дополнительно"
        }
    }

    var hint: String {
        switch self {
        case .BARCODE:     return "Крупно штрихкод"
        case .FRONT:       return "Упаковка спереди"
        case .BACK:        return "Упаковка сзади"
        case .INGREDIENTS: return "Список ингредиентов"
        case .NUTRITION:   return "Таблица БЖУ"
        case .EXTRA:       return "Бок, дно и т.п."
        }
    }

    var systemImage: String {
        switch self {
        case .BARCODE:     return "barcode"
        case .FRONT:       return "shippingbox"
        case .BACK:        return "shippingbox.fill"
        case .INGREDIENTS: return "list.bullet.rectangle"
        case .NUTRITION:   return "chart.bar.doc.horizontal"
        case .EXTRA:       return "plus.viewfinder"
        }
    }
}
