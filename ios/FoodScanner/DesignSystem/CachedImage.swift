import SwiftUI

/// Изображение из кэша (память → диск → сеть). Блок 9.
struct CachedImage<Placeholder: View>: View {
    let storageKey: String
    var thumbnail: Bool = true
    var contentMode: ContentMode = .fill
    let api: APIClient
    @ViewBuilder var placeholder: () -> Placeholder

    @State private var image: UIImage?
    @State private var failed = false

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image).resizable().aspectRatio(contentMode: contentMode)
            } else if failed {
                placeholder()
            } else {
                ZStack {
                    placeholder()
                    ProgressView()
                }
            }
        }
        .task(id: storageKey + (thumbnail ? "·t" : "·f")) {
            image = await ImageStore.shared.image(storageKey: storageKey, thumbnail: thumbnail, api: api)
            failed = image == nil
        }
    }
}
