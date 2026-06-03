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

                ForEach(entry.photos) { photo in
                    HStack(spacing: 14) {
                        Image(systemName: PhotoSlot(rawValue: photo.type)?.systemImage ?? "photo")
                            .font(.title3).foregroundStyle(Theme.accent)
                            .frame(width: 44, height: 44)
                            .background(Theme.accent.opacity(0.10), in: RoundedRectangle(cornerRadius: 12))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(PhotoSlot(rawValue: photo.type)?.title ?? photo.type)
                                .font(.subheadline.weight(.medium)).foregroundStyle(Theme.textPrimary)
                            Text(photo.storageKey).font(.caption).foregroundStyle(Theme.textSecondary)
                                .lineLimit(1).truncationMode(.middle)
                        }
                        Spacer(minLength: 0)
                    }.card(padding: 14)
                }
            }
            .padding(Theme.pad)
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
