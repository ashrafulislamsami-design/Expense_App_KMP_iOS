import SwiftUI

struct DailyTrendPointDTO: Identifiable {
    let id = UUID()
    let dayLabel: String
    let currentAmount: Int64
    let previousAmount: Int64
}

struct PulseCurveChartView: View {
    let points: [DailyTrendPointDTO]
    let currentTotal: Int64
    let safeDailyAllowance: Int64
    let currencySymbol: String
    
    @State private var selectedIndex: Int = 6
    @Environment(\.colorScheme) private var colorScheme
    
    private var maxAmount: Int64 {
        let maxPoint = points.map { $0.currentAmount }.max() ?? 0
        let ceiling = max(maxPoint, safeDailyAllowance * 15 / 10)
        return ceiling <= 0 ? 200000 : Int64(Double(ceiling) * 1.2)
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header: Title & Selected Day Callout
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Spending pulse & daily pace")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(AppColors.onSurface(for: colorScheme))
                    
                    Text("Safe pace: \(formatCompact(safeDailyAllowance))/d")
                        .font(.caption2)
                        .foregroundColor(Color.appIncome)
                }
                
                Spacer()
                
                if points.indices.contains(selectedIndex) {
                    let pt = points[selectedIndex]
                    VStack(alignment: .trailing, spacing: 1) {
                        Text(pt.dayLabel)
                            .font(.caption2)
                            .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                        Text(formatCompact(pt.currentAmount))
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(AppColors.primary(for: colorScheme))
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(AppColors.surfaceVariant(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                }
            }
            
            // Canvas Plot Area with Y-Axis scale
            HStack(spacing: 8) {
                // Y-Axis scale labels
                VStack(alignment: .trailing) {
                    Text(formatAxis(maxAmount))
                    Spacer()
                    Text(formatAxis(maxAmount / 2))
                    Spacer()
                    Text("\(currencySymbol)0")
                }
                .font(.system(size: 9))
                .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                .frame(width: 40, height: 110)
                
                // Curve Canvas
                GeometryReader { geo in
                    let w = geo.size.width
                    let h = geo.size.height
                    let count = points.count
                    let stepX = count > 1 ? w / CGFloat(count - 1) : w
                    
                    ZStack {
                        // Horizontal dashed grid lines
                        ForEach(0..<3) { i in
                            let y = CGFloat(i) * (h / 2)
                            Path { p in
                                p.move(to: CGPoint(x: 0, y: y))
                                p.addLine(to: CGPoint(x: w, y: y))
                            }
                            .stroke(AppColors.border(for: colorScheme), style: StrokeStyle(lineWidth: 0.8, dash: [4, 4]))
                        }
                        
                        // Safe Daily Allowance Reference Line
                        if safeDailyAllowance > 0 {
                            let ratio = min(max(CGFloat(safeDailyAllowance) / CGFloat(maxAmount), 0.05), 0.95)
                            let safeY = h * (1.0 - ratio)
                            Path { p in
                                p.move(to: CGPoint(x: 0, y: safeY))
                                p.addLine(to: CGPoint(x: w, y: safeY))
                            }
                            .stroke(Color.appIncome.opacity(0.65), style: StrokeStyle(lineWidth: 1.5, dash: [6, 4]))
                        }
                        
                        // Current Week Bezier Curve & Fill
                        if points.count > 1 {
                            let coords: [CGPoint] = points.enumerated().map { idx, pt in
                                let x = CGFloat(idx) * stepX
                                let ratio = min(max(CGFloat(pt.currentAmount) / CGFloat(maxAmount), 0), 1)
                                let y = h * (1.0 - ratio)
                                return CGPoint(x: x, y: y)
                            }
                            
                            // Gradient Area
                            Path { path in
                                path.move(to: CGPoint(x: coords[0].x, y: h))
                                path.addLine(to: coords[0])
                                for i in 0..<coords.count - 1 {
                                    let p0 = coords[i]
                                    let p1 = coords[i + 1]
                                    let ctrl = CGPoint(x: (p0.x + p1.x) / 2, y: p0.y)
                                    let ctrl2 = CGPoint(x: (p0.x + p1.x) / 2, y: p1.y)
                                    path.addCurve(to: p1, control1: ctrl, control2: ctrl2)
                                }
                                path.addLine(to: CGPoint(x: coords.last!.x, y: h))
                                path.closeSubpath()
                            }
                            .fill(
                                LinearGradient(
                                    colors: [AppColors.primary(for: colorScheme).opacity(0.20), AppColors.primary(for: colorScheme).opacity(0.01)],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                            )
                            
                            // Line Path
                            Path { path in
                                path.move(to: coords[0])
                                for i in 0..<coords.count - 1 {
                                    let p0 = coords[i]
                                    let p1 = coords[i + 1]
                                    let ctrl = CGPoint(x: (p0.x + p1.x) / 2, y: p0.y)
                                    let ctrl2 = CGPoint(x: (p0.x + p1.x) / 2, y: p1.y)
                                    path.addCurve(to: p1, control1: ctrl, control2: ctrl2)
                                }
                            }
                            .stroke(AppColors.primary(for: colorScheme), style: StrokeStyle(lineWidth: 2.5, lineCap: .round))
                            
                            // Data Nodes
                            ForEach(0..<coords.count, id: \.self) { idx in
                                let pt = coords[idx]
                                let isSelected = idx == selectedIndex
                                Circle()
                                    .fill(isSelected ? AppColors.primary(for: colorScheme) : AppColors.surface(for: colorScheme))
                                    .frame(width: isSelected ? 10 : 6, height: isSelected ? 10 : 6)
                                    .overlay(
                                        Circle()
                                            .stroke(AppColors.primary(for: colorScheme), lineWidth: isSelected ? 2 : 1)
                                    )
                                    .position(pt)
                            }
                        }
                    }
                }
                .frame(height: 110)
            }
            
            // X-Axis Day Labels
            HStack {
                Spacer().frame(width: 48)
                HStack {
                    ForEach(0..<points.count, id: \.self) { idx in
                        let pt = points[idx]
                        let isSelected = idx == selectedIndex
                        Text(pt.dayLabel)
                            .font(.system(size: 10, weight: isSelected ? .bold : .regular))
                            .foregroundColor(isSelected ? AppColors.primary(for: colorScheme) : AppColors.onSurfaceVariant(for: colorScheme))
                            .frame(maxWidth: .infinity)
                            .onTapGesture {
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
    
    private func formatCompact(_ units: Int64) -> String {
        let major = units / 100
        return "\(currencySymbol)\(major)"
    }
    
    private func formatAxis(_ units: Int64) -> String {
        let major = units / 100
        if major >= 1000 {
            return "\(currencySymbol)\(major / 1000)k"
        }
        return "\(currencySymbol)\(major)"
    }
}
