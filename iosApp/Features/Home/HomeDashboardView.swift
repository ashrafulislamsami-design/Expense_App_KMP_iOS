import SwiftUI

struct HomeDashboardView: View {
    @Environment(\.colorScheme) private var colorScheme
    
    @State private var selectedTimeframe: String = "Current cycle"
    @AppStorage("userName") private var userName: String = "User"
    @AppStorage("userAvatar") private var userAvatar: String = "🧑‍💻"
    @State private var showingQuickAdd: Bool = false
    @State private var showingSetup: Bool = false
    @State private var donutMode: Int = 0
    
    @AppStorage("monthlyIncomeMinor") private var storedIncomeMinor: Int = 5000000
    @AppStorage("monthlyBudgetMinor") private var storedBudgetMinor: Int = 3500000
    @AppStorage("salaryPayday") private var storedSalaryPayday: Int = 1
    @AppStorage("cycleSpentMinor") private var storedCycleSpentMinor: Int = 0
    
    // Live reactive database state
    @State private var currencySymbol: String = "৳"
    @State private var totalNetBalance: Int64 = 0
    @State private var availableBudget: Int64 = 0
    @State private var periodSpent: Int64 = 0
    @State private var safeToday: Int64 = 0
    @State private var savingsRate: Int = 0
    @State private var daysRemaining: Int = 20
    @State private var recentTransactions: [TransactionRecord] = []
    @State private var walletBalances: [WalletBalanceRecord] = []
    @State private var categoryBreakdown: [CategoryBreakdownRecord] = []
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: AppSpacing.md) {
                    // 1. Master Financial Command Center
                    executiveHeroSection
                    
                    // 2. Account Wallets Carousel
                    walletsCarouselSection
                    
                    // 3. 7-Day Spending Pulse & Safe Pace Bezier Curve
                    PulseCurveChartView(
                        points: dynamicWeeklyPoints,
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
                            Text("Hello, \(userName)")
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(AppColors.onSurface(for: colorScheme))
                                .lineLimit(1)
                            Text("Cycle · \(daysRemaining)d left")
                                .font(.caption2)
                                .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                        }
                    }
                }
                
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: { showingQuickAdd = true }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 22, weight: .semibold))
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
            .onAppear(perform: loadDataFromDatabase)
            .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("DatabaseDataChanged"))) { _ in
                loadDataFromDatabase()
            }
        }
    }
    
    private func loadDataFromDatabase() {
        let db = DatabaseManager.shared
        let net = db.fetchNetBalanceMinor()
        let spent = db.fetchTotalSpentMinor()
        let records = db.fetchAllTransactions()
        
        // Days remaining calculation in current cycle
        let calendar = Calendar.current
        let today = calendar.component(.day, from: Date())
        let range = calendar.range(of: .day, in: .month, for: Date())?.count ?? 30
        let payday = storedSalaryPayday
        if today >= payday {
            daysRemaining = max(1, range - today + payday)
        } else {
            daysRemaining = max(1, payday - today)
        }
        
        let budgetMinor = Int64(storedBudgetMinor)
        
        totalNetBalance = net
        periodSpent = spent
        availableBudget = max(0, budgetMinor - spent)
        safeToday = max(0, availableBudget / Int64(max(1, daysRemaining)))
        
        let incomeMinor = Int64(storedIncomeMinor)
        if incomeMinor > 0 {
            let retention = max(0, incomeMinor - spent)
            savingsRate = min(100, max(0, Int((Double(retention) / Double(incomeMinor)) * 100)))
        } else {
            savingsRate = 0
        }
        
        recentTransactions = Array(records.prefix(5))
        categoryBreakdown = db.fetchCategorySpendingBreakdown()
        walletBalances = db.fetchWalletsBalances()
    }
    
    // Executive Master Financial Command Center
    private var executiveHeroSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Header Row: Tag & Status Badge
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: "wallet.pass.fill")
                        .font(.caption)
                        .foregroundColor(AppColors.primary(for: colorScheme))
                    Text("TOTAL NET BALANCE")
                        .font(.system(size: 11, weight: .bold))
                        .tracking(1.0)
                        .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                }
                Spacer()
                Text("\(daysRemaining)d remaining in cycle")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(AppColors.primary(for: colorScheme))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(AppColors.primary(for: colorScheme).opacity(0.12))
                    .clipShape(Capsule())
            }
            
            // Dominant Tabular Numeral Balance
            Text("\(currencySymbol)\(totalNetBalance / 100)")
                .font(.system(size: 34, weight: .heavy))
                .monospacedDigit()
                .foregroundColor(AppColors.onSurface(for: colorScheme))
            
            let budgetCap = max(1, Int64(storedBudgetMinor))
            let cyclePct = min(100, max(0, Int((Double(availableBudget) / Double(budgetCap)) * 100)))
            Text("\(currencySymbol)\(availableBudget / 100) available to spend · \(cyclePct)% of budget intact")
                .font(.caption)
                .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
            
            Divider()
                .opacity(0.4)
            
            // Three-Pillar Performance Ribbon
            HStack(spacing: 0) {
                // Pillar 1: Daily Pace
                VStack(alignment: .leading, spacing: 2) {
                    Text("SAFE TODAY")
                        .font(.system(size: 10, weight: .bold))
                        .tracking(0.6)
                        .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                    Text("\(currencySymbol)\(safeToday / 100) / d")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .monospacedDigit()
                        .foregroundColor(AppColors.onSurface(for: colorScheme))
                    Text("Daily pace")
                        .font(.system(size: 10))
                        .foregroundColor(Color.appIncome)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                
                Rectangle()
                    .fill(AppColors.border(for: colorScheme).opacity(0.5))
                    .frame(width: 1, height: 32)
                
                // Pillar 2: Cycle Spend
                VStack(alignment: .leading, spacing: 2) {
                    Text("SPENT")
                        .font(.system(size: 10, weight: .bold))
                        .tracking(0.6)
                        .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                    Text("\(currencySymbol)\(periodSpent / 100)")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .monospacedDigit()
                        .foregroundColor(Color.appExpense)
                    Text("Current cycle")
                        .font(.system(size: 10))
                        .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 8)
                
                Rectangle()
                    .fill(AppColors.border(for: colorScheme).opacity(0.5))
                    .frame(width: 1, height: 32)
                
                // Pillar 3: Retained Savings
                VStack(alignment: .trailing, spacing: 2) {
                    Text("SAVINGS")
                        .font(.system(size: 10, weight: .bold))
                        .tracking(0.6)
                        .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                    Text("+ \(savingsRate)%")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .monospacedDigit()
                        .foregroundColor(Color.appSaving)
                    Text("Retention")
                        .font(.system(size: 10))
                        .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                }
                .frame(maxWidth: .infinity, alignment: .trailing)
            }
            
            Divider()
                .opacity(0.4)
            
            // Tactile Quick-Action Bar
            HStack(spacing: 8) {
                Button(action: { showingQuickAdd = true }) {
                    HStack(spacing: 4) {
                        Image(systemName: "plus")
                            .font(.system(size: 11, weight: .bold))
                        Text("Expense")
                            .font(.system(size: 12, weight: .bold))
                    }
                    .foregroundColor(AppColors.onPrimary(for: colorScheme))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(AppColors.primary(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                
                Button(action: { showingQuickAdd = true }) {
                    HStack(spacing: 4) {
                        Image(systemName: "arrow.down")
                            .font(.system(size: 11, weight: .bold))
                        Text("Income")
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .foregroundColor(Color.appIncome)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(AppColors.surfaceVariant(for: colorScheme).opacity(0.7))
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .stroke(AppColors.border(for: colorScheme), lineWidth: 1)
                    )
                }
                
                Button(action: { showingQuickAdd = true }) {
                    HStack(spacing: 4) {
                        Image(systemName: "arrow.left.arrow.right")
                            .font(.system(size: 11, weight: .bold))
                        Text("Transfer")
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .foregroundColor(AppColors.primary(for: colorScheme))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(AppColors.surfaceVariant(for: colorScheme).opacity(0.7))
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .stroke(AppColors.border(for: colorScheme), lineWidth: 1)
                    )
                }
                
                Button(action: { showingSetup = true }) {
                    HStack(spacing: 4) {
                        Image(systemName: "creditcard")
                            .font(.system(size: 11, weight: .bold))
                        Text("Wallets")
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .foregroundColor(AppColors.onSurface(for: colorScheme))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(AppColors.surfaceVariant(for: colorScheme).opacity(0.7))
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .stroke(AppColors.border(for: colorScheme), lineWidth: 1)
                    )
                }
            }
        }
        .padding(16)
        .background(AppColors.surface(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(AppColors.border(for: colorScheme).opacity(0.6), lineWidth: 1)
        )
    }
    
    // Wallets Horizontal Carousel
    private var walletsCarouselSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                if !walletBalances.isEmpty {
                    ForEach(walletBalances) { w in
                        walletItem(name: w.name, amount: w.balanceMinor, color: Color(hex: w.colorHex))
                    }
                } else {
                    walletItem(name: "Cash", amount: 0, color: Color.green)
                    walletItem(name: "Bank", amount: 0, color: Color.blue)
                    walletItem(name: "Mobile Wallet", amount: 0, color: Color.pink)
                }
                
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
                let budgetCap = max(1, Int64(storedBudgetMinor))
                let usedRatio = min(1.0, max(0.0, Double(periodSpent) / Double(budgetCap)))
                let usedPct = Int(usedRatio * 100)
                
                Text("\(usedPct)% used")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.onSurface(for: colorScheme))
                
                ProgressView(value: usedRatio)
                    .tint(usedRatio > 0.9 ? Color.appExpense : AppColors.primary(for: colorScheme))
                
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
            
            if !recentTransactions.isEmpty {
                ForEach(recentTransactions) { tx in
                    let isExp = tx.type.uppercased().contains("EXP") || tx.type.contains("Debt Repayment")
                    let signedAmt = isExp ? -abs(tx.amount) : abs(tx.amount)
                    activityRow(
                        title: tx.note.isEmpty ? tx.category : tx.note,
                        category: tx.category,
                        account: tx.account,
                        amount: signedAmt,
                        date: tx.date
                    )
                }
            } else {
                HStack {
                    Spacer()
                    VStack(spacing: 4) {
                        Image(systemName: "tray")
                            .font(.title3)
                            .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme).opacity(0.5))
                        Text("No transactions logged yet")
                            .font(.caption2)
                            .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                    }
                    .padding(.vertical, 8)
                    Spacer()
                }
            }
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

    private var dynamicWeeklyPoints: [DailyTrendPointDTO] {
        let days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
        if periodSpent == 0 {
            return days.map { DailyTrendPointDTO(dayLabel: $0, currentAmount: 0, previousAmount: 0) }
        }
        
        let avg = periodSpent / 7
        return [
            DailyTrendPointDTO(dayLabel: "Mon", currentAmount: avg * 6 / 10, previousAmount: avg),
            DailyTrendPointDTO(dayLabel: "Tue", currentAmount: avg * 8 / 10, previousAmount: avg * 9 / 10),
            DailyTrendPointDTO(dayLabel: "Wed", currentAmount: avg * 12 / 10, previousAmount: avg),
            DailyTrendPointDTO(dayLabel: "Thu", currentAmount: avg * 5 / 10, previousAmount: avg * 7 / 10),
            DailyTrendPointDTO(dayLabel: "Fri", currentAmount: avg * 14 / 10, previousAmount: avg * 8 / 10),
            DailyTrendPointDTO(dayLabel: "Sat", currentAmount: avg * 9 / 10, previousAmount: avg * 6 / 10),
            DailyTrendPointDTO(dayLabel: "Sun", currentAmount: max(0, periodSpent - (avg * 54 / 10)), previousAmount: avg * 5 / 10)
        ]
    }
    private var currentDonutSlices: [DonutSliceDTO] {
        switch donutMode {
        case 0:
            var list: [DonutSliceDTO] = []
            let totalCap = max(1, Int64(storedBudgetMinor))
            
            if availableBudget > 0 {
                let avPct = min(100, max(1, Int((Double(availableBudget) / Double(totalCap)) * 100)))
                list.append(DonutSliceDTO(name: "Available budget", amount: availableBudget, percentage: avPct, color: Color.appIncome))
            }
            
            if !categoryBreakdown.isEmpty {
                for c in categoryBreakdown {
                    let pct = min(100, max(1, Int((Double(c.amountMinor) / Double(totalCap)) * 100)))
                    list.append(DonutSliceDTO(name: c.name, amount: c.amountMinor, percentage: pct, color: Color(hex: c.colorHex)))
                }
            } else if periodSpent > 0 {
                let spPct = min(100, max(1, Int((Double(periodSpent) / Double(totalCap)) * 100)))
                list.append(DonutSliceDTO(name: "Initial Cycle Spend", amount: periodSpent, percentage: spPct, color: Color.appExpense))
            }
            
            if list.isEmpty {
                list.append(DonutSliceDTO(name: "Available budget", amount: totalCap, percentage: 100, color: Color.appIncome))
            }
            return list
            
        case 1:
            let inc = max(1, Int64(storedIncomeMinor))
            return [
                DonutSliceDTO(name: "Monthly Salary", amount: inc, percentage: 100, color: Color.appIncome)
            ]
        default:
            return [
                DonutSliceDTO(name: "No Outstanding Debt", amount: 0, percentage: 100, color: Color.appIncome)
            ]
        }
    }
}