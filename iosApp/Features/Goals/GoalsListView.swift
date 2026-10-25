import SwiftUI

struct GoalsListView: View {
    @State private var showingAddGoal: Bool = false
    @State private var showingTreeOfHope: Bool = false
    
    @State private var goals: [SavingsGoalItem] = [
        SavingsGoalItem(id: "g-1", title: "Emergency Fund (6 Months)", targetMinor: 30000000, currentMinor: 18500000, targetDate: "Dec 2026", icon: "shield.lefthalf.filled", color: .blue),
        SavingsGoalItem(id: "g-2", title: "MacBook Pro M-Series", targetMinor: 22000000, currentMinor: 11000000, targetDate: "Nov 2026", icon: "laptopcomputer", color: .indigo),
        SavingsGoalItem(id: "g-3", title: "Family Vacation", targetMinor: 15000000, currentMinor: 6000000, targetDate: "Jan 2027", icon: "airplane.departure", color: .teal)
    ]
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: SpacingTokens.lg) {
                    // Tree of Hope Banner Card
                    TreeOfHopeBannerCard(action: { showingTreeOfHope = true })
                    
                    // Goals Section Header
                    HStack {
                        Text("Active Savings Goals")
                            .font(.headline)
                            .foregroundColor(AppColors.textPrimary)
                        Spacer()
                        Text("\(goals.count) Active")
                            .font(.caption)
                            .foregroundColor(AppColors.textSecondary)
                    }
                    
                    // Goals List
                    VStack(spacing: SpacingTokens.md) {
                        ForEach(goals) { goal in
                            SavingsGoalCard(goal: goal)
                        }
                    }
                }
                .padding(.horizontal, SpacingTokens.screenPadding)
                .padding(.vertical, SpacingTokens.md)
            }
            .background(AppColors.background.ignoresSafeArea())
            .navigationTitle("Goals & Tree of Hope")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingAddGoal = true }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(AppColors.brandPrimary)
                    }
                }
            }
            .sheet(isPresented: $showingTreeOfHope) {
                TreeOfHopeView()
            }
            .sheet(isPresented: $showingAddGoal) {
                AddGoalSheet()
            }
        }
    }
}

struct SavingsGoalItem: Identifiable {
    let id: String
    let title: String
    let targetMinor: Int64
    let currentMinor: Int64
    let targetDate: String
    let icon: String
    let color: Color
    
    var progress: Double {
        guard targetMinor > 0 else { return 0 }
        return Double(currentMinor) / Double(targetMinor)
    }
    
    var remainingMinor: Int64 {
        max(0, targetMinor - currentMinor)
    }
}

struct TreeOfHopeBannerCard: View {
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: SpacingTokens.md) {
                ZStack {
                    Circle()
                        .fill(AppColors.incomeGreen.opacity(0.18))
                        .frame(width: 54, height: 54)
                    Image(systemName: "leaf.circle.fill")
                        .foregroundColor(AppColors.incomeGreen)
                        .font(.system(size: 32))
                }
                
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 6) {
                        Text("Tree of Hope")
                            .font(.headline)
                            .foregroundColor(AppColors.textPrimary)
                        Text("Vibrant")
                            .font(.caption2)
                            .fontWeight(.bold)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(AppColors.incomeGreen.opacity(0.2))
                            .foregroundColor(AppColors.incomeGreen)
                            .clipShape(Capsule())
                    }
                    
                    Text("Vitality: 88% • 14-day spending discipline streak")
                        .font(.caption)
                        .foregroundColor(AppColors.textSecondary)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .foregroundColor(AppColors.textSecondary)
                    .font(.system(size: 14, weight: .semibold))
            }
            .padding(SpacingTokens.cardPadding)
            .background(
                LinearGradient(
                    colors: [AppColors.surface, AppColors.incomeGreen.opacity(0.08)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: ShapeTokens.cardRadius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: ShapeTokens.cardRadius, style: .continuous)
                    .stroke(AppColors.incomeGreen.opacity(0.3), lineWidth: 1.5)
            )
            .shadow(color: Color.black.opacity(0.03), radius: 6, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }
}

struct SavingsGoalCard: View {
    let goal: SavingsGoalItem
    
    var body: some View {
        VStack(spacing: SpacingTokens.sm) {
            HStack(spacing: SpacingTokens.sm) {
                ZStack {
                    Circle()
                        .fill(goal.color.opacity(0.15))
                        .frame(width: 44, height: 44)
                    Image(systemName: goal.icon)
                        .foregroundColor(goal.color)
                        .font(.system(size: 18, weight: .semibold))
                }
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(goal.title)
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.textPrimary)
                    
                    Text("Target: \(goal.targetDate)")
                        .font(.caption2)
                        .foregroundColor(AppColors.textSecondary)
                }
                
                Spacer()
                
                Text("\(Int(goal.progress * 100))%")
                    .font(.callout)
                    .fontWeight(.bold)
                    .foregroundColor(goal.color)
            }
            
            // Progress Bar
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(AppColors.surfaceSecondary)
                        .frame(height: 10)
                    
                    Capsule()
                        .fill(goal.color)
                        .frame(width: min(geo.size.width * CGFloat(min(goal.progress, 1.0)), geo.size.width), height: 10)
                }
            }
            .frame(height: 10)
            
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Saved")
                        .font(.caption2)
                        .foregroundColor(AppColors.textSecondary)
                    MoneyText(minorUnits: goal.currentMinor, currencySymbol: "৳", font: .caption, weight: .bold, color: goal.color)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Goal Target")
                        .font(.caption2)
                        .foregroundColor(AppColors.textSecondary)
                    MoneyText(minorUnits: goal.targetMinor, currencySymbol: "৳", font: .caption, weight: .semibold, color: AppColors.textPrimary)
                }
            }
        }
        .padding(SpacingTokens.cardPadding)
        .background(AppColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: ShapeTokens.cardRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ShapeTokens.cardRadius, style: .continuous)
                .stroke(AppColors.cardBorder, lineWidth: 1)
        )
    }
}

struct AddGoalSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var goalTitle: String = ""
    @State private var targetAmount: String = ""
    
    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Goal Information")) {
                    TextField("Goal Title (e.g. Emergency Fund)", text: $goalTitle)
                    HStack {
                        Text("৳").foregroundColor(AppColors.textSecondary)
                        TextField("Target Amount", text: $targetAmount)
                            .keyboardType(.numberPad)
                    }
                }
            }
            .navigationTitle("New Savings Goal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { dismiss() }
                        .disabled(goalTitle.isEmpty || targetAmount.isEmpty)
                }
            }
        }
    }
}
