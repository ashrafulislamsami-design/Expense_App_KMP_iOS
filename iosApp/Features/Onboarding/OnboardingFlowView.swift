import SwiftUI

struct WalletOption: Identifiable {
    let id = UUID()
    let name: String
    let subtitle: String
    let color: Color
    let iconName: String
}

struct OnboardingFlowView: View {
    var isPresentedAsSheet: Bool = false
    var onCompleted: (() -> Void)? = nil
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    
    @State private var currentStep: Int = 1
    private let totalSteps: Int = 5
    
    // Form States
    @State private var selectedWallets: Set<String> = ["Cash", "BRAC Bank Astha", "bKash", "Card"]
    @State private var customWalletName: String = ""
    @State private var monthlyIncome: String = "50000"
    @State private var selectedPayDay: Int = 1
    @State private var monthlyBudget: String = "35000"
    @State private var alreadySpentThisCycle: String = ""
    @State private var spentWallet: String = "BRAC Bank Astha"
    
    private let presets: [WalletOption] = [
        WalletOption(name: "Cash", subtitle: "Physical wallet", color: .green, iconName: "banknote"),
        WalletOption(name: "BRAC Bank Astha", subtitle: "BRAC Bank app & A/C", color: .blue, iconName: "building.columns"),
        WalletOption(name: "bKash", subtitle: "Mobile wallet", color: .pink, iconName: "iphone"),
        WalletOption(name: "Card", subtitle: "Debit / Credit Card", color: .purple, iconName: "creditcard"),
        WalletOption(name: "Bank Account", subtitle: "Salary / Savings bank", color: .cyan, iconName: "building.columns"),
        WalletOption(name: "Citytouch", subtitle: "City Bank digital", color: .red, iconName: "building.columns"),
        WalletOption(name: "Nagad", subtitle: "Mobile wallet", color: .orange, iconName: "iphone"),
        WalletOption(name: "Rocket / Upay", subtitle: "Mobile banking", color: .indigo, iconName: "iphone"),
        WalletOption(name: "DBBL NexusPay", subtitle: "Dutch-Bangla Bank", color: .teal, iconName: "creditcard")
    ]
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Progress Bar
                ProgressView(value: Double(currentStep), total: Double(totalSteps))
                    .tint(AppColors.primary(for: colorScheme))
                    .padding(.horizontal)
                    .padding(.top, 8)
                
