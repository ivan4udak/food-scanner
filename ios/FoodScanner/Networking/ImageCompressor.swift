import UIKit

/// Блок 8: сжатие перед загрузкой.
/// Ограничиваем большую сторону до Full HD (1920), JPEG высокого качества,
/// читаемость мелкого текста сохраняется. Ориентир 300–800 КБ.
enum ImageCompressor {

    static let maxSide: CGFloat = 1920
    static let targetMaxBytes = 800 * 1024
    static let targetMinBytes = 300 * 1024

    static func compress(_ image: UIImage) -> Data {
        let scaled = downscale(image, maxSide: maxSide)

        // Подбираем качество: стартуем с 0.8 и снижаем, пока > 800 КБ.
        var quality: CGFloat = 0.8
        var data = scaled.jpegData(compressionQuality: quality) ?? Data()
        while data.count > targetMaxBytes && quality > 0.4 {
            quality -= 0.1
            data = scaled.jpegData(compressionQuality: quality) ?? data
        }
        return data
    }

    /// Уменьшает до maxSide по большей стороне. Не увеличивает.
    private static func downscale(_ image: UIImage, maxSide: CGFloat) -> UIImage {
        let w = image.size.width, h = image.size.height
        let longSide = max(w, h)
        guard longSide > maxSide, longSide > 0 else { return image }

        let scale = maxSide / longSide
        let newSize = CGSize(width: floor(w * scale), height: floor(h * scale))

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1                      // пиксели, а не точки
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(size: newSize, format: format)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: newSize))
        }
    }
}
