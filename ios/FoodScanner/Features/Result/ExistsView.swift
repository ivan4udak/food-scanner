import SwiftUI

/// Красный экран: продукт с таким штрихкодом уже в каталоге.
struct ExistsView: View {
    let barcode: String
    @Binding var path: NavigationPath

    var body: some View {
        Screen {
            VStack(spacing: 28) {
                Spacer()
                ZStack {
                    Circle().fill(Theme.danger.opacity(0.12)).frame(width: 132, height: 132)
                    Image(systemName: "xmark.octagon.fill")
                        .font(.system(size: 56)).foregroundStyle(Theme.danger)
                }
                VStack(spacing: 10) {
                    Text("Уже в каталоге")
                        .font(.system(size: 26, weight: .bold, design: .rounded))
                        .foregroundStyle(Theme.textPrimary)
                    Text("Продукт с штрихкодом\n\(barcode)\nуже добавлен ранее")
                        .font(.subheadline).foregroundStyle(Theme.textSecondary)
                        .multilineTextAlignment(.center)
                }
                Spacer()
                VStack(spacing: 12) {
                    PrimaryButton(title: "Посмотреть запись", systemImage: "doc.text.magnifyingglass") {
                        path.append(Route.lookup(barcode: barcode))
                    }
                    SecondaryButton(title: "Сканировать другой", systemImage: "barcode.viewfinder") {
                        path = NavigationPath()
                    }
                }
            }
            .padding(Theme.pad)
        }
        .navigationBarBackButtonHidden(false)
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
    }
}
