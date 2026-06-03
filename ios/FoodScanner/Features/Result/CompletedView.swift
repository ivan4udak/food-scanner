import SwiftUI

/// Зелёный экран успеха после завершения каталога.
struct CompletedView: View {
    let count: Int
    @Binding var path: NavigationPath
    @State private var pop = false

    var body: some View {
        Screen {
            VStack(spacing: 28) {
                Spacer()
                ZStack {
                    Circle().fill(Theme.accent.opacity(0.12)).frame(width: 132, height: 132)
                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: 58)).foregroundStyle(Theme.accent)
                        .scaleEffect(pop ? 1 : 0.6)
                        .animation(.spring(response: 0.45, dampingFraction: 0.5), value: pop)
                }
                VStack(spacing: 10) {
                    Text("Готово!")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundStyle(Theme.textPrimary)
                    Text("Продукт добавлен в каталог")
                        .font(.subheadline).foregroundStyle(Theme.textSecondary)
                }

                HStack(spacing: 12) {
                    Image(systemName: "trophy.fill").foregroundStyle(Theme.warning)
                    Text("Ваш вклад: **\(count)** \(plural(count))")
                        .foregroundStyle(Theme.textPrimary)
                }
                .font(.subheadline)
                .padding(.horizontal, 18).padding(.vertical, 12)
                .background(Theme.surface, in: Capsule())
                .overlay(Capsule().stroke(Theme.stroke))

                Spacer()
                PrimaryButton(title: "Сканировать ещё", systemImage: "barcode.viewfinder") {
                    path = NavigationPath()
                }
            }
            .padding(Theme.pad)
        }
        .navigationBarBackButtonHidden(true)
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { pop = true }
    }

    private func plural(_ n: Int) -> String {
        let mod10 = n % 10, mod100 = n % 100
        if mod10 == 1 && mod100 != 11 { return "продукт" }
        if (2...4).contains(mod10) && !(12...14).contains(mod100) { return "продукта" }
        return "продуктов"
    }
}
