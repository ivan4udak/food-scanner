import SwiftUI
import SwiftData

/// Фото для отображения (из сервера или офлайн-кэша).
struct DisplayPhoto: Identifiable, Hashable {
    let id = UUID()
    let type: String
    let storageKey: String
}

/// Просмотр записи каталога по штрихкоду.
/// Блок 18: сначала показываем локальную запись (SwiftData), затем тихо обновляем с сервера.
struct LookupView: View {
    let barcode: String
    @EnvironmentObject private var state: AppState
    @Environment(\.modelContext) private var ctx

    @State private var photos: [DisplayPhoto] = []
    @State private var title: String = ""
    @State private var updatedAt: Date?
    @State private var fromCache = false
    @State private var loading = true
    @State private var error: String?
    @State private var opened: DisplayPhoto?

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter(); f.dateStyle = .medium; f.timeStyle = .short; return f
    }()

    var body: some View {
        Screen {
            Group {
                if !photos.isEmpty {
                    content
                } else if loading {
                    ProgressView().controlSize(.large)
                } else {
                    VStack(spacing: 12) {
                        Image(systemName: "tray").font(.largeTitle).foregroundStyle(Theme.textSecondary)
                        Text(error ?? "Запись не найдена").foregroundStyle(Theme.textSecondary)
                    }.padding()
                }
            }
        }
        .navigationTitle("Запись каталога")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
        .fullScreenCover(item: $opened) { photo in
            PhotoViewer(storageKey: photo.storageKey, api: state.api)
        }
    }

    private var content: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 8) {
                    Label(barcode, systemImage: "barcode")
                        .font(.system(.title3, design: .rounded).weight(.bold))
                        .foregroundStyle(Theme.textPrimary)
                    if let updatedAt {
                        Text("Обновлено \(Self.dateFormatter.string(from: updatedAt))"
                             + (fromCache ? " · из кэша" : ""))
                            .font(.footnote).foregroundStyle(Theme.textSecondary)
                    }
                }.card()

                Text("Фотографии (\(photos.count))")
                    .font(.subheadline.weight(.semibold)).foregroundStyle(Theme.textSecondary)

                LazyVGrid(columns: [GridItem(.flexible(), spacing: 14),
                                    GridItem(.flexible(), spacing: 14)], spacing: 14) {
                    ForEach(photos) { photoCell($0) }
                }
            }
            .padding(Theme.pad)
        }
    }

    private func photoCell(_ photo: DisplayPhoto) -> some View {
        let label = PhotoSlot(rawValue: photo.type)?.title ?? photo.type
        return Button { opened = photo } label: {
            ZStack {
                CachedImage(storageKey: photo.storageKey, thumbnail: true, api: state.api) {
                    ZStack {
                        Theme.background
                        Image(systemName: PhotoSlot(rawValue: photo.type)?.systemImage ?? "photo")
                            .font(.title).foregroundStyle(Theme.accent)
                    }
                }
                VStack {
                    Spacer()
                    Text(label)
                        .font(.caption.weight(.semibold)).foregroundStyle(.white)
                        .frame(maxWidth: .infinity).padding(.vertical, 6)
                        .background(.ultraThinMaterial)
                }
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(1, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: Theme.radius, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: Theme.radius, style: .continuous).stroke(Theme.stroke))
        }
        .buttonStyle(.plain)
    }

    // MARK: Загрузка (кэш → сервер)

    private func load() async {
        loading = true; error = nil

        if let cached = fetchCached() {           // 1) мгновенно из кэша
            apply(types: cached.photoTypes, keys: cached.photoKeys, updated: cached.updatedAt, cache: true)
        }

        do {                                       // 2) тихо обновляем с сервера
            if let resp = try await state.api.entry(barcode: barcode) {
                apply(types: resp.photos.map(\.type), keys: resp.photos.map(\.storageKey),
                      updated: resp.createdAt, cache: false)
                upsertCache(resp)
            } else if photos.isEmpty {
                error = "Запись не найдена"
            }
        } catch {
            if photos.isEmpty {                    // офлайн и нет кэша
                self.error = (error as? APIError)?.errorDescription ?? error.localizedDescription
            }
        }
        loading = false
    }

    private func apply(types: [String], keys: [String], updated: Date, cache: Bool) {
        photos = zip(types, keys).map { DisplayPhoto(type: $0, storageKey: $1) }
        updatedAt = updated
        fromCache = cache
        title = barcode
    }

    private func fetchCached() -> CachedEntry? {
        let bc = barcode
        let descriptor = FetchDescriptor<CachedEntry>(predicate: #Predicate { $0.barcode == bc })
        return try? ctx.fetch(descriptor).first
    }

    private func upsertCache(_ resp: CatalogEntryResponse) {
        let types = resp.photos.map(\.type)
        let keys  = resp.photos.map(\.storageKey)
        if let existing = fetchCached() {
            existing.photoTypes = types
            existing.photoKeys  = keys
            existing.updatedAt  = Date()
            existing.name       = resp.barcode
        } else {
            ctx.insert(CachedEntry(barcode: resp.barcode, name: resp.barcode,
                                   photoTypes: types, photoKeys: keys, updatedAt: Date()))
        }
        try? ctx.save()
    }
}

/// Полноэкранный просмотр в полном качестве (full ≤ FullHD), из кэша.
private struct PhotoViewer: View {
    let storageKey: String
    let api: APIClient
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            CachedImage(storageKey: storageKey, thumbnail: false, contentMode: .fit, api: api) {
                ProgressView().tint(.white)
            }
            .ignoresSafeArea()
            VStack {
                HStack {
                    Spacer()
                    Button { dismiss() } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 30)).foregroundStyle(.white.opacity(0.9)).padding()
                    }
                }
                Spacer()
            }
        }
    }
}
