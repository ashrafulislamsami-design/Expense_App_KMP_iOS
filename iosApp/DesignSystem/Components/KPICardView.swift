import SwiftUI

struct KPICardView: View {
    let title: String
    let value: String
    let badgeText: String
    let isBadgePositive: Bool
    let subtitle: String
    let accentColor: Color
    var onTap: (() -> Void)? = nil
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        Button(action: { onTap?() }) {
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(title)
                        .font(.caption)
                        .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                    
                    Spacer()
                    
                    Circle()
                        .fill(accentColor)
                        .frame(width: 7, height: 7)
                }
                
                Text(value)
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.onSurface(for: colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                
                HStack(spacing: 4) {
                    HStack(spacing: 2) {
                        Text(isBadgePositive ? "↑" : "↓")
                            .font(.system(size: 9, weight: .bold))
                        Text(badgeText)
                            .font(.system(size: 10, weight: .semibold))
                    }
                    .padding(.horizontal, 5)
                    .padding(.vertical, 2)
                    .background(isBadgePositive ? Color.appIncome.opacity(0.12) : Color.appExpense.opacity(0.12))
                    .foregroundColor(isBadgePositive ? Color.appIncome : Color.appExpense)
                    .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
                    
                    Text(subtitle)
                        .font(.system(size: 10))
                        .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                        .lineLimit(1)
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppColors.surface(for: colorScheme))
            .clipShape(AppShapes.medium)
            .overlay(
                AppShapes.medium
                    .stroke(AppColors.border(for: colorScheme), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}
