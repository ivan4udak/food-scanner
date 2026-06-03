import SwiftUI

/// Просмотр существующей записи каталога по штрихкоду (GET /entries/{barcode}).
struct LookupView: View {
    let barcode: String
    @EnvironmentObject private var state: AppState

    @State private var entry: CatalogEntryResponse?
    @State private var loading = true
    @State private var error: String?

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateStyle = .medium; f.timeStyle = .short
        return f
    }()

    var body: some View {
        Screen {
            Group {
                if loading {
                    ProgressView().controlSize(.large)
                } else if let entry {
                    content(entry)
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
    }

    private func content(_ entry: CatalogEntryResponse) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 8) {
                    Label(entry.barcode, systemImage: "barcode")
                        .font(.system(.title3, design: .rounded).weight(.bold))
                        .foregroundStyle(Theme.textPrimary)
                    Text("Добавлено \(Self.dateFormatter.string(from: entry.createdAt))")
                        .font(.footnote).foregroundStyle(Theme.textSecondary)
                }.card()

                Text("Фотографии (\(entry.photos.count))")
                    .font(.subheadline.weight(.semibold)).foregroundStyle(Theme.textSecondary)

                LazyVGrid(columns: [GridItem(.flexible(), spacing: 14),
                                    GridItem(.flexible(), spacing: 14)], spacing: 14) {
                    ForEach(entry.photos) { photo in
                        photoCell(photo)
                    }
                }
            }
            .padding(Theme.pad)
        }
    }

    private func photoCell(_ photo: CatalogEntryResponse.Photo) -> some View {
        let title = PhotoSlot(rawValue: photo.type)?.title ?? photo.type
        return ZStack {
            if let url = state.api.photoURL(storageKey: photo.storageKey) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let img):
                        img.resizable().scaledToFill()
                    case .failure:
                        placeholder(photo)
                    default:
                        ProgressView()
                    }
                }
            } else {
                placeholder(photo)
            }
            VStack {
                Spacer()
                Text(title)
                    .font(.caption.weight(.semibold)).foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
                    .background(.ultraThinMaterial)
            }
        }
        .frame(maxWidth: .infinity)
        .aspectRatio(1, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: Theme.radius, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: Theme.radius, style: .continuous).stroke(Theme.stroke))
    }

    private func placeholder(_ photo: CatalogEntryResponse.Photo) -> some View {
        ZStack {
            Theme.background
            Image(systemName: PhotoSlot(rawValue: photo.type)?.systemImage ?? "photo")
                .font(.title).foregroundStyle(Theme.accent)
        }
    }

    private func load() async {
        loading = true; error = nil
        do {
            entry = try await state.api.entry(barcode: barcode)
            if entry == nil { error = "Запись не найдена" }
        } catch {
            self.error = (error as? APIError)?.errorDescription ?? error.localizedDescription
        }
        loading = false
    }
}