                // Step Content
                TabView(selection: $currentStep) {
                    step1Welcome.tag(1)
                    step2MultiWallets.tag(2)
                    step3SalaryAndPayday.tag(3)
                    step4BudgetAndMidCycle.tag(4)
                    step5Summary.tag(5)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(.easeInOut, value: currentStep)
            }
            .background(AppColors.background(for: colorScheme))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    if currentStep > 1 {
                        Button("Back") { currentStep -= 1 }
                            .foregroundColor(AppColors.primary(for: colorScheme))
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Skip") { finishOnboarding() }
                        .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                }
            }
        }
    }
    
    // Step 1: Welcome
    private var step1Welcome: some View {
        VStack(spacing: 20) {
            Spacer()
            
            Image(systemName: "chart.line.uptrend.xyaxis.circle.fill")
                .resizable()
                .frame(width: 72, height: 72)
                .foregroundColor(AppColors.primary(for: colorScheme))
            
            Text("Salary & Expense Command Center")
                .font(.title2)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
                .foregroundColor(AppColors.onSurface(for: colorScheme))
            
            Text("Select all your wallets (BRAC Bank Astha, bKash, Cards, Bank, Cash), set your monthly budget, and track your mid-cycle spending at a glance.")
                .font(.body)
                .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Image(systemName: "lock.shield.fill")
                        .foregroundColor(Color.appIncome)
                    Text("100% Private & Offline-First")
                        .font(.subheadline)
                        .fontWeight(.bold)
                }
                Text("All your records remain stored exclusively on your device. No cloud sync, zero ads, zero internet telemetry.")
                    .font(.caption)
                    .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
            }
            .padding()
            .background(AppColors.surface(for: colorScheme))
            .clipShape(AppShapes.medium)
            .overlay(AppShapes.medium.stroke(AppColors.border(for: colorScheme), lineWidth: 1))
            .padding(.horizontal)
            
            Spacer()
            
            Button(action: { currentStep = 2 }) {
                Text("Get Started")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.primary(for: colorScheme))
                    .clipShape(AppShapes.medium)
            }
            .padding(.horizontal)
            .padding(.bottom, 24)
        }
    }
    
    // Step 2: Multi-Wallet Selection
    private var step2MultiWallets: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Select your wallets")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(AppColors.onSurface(for: colorScheme))
                
                Text("Select multiple wallets/accounts you use:")
                    .font(.subheadline)
                    .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    ForEach(presets) { opt in
                        let isSelected = selectedWallets.contains(opt.name)
                        Button(action: {
                            if isSelected { selectedWallets.remove(opt.name) }
                            else { selectedWallets.insert(opt.name) }
                        }) {
                            HStack(spacing: 8) {
                                Image(systemName: opt.iconName)
                                    .foregroundColor(opt.color)
                                    .font(.subheadline)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(opt.name)
                                        .font(.caption)
                                        .fontWeight(.bold)
                                        .foregroundColor(AppColors.onSurface(for: colorScheme))
                                        .lineLimit(1)
                                    Text(opt.subtitle)
                                        .font(.system(size: 9))
                                        .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                                        .lineLimit(1)
                                }
                                Spacer()
                                if isSelected {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(AppColors.primary(for: colorScheme))
                                        .font(.caption)
                                }
                            }
                            .padding(10)
                            .background(isSelected ? AppColors.primary(for: colorScheme).opacity(0.12) : AppColors.surface(for: colorScheme))
                            .clipShape(AppShapes.medium)
                            .overlay(
                                AppShapes.medium.stroke(isSelected ? AppColors.primary(for: colorScheme) : AppColors.border(for: colorScheme), lineWidth: 1)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                
                // Add Custom Wallet
                HStack {
                    TextField("Add custom bank/wallet", text: $customWalletName)
                        .textFieldStyle(.roundedBorder)
                    Button("Add") {
                        let trimmed = customWalletName.trimmingCharacters(in: .whitespaces)
                        if !trimmed.isEmpty {
                            selectedWallets.insert(trimmed)
                            customWalletName = ""
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppColors.primary(for: colorScheme))
                }
                
                Spacer()
                
                Button(action: { currentStep = 3 }) {
                    Text("Continue (\(selectedWallets.count) selected)")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(AppColors.primary(for: colorScheme))
                        .clipShape(AppShapes.medium)
                }
            }
            .padding()
        }
    }
    
    // Step 3: Salary & Payday
    private var step3SalaryAndPayday: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Salary & Payday")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(AppColors.onSurface(for: colorScheme))
            
            VStack(alignment: .leading, spacing: 6) {
                Text("Monthly salary / income (৳)")
                    .font(.caption)
                    .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                TextField("50000", text: $monthlyIncome)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(.roundedBorder)
                    .font(.title3)
            }
            
            VStack(alignment: .leading, spacing: 8) {
                Text("Which date of the month do you get paid?")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(AppColors.onSurface(for: colorScheme))
                
                let days = [1, 2, 5, 7, 10, 15, 20, 25, 28, 30, 31]
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(days, id: \.self) { day in
                            let isSel = selectedPayDay == day
                            Text("\(day)\(ordinal(day))")
                                .font(.caption)
                                .fontWeight(isSel ? .bold : .regular)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(isSel ? AppColors.primary(for: colorScheme) : AppColors.surface(for: colorScheme))
                                .foregroundColor(isSel ? .white : AppColors.onSurface(for: colorScheme))
                                .clipShape(AppShapes.pill)
                                .overlay(AppShapes.pill.stroke(AppColors.border(for: colorScheme), lineWidth: 1))
                                .onTapGesture { selectedPayDay = day }
                        }
                    }
                }
            }
            
            Spacer()
            
            Button(action: { currentStep = 4 }) {
                Text("Continue to Budget")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.primary(for: colorScheme))
                    .clipShape(AppShapes.medium)
            }
        }
        .padding()
    }
    
    // Step 4: Budget & Mid-Cycle "Already Spent"
    private var step4BudgetAndMidCycle: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Budget & Cycle Spend")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(AppColors.onSurface(for: colorScheme))
            
            VStack(alignment: .leading, spacing: 6) {
                Text("Monthly spending limit (৳)")
                    .font(.caption)
                    .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                TextField("35000", text: $monthlyBudget)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(.roundedBorder)
                    .font(.title3)
            }
            
            // Mid-cycle check
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "clock.arrow.circlepath")
                        .foregroundColor(Color.appWarning)
                    Text("Mid-Cycle Spend Check")
                        .font(.subheadline)
                        .fontWeight(.bold)
                }
                Text("If today is not your salary date, enter how much you have already spent this cycle so your safe-to-spend allowance is accurate:")
                    .font(.caption)
                    .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
                
                TextField("Already spent (e.g. 12500)", text: $alreadySpentThisCycle)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(.roundedBorder)
            }
            .padding()
            .background(AppColors.surface(for: colorScheme))
            .clipShape(AppShapes.medium)
            .overlay(AppShapes.medium.stroke((Double(monthlyBudget) ?? 0) > 0 && (Double(alreadySpentThisCycle) ?? 0) > (Double(monthlyBudget) ?? 0) ? Color.red : Color.appWarning.opacity(0.5), lineWidth: 1))
            
            if let bVal = Double(monthlyBudget), let sVal = Double(alreadySpentThisCycle), bVal > 0, sVal > bVal {
                HStack(spacing: 12) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundColor(.white)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Budget Exceeded (Starting in Deficit)")
                            .font(.subheadline)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                        Text("Already spent exceeds limit by ৳\(Int(sVal - bVal)).")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.9))
                    }
                    Spacer()
                }
                .padding(12)
                .background(Color.red.opacity(0.85))
                .clipShape(AppShapes.medium)
            }
            
            Spacer()
            
            Button(action: { currentStep = 5 }) {
                Text("Review Snapshot")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.primary(for: colorScheme))
                    .clipShape(AppShapes.medium)
            }
        }
        .padding()
    }
    
    // Step 5: Summary
    private var step5Summary: some View {
        VStack(spacing: 20) {
            Image(systemName: "checkmark.seal.fill")
                .resizable()
                .frame(width: 60, height: 60)
                .foregroundColor(Color.appIncome)
            
            Text("Ready to Launch!")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(AppColors.onSurface(for: colorScheme))
            
            VStack(spacing: 12) {
                summaryRow(label: "Wallets", value: selectedWallets.joined(separator: ", "))
                summaryRow(label: "Monthly Income", value: "৳\(monthlyIncome)")
                summaryRow(label: "Payday", value: "\(selectedPayDay)\(ordinal(selectedPayDay)) of every month")
                summaryRow(label: "Budget Limit", value: "৳\(monthlyBudget)")
                if let bVal = Double(monthlyBudget), let sVal = Double(alreadySpentThisCycle), bVal > 0, sVal > bVal {
                    summaryRow(label: "Budget Deficit", value: "-৳\(Int(sVal - bVal)) (Exceeded)")
                }
                if !alreadySpentThisCycle.isEmpty {
                    summaryRow(label: "Already Spent", value: "-৳\(alreadySpentThisCycle)")
                }
            }
            .padding()
            .background(AppColors.surface(for: colorScheme))
            .clipShape(AppShapes.medium)
            .overlay(AppShapes.medium.stroke(AppColors.border(for: colorScheme), lineWidth: 1))
            
            Spacer()
            
            Button(action: { finishOnboarding() }) {
                Text("Open Dashboard")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(AppColors.primary(for: colorScheme))
                    .clipShape(AppShapes.medium)
            }
        }
        .padding()
    }
    
    private func summaryRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.caption)
                .foregroundColor(AppColors.onSurfaceVariant(for: colorScheme))
            Spacer()
            Text(value)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(AppColors.onSurface(for: colorScheme))
        }
    }
    
    private func finishOnboarding() {
        if isPresentedAsSheet {
            dismiss()
        } else {
            onCompleted?()
        }
    }
    
    private func ordinal(_ n: Int) -> String {
        if (11...13).contains(n) { return "th" }
        switch n % 10 {
        case 1: return "st"
        case 2: return "nd"
        case 3: return "rd"
        default: return "th"
        }
    }
}
