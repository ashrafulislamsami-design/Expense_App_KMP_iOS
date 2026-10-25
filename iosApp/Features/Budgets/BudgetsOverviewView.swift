import SwiftUI

struct BudgetsOverviewView: View {
    @State private var showingAddBudget: Bool = false
    
    // Sample state representing budgets with integer minor units
    @State private var budgets: [CategoryBudget] = [
        CategoryBudget(id: "b-1", categoryName: "Groceries", allocatedMinor: 2500000, spentMinor: 1450000, icon: "cart.fill", color: .green),
        CategoryBudget(id: "b-2", categoryName: "Food & Dining", allocatedMinor: 1500000, spentMinor: 1280000, icon: "fork.knife", color: .orange),
        CategoryBudget(id: "b-3", categoryName: "Transport", allocatedMinor: 800000, spentMinor: 520000, icon: "car.fill", color: .blue),
        CategoryBudget(id: "b-4", categoryName: "Bills & Utilities", allocatedMinor: 1200000, spentMinor: 1150000, icon: "bolt.fill", color: .yellow),
        CategoryBudget(id: "b-5", categoryName: "Entertainment", allocatedMinor: 500000, spentMinor: 560000, icon: "tv.fill", color: .purple)
    ]
    
    var totalAllocatedMinor: Int64 {
        budgets.reduce(0) { $0 + $1.allocatedMinor }
    }
    
    var totalSpentMinor: Int64 {
        budgets.reduce(0) { $0 + $1.spentMinor }
    }
    
    var totalRemainingMinor: Int64 {
        totalAllocatedMinor - totalSpentMinor
    }
    
    var overallProgress: Double {
        guard totalAllocatedMinor > 0 else { return 0 }
        return Double(totalSpentMinor) / Double(totalAllocatedMinor)
    }
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: SpacingTokens.lg) {
                    // Overall Budget Envelope Card
                    VStack(alignment: .leading, spacing: SpacingTokens.md) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Monthly Budget Envelope")
                                    .font(.caption)
                                    .fontWeight(.semibold)
                                    .textCase(.uppercase)
                                    .foregroundColor(AppColors.textSecondary)
                                Text("Ceiling & Pace")
                                    .font(.title3)
                                    .fontWeight(.bold)
                                    .foregroundColor(AppColors.textPrimary)
                            }
                            Spacer()
                            Text("\(Int(overallProgress * 100))% Spent")
                                .font(.callout)
                                .fontWeight(.bold)
                                .foregroundColor(overallProgress > 1.0 ? AppColors.expenseRed : AppColors.brandPrimary)
                        }
                        
                        // Progress Bar
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule()
                                    .fill(AppColors.surfaceSecondary)
                                    .frame(height: 12)
                                
                                Capsule()
                                    .fill(overallProgress > 1.0 ? AppColors.expenseRed : (overallProgress > 0.85 ? AppColors.warningYellow : AppColors.brandPrimary))
                                    .frame(width: min(geo.size.width * CGFloat(min(overallProgress, 1.0)), geo.size.width), height: 12)
                            }
                        }
                        .frame(height: 12)
                        
                        Divider()
                            .padding(.vertical, 2)
                        
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Total Allocated")
                                    .font(.caption2)
                                    .foregroundColor(AppColors.textSecondary)
                                MoneyText(minorUnits: totalAllocatedMinor, currencySymbol: "৳", font: .subheadline, weight: .semibold)
                            }
                            Spacer()
                            VStack(alignment: .center, spacing: 2) {
                                Text("Spent So Far")
                                    .font(.caption2)
                                    .foregroundColor(AppColors.textSecondary)
                                MoneyText(minorUnits: totalSpentMinor, currencySymbol: "৳", font: .subheadline, weight: .semibold, color: AppColors.expenseRed)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 2) {
                                Text("Remaining")
                                    .font(.caption2)
                                    .foregroundColor(AppColors.textSecondary)
                                MoneyText(minorUnits: totalRemainingMinor, currencySymbol: "৳", font: .subheadline, weight: .bold, color: totalRemainingMinor >= 0 ? AppColors.incomeGreen : AppColors.expenseRed)
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
                    .shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 3)
                    
                    // Category Budgets Header
                    HStack {
                        Text("Category Ceilings")
                            .font(.headline)
                            .foregroundColor(AppColors.textPrimary)
                        Spacer()
                        Text("\(budgets.count) Categories")
                            .font(.caption)
                            .foregroundColor(AppColors.textSecondary)
                    }
                    
                    // Category Budgets List
                    VStack(spacing: SpacingTokens.md) {
                        ForEach(budgets) { budget in
                            BudgetRowCard(budget: budget)
                        }
                    }
                }
                .padding(.horizontal, SpacingTokens.screenPadding)
                .padding(.vertical, SpacingTokens.md)
            }
            .background(AppColors.background.ignoresSafeArea())
            .navigationTitle("Budgets & Ceilings")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingAddBudget = true }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(AppColors.brandPrimary)
                    }
                }
            }
            .sheet(isPresented: $showingAddBudget) {
                AddBudgetSheet()
            }
        }
    }
}

