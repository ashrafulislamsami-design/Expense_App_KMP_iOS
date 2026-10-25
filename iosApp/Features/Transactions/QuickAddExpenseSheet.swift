import SwiftUI

struct QuickAddExpenseSheet: View {
    var currencySymbol: String = "৳"
    @Environment(\.dismiss) private var dismiss
    
    @State private var transactionType: TransactionType = .expense
    @State private var amountString: String = ""
    @State private var selectedAccount: String = "Cash"
    @State private var targetAccount: String = "bKash"
    @State private var selectedCategory: String = "Food & Dining"
    @State private var note: String = ""
    @State private var date: Date = Date()
    
    enum TransactionType: String, CaseIterable, Identifiable {
        case expense = "Expense"
        case income = "Income"
        case transfer = "Transfer"
        
        var id: String { rawValue }
    }
    
    let accounts = ["Cash", "BRAC Bank Astha", "bKash", "City Bank Card", "Nagad"]
    
    let expenseCategories: [(name: String, icon: String, color: Color)] = [
        ("Food & Dining", "fork.knife", .orange),
        ("Groceries", "cart.fill", .green),
        ("Transport", "car.fill", .blue),
        ("Bills & Utilities", "bolt.fill", .yellow),
        ("Shopping", "bag.fill", .pink),
        ("Entertainment", "tv.fill", .purple),
        ("Health", "cross.fill", .red),
        ("Education", "book.fill", .indigo),
        ("Other", "ellipsis.circle.fill", .gray)
    ]
    
    let incomeCategories: [(name: String, icon: String, color: Color)] = [
        ("Salary", "briefcase.fill", .teal),
        ("Freelance", "laptopcomputer", .blue),
        ("Investment", "chart.line.uptrend.xyaxis", .green),
        ("Gift", "gift.fill", .pink),
        ("Other", "ellipsis.circle.fill", .gray)
    ]
    
    var currentMinorUnits: Int64 {
        let clean = amountString.replacingOccurrences(of: ",", with: "").trimmingCharacters(in: .whitespaces)
        guard let decimal = Decimal(string: clean) else { return 0 }
        return NSDecimalNumber(decimal: decimal * 100).int64Value
    }
    
    var body: some View {
        NavigationStack {
            Form {
                // Segmented picker for type
                Section {
                    Picker("Transaction Type", selection: $transactionType) {
                        ForEach(TransactionType.allCases) { type in
                            Text(type.rawValue).tag(type)
                        }
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                    
                    // Amount Input
                    VStack(alignment: .center, spacing: 6) {
                        Text("Amount")
                            .font(.caption)
                            .foregroundColor(AppColors.textSecondary)
                        
                        HStack(alignment: .firstTextBaseline, spacing: 4) {
                            Text(currencySymbol)
                                .font(.system(size: 32, weight: .bold))
                                .foregroundColor(amountColor)
                            
                            TextField("0.00", text: $amountString)
                                .font(.system(size: 40, weight: .bold, design: .rounded))
                                .keyboardType(.decimalPad)
                                .foregroundColor(amountColor)
                                .multilineTextAlignment(.leading)
                                .frame(minWidth: 120)
                        }
                        .frame(maxWidth: .infinity, alignment: .center)
                        
                        if currentMinorUnits > 0 {
                            Text("Minor units: \(currentMinorUnits) poisha")
                                .font(.caption2)
                                .foregroundColor(AppColors.textSecondary)
                        }
                    }
                    .padding(.vertical, SpacingTokens.md)
                }
                
                // Account Selection
                Section(header: Text("Accounts").font(.caption).foregroundColor(AppColors.textSecondary)) {
                    if transactionType == .transfer {
                        Picker("From Account", selection: $selectedAccount) {
                            ForEach(accounts, id: \.self) { acc in
                                Text(acc).tag(acc)
                            }
                        }
                        Picker("To Account", selection: $targetAccount) {
                            ForEach(accounts.filter { $0 != selectedAccount }, id: \.self) { acc in
                                Text(acc).tag(acc)
                            }
                        }
                    } else {
                        Picker(transactionType == .expense ? "Paid From" : "Deposit To", selection: $selectedAccount) {
                            ForEach(accounts, id: \.self) { acc in
                                Text(acc).tag(acc)
                            }
                        }
                    }
                }
                
                // Category Picker (for Expense & Income)
                if transactionType != .transfer {
                    Section(header: Text("Category").font(.caption).foregroundColor(AppColors.textSecondary)) {
                        let categories = transactionType == .expense ? expenseCategories : incomeCategories
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                ForEach(categories, id: \.name) { cat in
                                    Button(action: { selectedCategory = cat.name }) {
                                        VStack(spacing: 6) {
                                            ZStack {
                                                Circle()
                                                    .fill(selectedCategory == cat.name ? cat.color : cat.color.opacity(0.15))
                                                    .frame(width: 48, height: 48)
                                                Image(systemName: cat.icon)
                                                    .foregroundColor(selectedCategory == cat.name ? .white : cat.color)
                                                    .font(.system(size: 20))
                                            }
                                            Text(cat.name)
                                                .font(.caption2)
                                                .foregroundColor(selectedCategory == cat.name ? AppColors.textPrimary : AppColors.textSecondary)
                                                .fontWeight(selectedCategory == cat.name ? .bold : .regular)
                                        }
                                        .frame(width: 72)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding(.vertical, 8)
                        }
                    }
                }
                
                // Note & Date
                Section(header: Text("Details").font(.caption).foregroundColor(AppColors.textSecondary)) {
                    TextField("Add note or memo", text: $note)
                    DatePicker("Date & Time", selection: $date)
                }
            }
            .navigationTitle(transactionType == .expense ? "Add Expense" : (transactionType == .income ? "Add Income" : "Transfer"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        saveTransaction()
                    }
                    .disabled(currentMinorUnits <= 0)
                    .fontWeight(.bold)
                }
            }
        }
    }
    
    private var amountColor: Color {
        switch transactionType {
        case .expense: return AppColors.expenseRed
        case .income: return AppColors.incomeGreen
        case .transfer: return AppColors.brandPrimary
        }
    }
    
    private func saveTransaction() {
        guard currentMinorUnits > 0 else { return }
        // Save via DatabaseManager / SharedCore
        dismiss()
    }
}
