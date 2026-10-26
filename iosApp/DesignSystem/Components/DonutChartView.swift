import SwiftUI

struct DonutSliceDTO: Identifiable {
    let id = UUID()
    let name: String
    let amount: Int64
    let percentage: Int
    let color: Color
}

struct DonutSliceData: Identifiable {
    let id = UUID()
    let name: String
    let amountMinor: Int64
    let color: Color
}

struct DonutChartView: View {
    let slices: [DonutSliceDTO]
    let currencySymbol: String
    let centerCustomLabel: String?
    let totalCustomAmount: Int64?
    
    @State private var selectedIndex: Int = 0
    @Environment(\.colorScheme) private var colorScheme
    
    // Primary initializer for DonutSliceDTO
    init(slices: [DonutSliceDTO], currencySymbol: String = "৳") {
        self.slices = slices
        self.currencySymbol = currencySymbol
        self.centerCustomLabel = nil
        self.totalCustomAmount = nil
    }
    
    // Convenience initializer for DonutSliceData (used in ReportsView)
    init(slices: [DonutSliceData], totalAmountMinor: Int64? = nil, centerLabel: String? = nil, currencySymbol: String = "৳") {
        let total = totalAmountMinor ?? slices.reduce(0) { $0 + $1.amountMinor }
        let safeTotal = total > 0 ? Double(total) : 1.0
        
        self.slices = slices.map { slice in
            let pct = Int(round((Double(slice.amountMinor) / safeTotal) * 100.0))
            return DonutSliceDTO(
                name: slice.name,
                amount: slice.amountMinor,
                percentage: max(1, pct),
                color: slice.color
            )
        }
        self.currencySymbol = currencySymbol
        self.centerCustomLabel = centerLabel
        self.totalCustomAmount = totalAmountMinor
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(centerCustomLabel ?? "Category Breakdown & Budget")
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(AppColors.onSurface(for: colorScheme))
            
            HStack(spacing: 16) {
                // Donut Ring with Center Callout
                ZStack {
                    Canvas { context, size in
                        let total = Double(slices.map { $0.percentage }.reduce(0, +))
                        let safeTotal = total > 0 ? total : 100
                        let strokeWidth: CGFloat = 16
                        let radius = (min(size.width, size.height) - strokeWidth) / 2
                        let center = CGPoint(x: size.width / 2, y: size.height / 2)
                        
                        var startAngle = Angle(degrees: -90)
                        let gap = slices.count > 1 ? Angle(degrees: 3) : Angle.zero
                        
                        for (idx, slice) in slices.enumerated() {
                            let sweep = Angle(degrees: (Double(slice.percentage) / safeTotal) * 360) - gap
                            let isSelected = idx == selectedIndex
                            
                            var path = Path()
                            path.addArc(
                                center: center,
                                radius: radius,
                                startAngle: startAngle,
                                endAngle: startAngle + sweep,
                                clockwise: false
                            )
                            
                            context.stroke(
                                path,
                                with: .color(slice.color),
                                style: StrokeStyle(lineWidth: isSelected ? 20 : strokeWidth, lineCap: .butt)
                            )
                            
                            startAngle += sweep + gap
                        }
                    }
                    .frame(width: 110, height: 110)
                    
                    // Center Callout
                    if slices.indices.contains(selectedIndex) {
                        let active = slices[selectedIndex]
                        VStack(spacing: 1) {
                            Text("\(currencySymbol)\(active.amount / 100)")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(AppColors.onSurface(for: colorScheme))
                                .lineLimit(1)
                            Text(active.name)
                                .font(.system(size: 9))
                                .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                                .lineLimit(1)
                            Text("\(active.percentage)%")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(active.color)
                        }
                        .frame(maxWidth: 80)
                    }
                }
                .frame(width: 110, height: 110)
                
                // Legend
                VStack(spacing: 6) {
                    ForEach(slices.indices, id: \.self) { idx in
                        let slice = slices[idx]
                        let isSelected = idx == selectedIndex
                        
                        HStack {
                            Circle()
                                .fill(slice.color)
                                .frame(width: 8, height: 8)
                            
                            Text(slice.name)
                                .font(.caption2)
                                .fontWeight(isSelected ? .bold : .regular)
                                .foregroundColor(AppColors.onSurface(for: colorScheme))
                                .lineLimit(1)
                            
                            Spacer()
                            
                            Text("\(currencySymbol)\(slice.amount / 100)")
                                .font(.caption2)
                                .fontWeight(.semibold)
                                .foregroundColor(AppColors.onSurface(for: colorScheme))
                            
                            Text("\(slice.percentage)%")
                                .font(.system(size: 10))
                                .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                        }
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(isSelected ? AppColors.surfaceVariant(for: colorScheme) : Color.clear)
                        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                        .onTapGesture {
                            selectedIndex = idx
                        }
                    }
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(14)
        .background(AppColors.surface(for: colorScheme))
        .clipShape(AppShapes.medium)
        .overlay(
            AppShapes.medium.stroke(AppColors.border(for: colorScheme), lineWidth: 1)
        )
    }
}
