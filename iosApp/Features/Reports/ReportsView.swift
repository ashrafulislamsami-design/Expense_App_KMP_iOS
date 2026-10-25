import SwiftUI

struct ReportsView: View {
    @State private var selectedPeriod: ReportPeriod = .thisMonth
    @State private var showingExportSheet: Bool = false
    @State private var exportDocumentURL: URL? = nil
    
    enum ReportPeriod: String, CaseIterable, Identifiable {
        case thisMonth = "Current Cycle"
        case lastMonth = "Last Cycle"
        case yearToDate = "Year to Date"
        
        var id: String { rawValue }
    }
    
    // Sample minor unit cashflow data
    let totalIncomeMinor: Int64 = 8500000
    let totalExpensesMinor: Int64 = 4250000
    var netSavingsMinor: Int64 { totalIncomeMinor - totalExpensesMinor }
    var savingsRate: Double {
        guard totalIncomeMinor > 0 else { return 0 }
        return Double(netSavingsMinor) / Double(totalIncomeMinor)
    }
    
    let categoryBreakdown: [(name: String, amountMinor: Int64, percentage: Double, color: Color)] = [
        ("Groceries", 1450000, 0.34, .green),
        ("Food & Dining", 1280000, 0.30, .orange),
        ("Bills & Utilities", 820000, 0.19, .yellow),
        ("Transport", 420000, 0.10, .blue),
        ("Entertainment", 280000, 0.07, .purple)
    ]
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: SpacingTokens.lg) {
                    // Period Picker
                    Picker("Reporting Cycle", selection: $selectedPeriod) {
                        ForEach(ReportPeriod.allCases) { period in
                            Text(period.rawValue).tag(period)
                        }
                    }
                    .pickerStyle(.segmented)
                    
                    // Cashflow Overview Card
                    VStack(alignment: .leading, spacing: SpacingTokens.md) {
                        Text("Cashflow Performance")
                            .font(.headline)
                            .foregroundColor(AppColors.textPrimary)
                        
                        HStack(spacing: SpacingTokens.md) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Total Inflow")
                                    .font(.caption)
                                    .foregroundColor(AppColors.textSecondary)
                                MoneyText(minorUnits: totalIncomeMinor, currencySymbol: "৳", font: .title3, weight: .bold, color: AppColors.incomeGreen)
                            }
                            
                            Spacer()
                            
                            VStack(alignment: .trailing, spacing: 4) {
                                Text("Total Outflow")
                                    .font(.caption)
                                    .foregroundColor(AppColors.textSecondary)
                                MoneyText(minorUnits: totalExpensesMinor, currencySymbol: "৳", font: .title3, weight: .bold, color: AppColors.expenseRed)
                            }
                        }
                        
                        Divider()
                        
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Net Retained Savings")
                                    .font(.caption)
                                    .foregroundColor(AppColors.textSecondary)
                                MoneyText(minorUnits: netSavingsMinor, currencySymbol: "৳", font: .headline, weight: .bold, color: AppColors.brandPrimary)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 2) {
                                Text("Savings Rate")
                                    .font(.caption)
                                    .foregroundColor(AppColors.textSecondary)
                                Text("\(Int(savingsRate * 100))%")
                                    .font(.headline)
                                    .fontWeight(.bold)
                                    .foregroundColor(AppColors.incomeGreen)
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
                    
                    // Donut Visual Breakdown
                    DonutChartView(
                        slices: categoryBreakdown.map {
                            DonutSliceData(name: $0.name, amountMinor: $0.amountMinor, color: $0.color)
                        },
                        totalAmountMinor: totalExpensesMinor,
                        centerLabel: "Total Outflow"
                    )
                    
                    // Export Section
                    VStack(spacing: SpacingTokens.sm) {
                        Button(action: generateCSVExport) {
                            HStack {
                                Image(systemName: "tablecells.badge.ellipsis")
                                Text("Export Cycle Data as CSV")
                            }
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(AppColors.surfaceSecondary)
                            .foregroundColor(AppColors.textPrimary)
                            .clipShape(RoundedRectangle(cornerRadius: ShapeTokens.buttonRadius, style: .continuous))
                        }
                        
                        Button(action: generatePDFExport) {
                            HStack {
                                Image(systemName: "doc.text.fill")
                                Text("Generate Executive Summary PDF")
                            }
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(AppColors.brandPrimary.opacity(0.12))
                            .foregroundColor(AppColors.brandPrimary)
                            .clipShape(RoundedRectangle(cornerRadius: ShapeTokens.buttonRadius, style: .continuous))
                        }
                    }
                }
                .padding(.horizontal, SpacingTokens.screenPadding)
                .padding(.vertical, SpacingTokens.md)
            }
            .background(AppColors.background.ignoresSafeArea())
            .navigationTitle("Analytics & Reports")
        }
    }
    
    private func generateCSVExport() {
        var csv = "Date,Title,Category,Account,Amount (BDT),Type\n"
        csv += "2026-10-25,Shwapno Grocery,Groceries,Cash,-4500.00,EXPENSE\n"
        csv += "2026-10-25,BRAC Bank Transfer,Transfer,Astha -> bKash,15000.00,TRANSFER\n"
        csv += "2026-10-24,Consulting Retainer,Salary,BRAC Bank,85000.00,INCOME\n"
        
        let path = FileManager.default.temporaryDirectory.appendingPathComponent("Expense_Report_\(Date().formatted(.iso8601)).csv")
        try? csv.write(to: path, atomically: true, encoding: .utf8)
        exportDocumentURL = path
    }
    
    private func generatePDFExport() {
        // PDF summary creation trigger
    }
}