struct CategoryBudget: Identifiable {
    let id: String
    let categoryName: String
    let allocatedMinor: Int64
    let spentMinor: Int64
    let icon: String
    let color: Color
    
    var remainingMinor: Int64 {
        allocatedMinor - spentMinor
    }
    
    var progress: Double {
        guard allocatedMinor > 0 else { return 0 }
        return Double(spentMinor) / Double(allocatedMinor)
    }
    
    var isOverBudget: Bool {
        spentMinor > allocatedMinor
    }
}

struct BudgetRowCard: View {
    let budget: CategoryBudget
    
    var body: some View {
        VStack(spacing: SpacingTokens.sm) {
            HStack(spacing: SpacingTokens.sm) {
                ZStack {
                    Circle()
                        .fill(budget.color.opacity(0.15))
                        .frame(width: 40, height: 40)
                    Image(systemName: budget.icon)
                        .foregroundColor(budget.color)
                        .font(.system(size: 16, weight: .semibold))
                }
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(budget.categoryName)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(AppColors.textPrimary)
                    
                    Text("\(Int(budget.progress * 100))% used")
                        .font(.caption2)
                        .foregroundColor(budget.isOverBudget ? AppColors.expenseRed : AppColors.textSecondary)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 2) {
                    HStack(spacing: 4) {
                        MoneyText(minorUnits: budget.spentMinor, currencySymbol: "৳", font: .subheadline, weight: .bold, color: budget.isOverBudget ? AppColors.expenseRed : AppColors.textPrimary)
                        Text("/ ")
                            .font(.caption)
                            .foregroundColor(AppColors.textSecondary)
                        MoneyText(minorUnits: budget.allocatedMinor, currencySymbol: "৳", font: .caption, weight: .medium, color: AppColors.textSecondary)
                    }
                    
                    Text(budget.isOverBudget ? "Over by ৳\(abs(budget.remainingMinor) / 100)" : "Remaining: ৳\(budget.remainingMinor / 100)")
                        .font(.caption2)
                        .foregroundColor(budget.isOverBudget ? AppColors.expenseRed : AppColors.incomeGreen)
                }
            }
            
            // Progress Bar
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(AppColors.surfaceSecondary)
                        .frame(height: 8)
                    
                    Capsule()
                        .fill(budget.isOverBudget ? AppColors.expenseRed : (budget.progress > 0.85 ? AppColors.warningYellow : budget.color))
                        .frame(width: min(geo.size.width * CGFloat(min(budget.progress, 1.0)), geo.size.width), height: 8)
                }
            }
            .frame(height: 8)
        }
        .padding(SpacingTokens.cardPadding)
        .background(AppColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: ShapeTokens.cardRadius, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: ShapeTokens.cardRadius, style: .continuous)
                .stroke(budget.isOverBudget ? AppColors.expenseRed.opacity(0.3) : AppColors.cardBorder, lineWidth: 1)
        )
    }
}

struct AddBudgetSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var categoryName: String = "Groceries"
    @State private var amountString: String = ""
    
    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Category")) {
                    TextField("Category Name", text: $categoryName)
                }
                Section(header: Text("Monthly Ceiling")) {
                    HStack {
                        Text("৳").font(.headline).foregroundColor(AppColors.textSecondary)
                        TextField("Amount (e.g. 15000)", text: $amountString)
                            .keyboardType(.numberPad)
                    }
                }
            }
            .navigationTitle("New Budget")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { dismiss() }
                        .disabled(amountString.isEmpty)
                }
            }
        }
    }
}
