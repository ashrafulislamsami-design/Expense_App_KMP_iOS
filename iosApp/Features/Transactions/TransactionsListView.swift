import SwiftUI

struct TransactionsListView: View {
    @State private var searchText: String = ""
    @State private var selectedFilter: TransactionFilter = .all
    @State private var showingAddExpense: Bool = false
    @State private var showingFilterSheet: Bool = false
    
    enum TransactionFilter: String, CaseIterable, Identifiable {
        case all = "All"
        case expense = "Expenses"
        case income = "Income"
        case transfer = "Transfers"
        
        var id: String { rawValue }
    }
    
    // Sample state until connected to DatabaseManager / SharedCore
    @State private var transactions: [TransactionItem] = [
        TransactionItem(id: "tx-1", title: "Shwapno Grocery", category: "Groceries", account: "Cash", amountMinor: -450000, date: "Today, 2:30 PM", icon: "cart.fill", iconColor: .green, type: .expense),
        TransactionItem(id: "tx-2", title: "BRAC Bank Transfer", category: "Transfer", account: "Astha -> bKash", amountMinor: 1500000, date: "Today, 11:15 AM", icon: "arrow.left.arrow.right", iconColor: .blue, type: .transfer),
        TransactionItem(id: "tx-3", title: "Consulting Retainer", category: "Salary", account: "BRAC Bank", amountMinor: 8500000, date: "Yesterday", icon: "briefcase.fill", iconColor: .teal, type: .income),
        TransactionItem(id: "tx-4", title: "Pathao Ride", category: "Transport", account: "bKash", amountMinor: -35000, date: "Yesterday", icon: "car.fill", iconColor: .orange, type: .expense),
        TransactionItem(id: "tx-5", title: "Netflix Subscription", category: "Entertainment", account: "City Bank Card", amountMinor: -120000, date: "Oct 22, 2026", icon: "tv.fill", iconColor: .purple, type: .expense),
        TransactionItem(id: "tx-6", title: "Electricity Bill (DESCO)", category: "Utilities", account: "bKash", amountMinor: -342000, date: "Oct 20, 2026", icon: "bolt.fill", iconColor: .yellow, type: .expense)
    ]
    
    var filteredTransactions: [TransactionItem] {
        transactions.filter { tx in
            let matchesFilter: Bool
            switch selectedFilter {
            case .all: matchesFilter = true
            case .expense: matchesFilter = tx.type == .expense
            case .income: matchesFilter = tx.type == .income
            case .transfer: matchesFilter = tx.type == .transfer
            }
            if searchText.isEmpty { return matchesFilter }
            return matchesFilter && (tx.title.localizedCaseInsensitiveContains(searchText) || tx.category.localizedCaseInsensitiveContains(searchText) || tx.account.localizedCaseInsensitiveContains(searchText))
        }
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search & Filter header
                VStack(spacing: SpacingTokens.sm) {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(AppColors.textSecondary)
                        TextField("Search transactions, accounts, notes...", text: $searchText)
                            .font(.subheadline)
                        if !searchText.isEmpty {
                            Button(action: { searchText = "" }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(AppColors.textSecondary)
                            }
                        }
                    }
                    .padding(10)
                    .background(AppColors.surfaceSecondary)
                    .clipShape(RoundedRectangle(cornerRadius: ShapeTokens.buttonRadius, style: .continuous))
                    
                    // Filter Chips
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(TransactionFilter.allCases) { filter in
                                Button(action: {
                                    withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                                        selectedFilter = filter
                                    }
                                }) {
                                    Text(filter.rawValue)
                                        .font(.caption)
                                        .fontWeight(selectedFilter == filter ? .semibold : .medium)
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 8)
                                        .background(selectedFilter == filter ? AppColors.brandPrimary : AppColors.surfaceSecondary)
                                        .foregroundColor(selectedFilter == filter ? .white : AppColors.textPrimary)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, SpacingTokens.md)
                .padding(.vertical, SpacingTokens.sm)
                .background(AppColors.surface)
                
                Divider()
                
                // Transactions List
                if filteredTransactions.isEmpty {
                    VStack(spacing: SpacingTokens.md) {
                        Spacer()
                        Image(systemName: "tray.fill")
                            .font(.system(size: 48))
                            .foregroundColor(AppColors.textSecondary.opacity(0.6))
                        Text("No Transactions Found")
                            .font(.headline)
                            .foregroundColor(AppColors.textPrimary)
                        Text("Try adjusting your search terms or filter selection.")
                            .font(.subheadline)
                            .foregroundColor(AppColors.textSecondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                        Spacer()
                    }
                } else {
                    List {
                        ForEach(filteredTransactions) { tx in
                            TransactionDetailRow(item: tx)
                                .listRowInsets(EdgeInsets(top: 10, leading: 16, bottom: 10, trailing: 16))
                                .listRowBackground(Color.clear)
                                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                    Button(role: .destructive) {
                                        deleteTransaction(tx)
                                    } label: {
                                        Label("Delete", systemImage: "trash")
                                    }
                                    Button {
                                        // Edit
                                    } label: {
                                        Label("Edit", systemImage: "pencil")
                                    }
                                    .tint(.blue)
                                }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .background(AppColors.background.ignoresSafeArea())
            .navigationTitle("Activity Ledger")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingAddExpense = true }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(AppColors.brandPrimary)
                    }
                }
            }
            .sheet(isPresented: $showingAddExpense) {
                QuickAddExpenseSheet()
            }
        }
    }
    
    private func deleteTransaction(_ item: TransactionItem) {
        withAnimation {
            transactions.removeAll { $0.id == item.id }
        }
    }
}

struct TransactionItem: Identifiable {
    let id: String
    let title: String
    let category: String
    let account: String
    let amountMinor: Int64
    let date: String
    let icon: String
    let iconColor: Color
    let type: TransactionType
    
    enum TransactionType {
        case expense, income, transfer, refund
    }
}

struct TransactionDetailRow: View {
    let item: TransactionItem
    
    var body: some View {
        HStack(spacing: SpacingTokens.md) {
            ZStack {
                Circle()
                    .fill(item.iconColor.opacity(0.15))
                    .frame(width: 44, height: 44)
                Image(systemName: item.icon)
                    .foregroundColor(item.iconColor)
                    .font(.system(size: 18, weight: .semibold))
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.body)
                    .fontWeight(.medium)
                    .foregroundColor(AppColors.textPrimary)
                    .lineLimit(1)
                
                HStack(spacing: 6) {
                    Text(item.category)
                        .font(.caption2)
                        .fontWeight(.medium)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(AppColors.surfaceSecondary)
                        .clipShape(Capsule())
                    
                    Text("•")
                        .font(.caption2)
                        .foregroundColor(AppColors.textSecondary)
                    
                    Text(item.account)
                        .font(.caption)
                        .foregroundColor(AppColors.textSecondary)
                        .lineLimit(1)
                }
            }
            
            Spacer()
            
            VStack(alignment: .trailing, spacing: 4) {
                MoneyText(
                    minorUnits: item.amountMinor,
                    currencySymbol: "৳",
                    showSign: true,
                    font: .callout,
                    weight: .bold,
                    color: item.type == .income ? AppColors.incomeGreen : (item.type == .transfer ? AppColors.brandPrimary : AppColors.expenseRed)
                )
                
                Text(item.date)
                    .font(.caption2)
                    .foregroundColor(AppColors.textSecondary)
            }
        }
        .padding(.vertical, 4)
    }
}
