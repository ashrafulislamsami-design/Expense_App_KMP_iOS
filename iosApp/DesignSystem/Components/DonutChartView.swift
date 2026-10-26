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
    
    private func shortLabel(_ name: String) -> String {
        if name.localizedCaseInsensitiveContains("debt") { return "Zero Debt" }
        if name.localizedCaseInsensitiveContains("available") { return "Available" }
        if name.localizedCaseInsensitiveContains("buffer") { return "Buffer" }
        if name.localizedCaseInsensitiveContains("spent") { return "Spent" }
        if name.localizedCaseInsensitiveContains("salary") { return "Salary" }
        if name.count > 10 { return String(name.prefix(9)) + "…" }
        return name
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(centerCustomLabel ?? "Category Breakdown & Budget")
                .font(.subheadline)
                .fontWeight(.bold)
                .foregroundColor(AppColors.onSurface(for: colorScheme))
            
            // Centered Donut Ring with generous hole diameter (150pt canvas, 116pt inner hole)
            HStack {
                Spacer()
                ZStack {
                    Canvas { context, size in
                        let total = Double(slices.map { $0.percentage }.reduce(0, +))
                        let safeTotal = total > 0 ? total : 100
                        let strokeWidth: CGFloat = 14
                        let selectedStrokeWidth: CGFloat = 17
                        let radius = (min(size.width, size.height) - selectedStrokeWidth) / 2
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
                                style: StrokeStyle(lineWidth: isSelected ? selectedStrokeWidth : strokeWidth, lineCap: .butt)
                            )
                            
                            startAngle += sweep + gap
                        }
                    }
                    .frame(width: 150, height: 150)
                    
                    // Center Callout (Bounded within max 94pt: never touches or crosses the ring)
                    if slices.indices.contains(selectedIndex) {
                        let active = slices[selectedIndex]
                        VStack(spacing: 2) {
                            Text("\(currencySymbol)\(active.amount / 100)")
                                .font(.system(size: 16, weight: .heavy))
                                .monospacedDigit()
                                .foregroundColor(AppColors.onSurface(for: colorScheme))
                                .lineLimit(1)
                            
                            Text(shortLabel(active.name))
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                                .lineLimit(1)
                            
                            Text("\(active.percentage)%")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(active.color)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(active.color.opacity(0.15))
                                .clipShape(Capsule())
                        }
                        .frame(maxWidth: 94)
                    }
                }
                .frame(width: 150, height: 150)
                Spacer()
            }
            .padding(.vertical, 4)
            
            Divider()
                .opacity(0.5)
            
            // Full-Width Category / Allocation Breakdown List
            VStack(spacing: 8) {
                ForEach(slices.indices, id: \.self) { idx in
                    let slice = slices[idx]
                    let isSelected = idx == selectedIndex
                    
                    VStack(spacing: 6) {
                        HStack {
                            Circle()
                                .fill(slice.color)
                                .frame(width: 9, height: 9)
                            
                            Text(slice.name)
                                .font(.subheadline)
                                .fontWeight(isSelected ? .bold : .medium)
                                .foregroundColor(AppColors.onSurface(for: colorScheme))
                                .lineLimit(1)
                            
                            Spacer()
                            
                            Text("\(currencySymbol)\(slice.amount / 100)")
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .monospacedDigit()
                                .foregroundColor(AppColors.onSurface(for: colorScheme))
                            
                            Text("\(slice.percentage)%")
                                .font(.caption2)
                                .fontWeight(.bold)
                                .foregroundColor(slice.color)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(slice.color.opacity(0.12))
                                .clipShape(RoundedRectangle(cornerRadius: 5, style: .continuous))
                        }
                        
                        // Sleek Linear Progress Bar
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule()
                                    .fill(AppColors.surfaceVariant(for: colorScheme))
                                    .frame(height: 4)
                                Capsule()
                                    .fill(slice.color)
                                    .frame(width: max(4, geo.size.width * CGFloat(min(100, slice.percentage)) / 100.0), height: 4)
                            }
                        }
                        .frame(height: 4)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(isSelected ? AppColors.surfaceVariant(for: colorScheme).opacity(0.6) : AppColors.surface(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .stroke(isSelected ? slice.color.opacity(0.4) : AppColors.border(for: colorScheme).opacity(0.4), lineWidth: 1)
                    )
                    .contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            selectedIndex = idx
                        }
                    }
                }
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
