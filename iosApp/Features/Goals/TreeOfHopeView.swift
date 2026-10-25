import SwiftUI

struct TreeOfHopeView: View {
    @Environment(\.dismiss) private var dismiss
    
    // Growth stages matching SharedCore GoalCalculationEngine.kt
    enum GrowthStage: String {
        case seed = "Seed"
        case sprout = "Sprout"
        case sapling = "Young Sapling"
        case tree = "Vibrant Tree"
        case fruitBearing = "Fruit-Bearing Sanctuary"
        case withered = "Needs Care"
        
        var icon: String {
            switch self {
            case .seed: return "circle.dashed"
            case .sprout: return "leaf.fill"
            case .sapling: return "tree.fill"
            case .tree: return "leaf.arrow.triangle.circlepath"
            case .fruitBearing: return "sparkles"
            case .withered: return "exclamationmark.triangle.fill"
            }
        }
    }
    
    @State private var vitalityScore: Int = 88
    @State private var stage: GrowthStage = .tree
    @State private var streakDays: Int = 14
    @State private var isWatering: Bool = false
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: SpacingTokens.lg) {
                    // Tree Visualizer Container
                    ZStack {
                        // Ambient radial glow
                        RadialGradient(
                            colors: [AppColors.incomeGreen.opacity(0.25), Color.clear],
                            center: .center,
                            startRadius: 20,
                            endRadius: 160
                        )
                        .frame(height: 280)
                        
                        VStack(spacing: SpacingTokens.md) {
                            // Tree Illustration Symbol
                            ZStack {
                                Circle()
                                    .fill(
                                        LinearGradient(
                                            colors: [AppColors.incomeGreen.opacity(0.2), AppColors.incomeGreen.opacity(0.05)],
                                            startPoint: .top,
                                            endPoint: .bottom
                                        )
                                    )
                                    .frame(width: 140, height: 140)
                                    .scaleEffect(isWatering ? 1.08 : 1.0)
                                    .animation(.spring(response: 0.4, dampingFraction: 0.6), value: isWatering)
                                
                                Image(systemName: "tree.fill")
                                    .font(.system(size: 80))
                                    .foregroundColor(AppColors.incomeGreen)
                                    .shadow(color: AppColors.incomeGreen.opacity(0.4), radius: 12, x: 0, y: 6)
                                
                                if isWatering {
                                    Image(systemName: "drop.fill")
                                        .foregroundColor(.blue)
                                        .font(.system(size: 24))
                                        .offset(y: -50)
                                        .transition(.opacity.combined(with: .scale))
                                }
                            }
                            
                            VStack(spacing: 4) {
                                Text(stage.rawValue)
                                    .font(.title2)
                                    .fontWeight(.bold)
                                    .foregroundColor(AppColors.textPrimary)
                                
                                Text("\(vitalityScore)% Vitality")
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                    .foregroundColor(AppColors.incomeGreen)
                            }
                        }
                    }
                    .padding(.top, SpacingTokens.md)
                    
                    // Vitality Stats Grid (2 columns)
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: SpacingTokens.md) {
                        VitalityStatCard(
                            title: "Discipline Streak",
                            value: "\(streakDays) Days",
                            subtitle: "Spent within safe daily pace",
                            icon: "flame.fill",
                            tint: .orange
                        )
                        
                        VitalityStatCard(
                            title: "Savings Health",
                            value: "Excellent",
                            subtitle: "88% budget cushion intact",
                            icon: "heart.fill",
                            tint: AppColors.incomeGreen
                        )
                    }
                    
                    // Motivation & Action Card
                    VStack(alignment: .leading, spacing: SpacingTokens.sm) {
                        HStack {
                            Image(systemName: "sparkles")
                                .foregroundColor(AppColors.brandPrimary)
                            Text("Guardian Wisdom")
                                .font(.headline)
                                .foregroundColor(AppColors.textPrimary)
                        }
                        
                        Text("You've spent ৳ 420 less than your safe daily pace for 14 straight days! Keep your non-essential spending under control until your next salary cycle (Nov 01) to evolve into a Fruit-Bearing Sanctuary.")
                            .font(.subheadline)
                            .foregroundColor(AppColors.textSecondary)
                            .lineSpacing(4)
                        
                        Button(action: {
                            withAnimation {
                                isWatering = true
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                                withAnimation { isWatering = false }
                            }
                        }) {
                            HStack {
                                Image(systemName: "drop.fill")
                                Text("Nourish with Daily Check-In")
                            }
                            .font(.subheadline)
                            .fontWeight(.bold)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(AppColors.brandPrimary)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: ShapeTokens.buttonRadius, style: .continuous))
                        }
                        .padding(.top, SpacingTokens.sm)
                    }
                    .padding(SpacingTokens.cardPadding)
                    .background(AppColors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: ShapeTokens.cardRadius, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: ShapeTokens.cardRadius, style: .continuous)
                            .stroke(AppColors.cardBorder, lineWidth: 1)
                    )
                }
                .padding(.horizontal, SpacingTokens.screenPadding)
                .padding(.bottom, SpacingTokens.lg)
            }
            .background(AppColors.background.ignoresSafeArea())
            .navigationTitle("Tree of Hope")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

struct VitalityStatCard: View {
    let title: String
    let value: String
    let subtitle: String
    let icon: String
    let tint: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ZStack {
                Circle()
                    .fill(tint.opacity(0.15))
                    .frame(width: 36, height: 36)
                Image(systemName: icon)
                    .foregroundColor(tint)
                    .font(.system(size: 16, weight: .bold))
            }
            
            Text(value)
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(AppColors.textPrimary)
            
            Text(title)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(AppColors.textSecondary)
            
            Text(subtitle)
                .font(.caption2)
                .foregroundColor(AppColors.textSecondary.opacity(0.8))
        }
        .padding(SpacingTokens.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: ShapeTokens.cardRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ShapeTokens.cardRadius, style: .continuous)
                .stroke(AppColors.cardBorder, lineWidth: 1)
        )
    }
}
