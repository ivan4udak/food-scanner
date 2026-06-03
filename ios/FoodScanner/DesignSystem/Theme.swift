import SwiftUI

/// Единая палитра и метрики. Минимализм: один акцент, мягкие поверхности.
enum Theme {
    // Поверхности
    static let background = Color(hex: 0xF6F7F5)
    static let surface    = Color.white
    static let stroke     = Color.black.opacity(0.06)

    // Акценты
    static let accent  = Color(hex: 0x1FB25A)   // зелёный — «новый продукт»
    static let danger  = Color(hex: 0xE5484D)   // красный — «уже в каталоге»
    static let warning = Color(hex: 0xF2A100)

    // Текст
    static let textPrimary   = Color(hex: 0x14180F)
    static let textSecondary = Color(hex: 0x6B7280)

    // Метрики
    static let radius: CGFloat   = 22
    static let radiusSm: CGFloat = 14
    static let pad: CGFloat      = 20
}

extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red:   Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue:  Double(hex & 0xFF) / 255,
            opacity: 1
        )
    }
}

extension View {
    /// Карточка с мягкой тенью.
    func card(padding: CGFloat = Theme.pad) -> some View {
        self
            .padding(padding)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: Theme.radius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: Theme.radius, style: .continuous)
                    .stroke(Theme.stroke, lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.05), radius: 14, x: 0, y: 8)
    }
}
