import SwiftUI

// MARK: - Primary button

struct PrimaryButton: View {
    let title: String
    var systemImage: String? = nil
    var tint: Color = Theme.accent
    var loading: Bool = false
    var enabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                if loading {
                    ProgressView().tint(.white)
                } else if let systemImage {
                    Image(systemName: systemImage)
                }
                Text(title).fontWeight(.semibold)
            }
            .font(.headline)
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(tint.opacity(enabled && !loading ? 1 : 0.4),
                        in: RoundedRectangle(cornerRadius: Theme.radiusSm, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!enabled || loading)
    }
}

// MARK: - Secondary button

struct SecondaryButton: View {
    let title: String
    var systemImage: String? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let systemImage { Image(systemName: systemImage) }
                Text(title).fontWeight(.medium)
            }
            .font(.subheadline)
            .foregroundStyle(Theme.textPrimary)
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: Theme.radiusSm, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: Theme.radiusSm, style: .continuous)
                .stroke(Theme.stroke, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Progress ring

struct ProgressRing: View {
    let value: Int
    let total: Int

    private var fraction: Double { total == 0 ? 0 : Double(value) / Double(total) }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Theme.stroke, lineWidth: 10)
            Circle()
                .trim(from: 0, to: fraction)
                .stroke(Theme.accent, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.spring(duration: 0.5), value: fraction)
            VStack(spacing: 0) {
                Text("\(value)").font(.system(size: 34, weight: .bold, design: .rounded))
                    .foregroundStyle(Theme.textPrimary)
                Text("из \(total)").font(.caption).foregroundStyle(Theme.textSecondary)
            }
        }
        .frame(width: 116, height: 116)
    }
}

// MARK: - Inline error banner

struct ErrorBanner: View {
    let message: String
    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "exclamationmark.triangle.fill")
            Text(message).font(.subheadline)
            Spacer(minLength: 0)
        }
        .foregroundStyle(Theme.danger)
        .padding(14)
        .background(Theme.danger.opacity(0.10),
                    in: RoundedRectangle(cornerRadius: Theme.radiusSm, style: .continuous))
    }
}

// MARK: - Screen container

struct Screen<Content: View>: View {
    @ViewBuilder var content: Content
    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            content
        }
    }
}
