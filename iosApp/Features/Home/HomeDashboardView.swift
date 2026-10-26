import SwiftUI

struct HomeDashboardView: View {
    @Environment(\.colorScheme) private var colorScheme
    
    @State private var selectedTimeframe: String = "Current cycle"
    @AppStorage("userName") private var userName: String = "User"
    @AppStorage("userAvatar") private var userAvatar: String = "🧑‍💻"
    @State private var showingQuickAdd: Bool = false
    @State private var showingSetup: Bool = false
    @State private var donutMode: Int = 0
    
    // Sample state simulating real database telemetry
    @State private var currencySymbol: String = "৳"
    @State private var totalNetBalance: Int64 = 4250000 // ৳42,500.00
    @State private var availableBudget: Int64 = 2300000 // ৳23,000.00
    @State private var periodSpent: Int64 = 1200000     // ৳12,000.00
    @State private var safeToday: Int64 = 115000        // ৳1,150.00
    @State private var savingsRate: Int = 28            // 28%
    @State private var daysRemaining: Int = 20
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: AppSpacing.md) {
                    // 1. Account Wallets Horizontal Carousel
                    walletsSection
                    
                    // 2. 2x2 Executive KPI Quad Grid
                    kpiQuadGridSection
                    
                    // 3. 7-Day Spending Pulse & Safe Pace Bezier Curve
                    PulseCurveChartView(
                        points: [
                            DailyTrendPointDTO(dayLabel: "Mon", currentAmount: 45000, previousAmount: 60000),
                            DailyTrendPointDTO(dayLabel: "Tue", currentAmount: 80000, previousAmount: 40000),
                            DailyTrendPointDTO(dayLabel: "Wed", currentAmount: 120000, previousAmount: 110000),
                            DailyTrendPointDTO(dayLabel: "Thu", currentAmount: 30000, previousAmount: 90000),
                            DailyTrendPointDTO(dayLabel: "Fri", currentAmount: 150000, previousAmount: 85000),
                            DailyTrendPointDTO(dayLabel: "Sat", currentAmount: 65000, previousAmount: 45000),
                            DailyTrendPointDTO(dayLabel: "Sun", currentAmount: 20000, previousAmount: 30000)
                        ],
                        currentTotal: periodSpent,
                        safeDailyAllowance: safeToday,
                        currencySymbol: currencySymbol
                    )
                    
                    // 4. Budget & Category Donut Ring Breakdown (Spending | Income | Debts)
                    VStack(alignment: .leading, spacing: 8) {
                        Picker("Category Breakdown", selection: $donutMode) {
                            Text("Spending").tag(0)
                            Text("Income").tag(1)
                            Text("Debts & Loans").tag(2)
                        }
                        .pickerStyle(.segmented)
                        
                        DonutChartView(
                            slices: currentDonutSlices,
                            currencySymbol: currencySymbol
                        )
                    }
                    
                    // 5. Budget Health & Savings Momentum Quick Glance
                    budgetAndHopeRow
                    
                    // 6. Recent Activity Structured Ledger
                    recentActivityCard
                }
                .padding(.horizontal, AppSpacing.lg)
                .padding(.vertical, AppSpacing.sm)
            }
            .background(AppColors.background(for: colorScheme))
            .navigationTitle("Dashboard")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    HStack(spacing: 8) {
                        ZStack {
                            Circle()
                                .fill(AppColors.primary(for: colorScheme).opacity(0.15))
                                .frame(width: 32, height: 32)
                            Text(userAvatar)
                                .font(.system(size: 16))
                        }
                        VStack(alignment: .leading, spacing: 1) {
                            Text("Hello, \(userName) 👋")
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(AppColors.onSurface(for: colorScheme))
                            Text("Cycle · \(daysRemaining)d left")
                                .font(.caption2)
                                .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                        }
                    }
                }
                
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: { showingQuickAdd = true }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title3)
                            .foregroundColor(AppColors.primary(for: colorScheme))
                    }
                }
            }
            .sheet(isPresented: $showingQuickAdd) {
                QuickAddExpenseSheet(currencySymbol: currencySymbol)
            }
            .sheet(isPresented: $showingSetup) {
                OnboardingFlowView(isPresentedAsSheet: true)
            }
        }
    }
    
    // Wallets Carousel
    private var walletsSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // Net Balance Pill
                HStack(spacing: 6) {
                    Image(systemName: "wallet.pass")
                        .font(.caption)
                        .foregroundColor(AppColors.primary(for: colorScheme))
                    Text("Net:")
                        .font(.caption2)
                        .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                    Text("\(currencySymbol)\(totalNetBalance / 100)")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.onSurface(for: colorScheme))
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 7)
                .background(AppColors.surfaceVariant(for: colorScheme))
                .clipShape(AppShapes.small)
                
                // Specific Wallets
                walletItem(name: "BRAC Astha", amount: 2800000, color: Color.blue)
                walletItem(name: "bKash", amount: 850000, color: Color.pink)
                walletItem(name: "Card", amount: 400000, color: Color.purple)
                walletItem(name: "Cash", amount: 200000, color: Color.green)
                
                // + Wallets & Setup Button
                Button(action: { showingSetup = true }) {
                    HStack(spacing: 4) {
                        Image(systemName: "plus")
                            .font(.system(size: 10, weight: .bold))
                        Text("Wallets & Setup")
                            .font(.caption2)
                            .fontWeight(.semibold)
                    }
                    .foregroundColor(AppColors.primary(for: colorScheme))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 7)
                    .background(AppColors.surface(for: colorScheme))
                    .clipShape(AppShapes.small)
                    .overlay(
                        AppShapes.small.stroke(AppColors.primary(for: colorScheme).opacity(0.35), lineWidth: 1)
                    )
                }
            }
        }
    }
    
    private func walletItem(name: String, amount: Int64, color: Color) -> some View {
        HStack(spacing: 6) {
            Circle().fill(color).frame(width: 6, height: 6)
            Text(name)
                .font(.caption2)
                .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
            Text("\(currencySymbol)\(amount / 100)")
                .font(.caption2)
                .fontWeight(.semibold)
                .foregroundColor(AppColors.onSurface(for: colorScheme))
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .background(AppColors.surface(for: colorScheme))
        .clipShape(AppShapes.small)
        .overlay(AppShapes.small.stroke(AppColors.border(for: colorScheme), lineWidth: 1))
    }
    
    // 2x2 KPI Quad Grid
    private var kpiQuadGridSection: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                KPICardView(
                    title: "Available",
                    value: "\(currencySymbol)\(availableBudget / 100)",
                    badgeText: "65% left",
                    isBadgePositive: true,
                    subtitle: "Of \(currencySymbol)35k limit",
                    accentColor: AppColors.primary(for: colorScheme)
                )
                
                KPICardView(
                    title: "Spent",
                    value: "\(currencySymbol)\(periodSpent / 100)",
                    badgeText: "-8% vs prev",
                    isBadgePositive: true,
                    subtitle: "In current cycle",
                    accentColor: Color.appExpense
                )
            }
            
            HStack(spacing: 8) {
                KPICardView(
                    title: "Safe today",
                    value: "\(currencySymbol)\(safeToday / 100)",
                    badgeText: "On track",
                    isBadgePositive: true,
                    subtitle: "\(daysRemaining) days left",
                    accentColor: Color.appIncome
                )
                
                KPICardView(
                    title: "Retained Savings",
                    value: "+ \(savingsRate)%",
                    badgeText: "\(currencySymbol)15k",
                    isBadgePositive: true,
                    subtitle: "Retention rate",
                    accentColor: Color.appSaving
                )
            }
        }
    }
    
    private var budgetAndHopeRow: some View {
        HStack(spacing: 8) {
            // Budget Health Card
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("Budget health")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(AppColors.onSurface(for: colorScheme))
                    Spacer()
                    Image(systemName: "chart.pie")
                        .font(.caption)
                        .foregroundColor(AppColors.primary(for: colorScheme))
                }
                Text("34% used")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.onSurface(for: colorScheme))
                
                ProgressView(value: 0.34)
                    .tint(AppColors.primary(for: colorScheme))
                
                Text("\(currencySymbol)\(availableBudget / 100) left")
                    .font(.caption2)
                    .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
            }
            .padding(12)
            .background(AppColors.surface(for: colorScheme))
            .clipShape(AppShapes.medium)
            .overlay(AppShapes.medium.stroke(AppColors.border(for: colorScheme), lineWidth: 1))
            
            // Savings Momentum Card
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("Savings Momentum")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(AppColors.onSurface(for: colorScheme))
                    Spacer()
                    Image(systemName: "leaf.fill")
                        .font(.caption)
                        .foregroundColor(Color.appIncome)
                }
                Text("Flowering")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.onSurface(for: colorScheme))
                
                ProgressView(value: 0.78)
                    .tint(Color.appIncome)
                
                Text("78% health score · 2 active goals")
                    .font(.caption2)
                    .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
            }
            .padding(12)
            .background(AppColors.surface(for: colorScheme))
            .clipShape(AppShapes.medium)
            .overlay(AppShapes.medium.stroke(AppColors.border(for: colorScheme), lineWidth: 1))
        }
    }
    
    private var recentActivityCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Recent activity")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(AppColors.onSurface(for: colorScheme))
                Spacer()
                Button(action: {}) {
                    Text("View all →")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(AppColors.primary(for: colorScheme))
                }
            }
            
            Divider()
            
            activityRow(title: "Supermarket Groceries", category: "Groceries", account: "bKash", amount: -145000, date: "Today")
            activityRow(title: "Monthly Internet Bill", category: "Internet", account: "BRAC Astha", amount: -120000, date: "Yesterday")
            activityRow(title: "Freelance Project Payment", category: "Income", account: "BRAC Astha", amount: 3500000, date: "22 Oct")
        }
        .padding(14)
        .background(AppColors.surface(for: colorScheme))
        .clipShape(AppShapes.medium)
        .overlay(AppShapes.medium.stroke(AppColors.border(for: colorScheme), lineWidth: 1))
    }
    
    private func activityRow(title: String, category: String, account: String, amount: Int64, date: String) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(AppColors.onSurface(for: colorScheme))
                Text("\(date) · \(account)")
                    .font(.caption2)
                    .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
            }
            Spacer()
            MoneyText(
                minorUnits: amount,
                currencySymbol: currencySymbol,
                decimals: 2,
                style: .caption,
                weight: .bold,
                color: amount > 0 ? Color.appIncome : AppColors.onSurface(for: colorScheme)
            )
        }
    }

    private var currentDonutSlices: [DonutSliceDTO] {
        switch donutMode {
        case 0:
            return [
                DonutSliceDTO(name: "Available budget", amount: availableBudget, percentage: 65, color: Color.appIncome),
                DonutSliceDTO(name: "Food & Dining", amount: 450000, percentage: 15, color: Color(red: 245/255, green: 158/255, blue: 11/255)),
                DonutSliceDTO(name: "Transport", amount: 300000, percentage: 10, color: Color(red: 59/255, green: 130/255, blue: 246/255)),
                DonutSliceDTO(name: "Utilities", amount: 200000, percentage: 7, color: Color(red: 239/255, green: 68/255, blue: 68/255)),
                DonutSliceDTO(name: "Shopping", amount: 90000, percentage: 3, color: Color(red: 236/255, green: 72/255, blue: 153/255))
            ]
        case 1:
            return [
                DonutSliceDTO(name: "Monthly Salary", amount: 5000000, percentage: 80, color: Color.appIncome),
                DonutSliceDTO(name: "Freelance", amount: 800000, percentage: 13, color: Color.blue),
                DonutSliceDTO(name: "Investments", amount: 450000, percentage: 7, color: Color.purple)
            ]
        default:
            return [
                DonutSliceDTO(name: "Loan Received", amount: 300000, percentage: 60, color: Color.red),
                DonutSliceDTO(name: "Borrowed Funds", amount: 200000, percentage: 40, color: Color.orange)
            ]
        }
    }
}