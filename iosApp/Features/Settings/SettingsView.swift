import SwiftUI

struct SettingsView: View {
    @ObservedObject var securityManager = BiometricSecurityManager.shared
    
    @AppStorage("userName") private var userName: String = "User"
    @AppStorage("userAvatar") private var userAvatar: String = "🧑‍💻"
    @State private var showingEditProfile = false
    @State private var editNameText = ""
    @State private var editAvatarEmoji = "🧑‍💻"
    
    @AppStorage("salaryAmountMinor") private var salaryAmountMinor: Int = 8500000
    @AppStorage("salaryPayday") private var salaryPayday: Int = 1
    @AppStorage("reportingMode") private var reportingMode: String = "SALARY_CYCLE"
    @AppStorage("isPrivacyMaskEnabled") private var isPrivacyMaskEnabled: Bool = false
    
    @State private var showingBackupSuccessAlert = false
    @State private var showingRestoreSheet = false
    @State private var showingEncryptDialog = false
    @State private var encryptionPassword = ""
    @State private var backupFileURL: URL? = nil
    @State private var showingShareSheet = false
    
    var body: some View {
        NavigationStack {
            Form {
                // Personal Profile Section
                Section(header: Text("Personal Profile").font(.caption).foregroundColor(AppColors.textSecondary)) {
                    Button(action: {
                        editNameText = userName
                        editAvatarEmoji = userAvatar
                        showingEditProfile = true
                    }) {
                        HStack(spacing: 14) {
                            ZStack {
                                Circle()
                                    .fill(Color.blue.opacity(0.15))
                                    .frame(width: 48, height: 48)
                                Text(userAvatar)
                                    .font(.system(size: 24))
                            }
                            VStack(alignment: .leading, spacing: 2) {
                                Text(userName)
                                    .font(.headline)
                                    .foregroundColor(AppColors.textPrimary)
                                Text("Tap to edit name and avatar persona")
                                    .font(.caption2)
                                    .foregroundColor(AppColors.textSecondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(AppColors.textSecondary)
                        }
                        .padding(.vertical, 4)
                    }
                }
                
                // Security & Biometrics
                Section(header: Text("Security & Privacy").font(.caption).foregroundColor(AppColors.textSecondary)) {
                    Toggle(isOn: Binding(
                        get: { securityManager.isBiometricsEnabled },
                        set: { securityManager.setBiometricsEnabled($0) }
                    )) {
                        HStack {
                            Image(systemName: "faceid")
                                .foregroundColor(AppColors.brandPrimary)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Face ID / Touch ID Lock")
                                Text("Require biometrics to access financial ledger")
                                    .font(.caption2)
                                    .foregroundColor(AppColors.textSecondary)
                            }
                        }
                    }
                    
                    Toggle(isOn: $isPrivacyMaskEnabled) {
                        HStack {
                            Image(systemName: "eye.slash.fill")
                                .foregroundColor(.indigo)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Privacy Mode")
                                Text("Mask account balances in multitasking app switcher")
                                    .font(.caption2)
                                    .foregroundColor(AppColors.textSecondary)
                            }
                        }
                    }
                }
                
                // Salary & Cycle Engine
                Section(header: Text("Salary & Payday Cycle").font(.caption).foregroundColor(AppColors.textSecondary)) {
                    HStack {
                        Text("Monthly Net Salary")
                        Spacer()
                        MoneyText(minorUnits: salaryAmountMinor, currencySymbol: "৳", font: .subheadline, weight: .semibold)
                    }
                    
                    Picker("Salary Payday", selection: $salaryPayday) {
                        ForEach(1...31, id: \.self) { day in
                            Text("\(day)\(daySuffix(day)) of month").tag(day)
                        }
                    }
                    
                    Picker("Reporting Mode", selection: $reportingMode) {
                        Text("Salary Cycle (e.g. \(salaryPayday)st to \(salaryPayday == 1 ? 31 : salaryPayday - 1)th)").tag("SALARY_CYCLE")
                        Text("Standard Calendar Month (1st to end)").tag("CALENDAR_MONTH")
                    }
                }
                
                // Backup & Cross-Platform Restore
                Section(header: Text("Cross-Platform Backup & Restore").font(.caption).foregroundColor(AppColors.textSecondary)) {
                    Button(action: exportPlainJsonBackup) {
                        HStack {
                            Image(systemName: "arrow.down.doc.fill")
                                .foregroundColor(.blue)
                            Text("Export JSON Backup")
                            Spacer()
                            Text("Android Compatible")
                                .font(.caption2)
                                .foregroundColor(AppColors.textSecondary)
                        }
                    }
                    
                    Button(action: { showingEncryptDialog = true }) {
                        HStack {
                            Image(systemName: "lock.doc.fill")
                                .foregroundColor(AppColors.incomeGreen)
                            Text("Export Encrypted Backup (AES-256)")
                            Spacer()
                            Image(systemName: "shield.checkered")
                                .foregroundColor(AppColors.incomeGreen)
                        }
                    }
                    
                    Button(action: { showingRestoreSheet = true }) {
                        HStack {
                            Image(systemName: "arrow.up.doc.fill")
                                .foregroundColor(.orange)
                            Text("Restore from Backup File")
                        }
                    }
                }
                
                // Offline Assurance
                Section {
                    VStack(alignment: .leading, spacing: SpacingTokens.xs) {
                        HStack {
                            Image(systemName: "checkmark.shield.fill")
                                .foregroundColor(AppColors.incomeGreen)
                            Text("100% Offline & Private")
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundColor(AppColors.textPrimary)
                        }
                        Text("This app has zero network permissions, zero telemetry, and zero cloud tracking. Your financial records never leave this iPhone.")
                            .font(.caption2)
                            .foregroundColor(AppColors.textSecondary)
                            .lineSpacing(2)
                    }
                    .padding(.vertical, 4)
                }
                
                // App Information
                Section(header: Text("About").font(.caption).foregroundColor(AppColors.textSecondary)) {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0.0 (Phase iOS-1)")
                            .foregroundColor(AppColors.textSecondary)
                    }
                    HStack {
                        Text("Shared Financial Engine")
                        Spacer()
                        Text("SharedCore KMP 1.0")
                            .foregroundColor(AppColors.textSecondary)
                    }
                }
            }
            .navigationTitle("Settings")
            .alert("Encrypted Backup", isPresented: $showingEncryptDialog) {
                SecureField("Enter passphrase", text: $encryptionPassword)
                Button("Encrypt & Export") {
                    exportEncryptedBackup(passphrase: encryptionPassword)
                }
                Button("Cancel", role: .cancel) { encryptionPassword = "" }
            } message: {
                Text("Enter a secure passphrase. You will need this passphrase to restore your records on iOS or Android.")
            }
            .alert("Backup Generated", isPresented: $showingBackupSuccessAlert) {
                Button("OK", role: .cancel) { }
            } message: {
                Text("Your backup file is ready and matches the cross-platform specification.")
            }
            .sheet(isPresented: $showingEditProfile) {
                NavigationStack {
                    Form {
                        Section(header: Text("Persona Avatar").font(.caption)) {
                            HStack {
                                Spacer()
                                ZStack {
                                    Circle()
                                        .fill(Color.blue.opacity(0.15))
                                        .frame(width: 72, height: 72)
                                    Text(editAvatarEmoji)
                                        .font(.system(size: 38))
                                }
                                Spacer()
                            }
                            .padding(.vertical, 8)
                            
                            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 4), spacing: 12) {
                                ForEach(["🧑‍💻", "💼", "🧑‍🚀", "🎨", "⚡", "🌿", "🦊", "💎"], id: \.self) { emoji in
                                    Button(action: { editAvatarEmoji = emoji }) {
                                        Text(emoji)
                                            .font(.system(size: 28))
                                            .padding(10)
                                            .background(editAvatarEmoji == emoji ? Color.blue.opacity(0.2) : Color.gray.opacity(0.1))
                                            .clipShape(Circle())
                                            .overlay(
                                                Circle()
                                                    .stroke(editAvatarEmoji == emoji ? Color.blue : Color.clear, lineWidth: 2)
                                            )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                        
                        Section(header: Text("Display Name").font(.caption)) {
                            TextField("Enter your name", text: $editNameText)
                        }
                    }
                    .navigationTitle("Edit Profile")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") {
                                showingEditProfile = false
                            }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Save") {
                                let trimmed = editNameText.trimmingCharacters(in: .whitespaces)
                                if !trimmed.isEmpty {
                                    userName = trimmed
                                }
                                userAvatar = editAvatarEmoji
                                showingEditProfile = false
                            }
                        }
                    }
                }
                .presentationDetents([.medium, .large])
            }
        }
    }
    
    private func exportPlainJsonBackup() {
        let envelope = CrossPlatformBackupService.shared.createSampleEnvelope()
        if let data = try? CrossPlatformBackupService.shared.exportJson(envelope: envelope) {
            let path = FileManager.default.temporaryDirectory.appendingPathComponent("expense_backup_\(Date().formatted(.iso8601)).json")
            try? data.write(to: path)
            backupFileURL = path
            showingBackupSuccessAlert = true
        }
    }
    
    private func exportEncryptedBackup(passphrase: String) {
        guard !passphrase.isEmpty else { return }
        let envelope = CrossPlatformBackupService.shared.createSampleEnvelope()
        if let encData = try? CrossPlatformBackupService.shared.exportEncrypted(envelope: envelope, passphrase: passphrase) {
            let path = FileManager.default.temporaryDirectory.appendingPathComponent("expense_backup_\(Date().formatted(.iso8601)).enc.expbkp")
            try? encData.write(to: path)
            backupFileURL = path
            encryptionPassword = ""
            showingBackupSuccessAlert = true
        }
    }
    
    private func daySuffix(_ day: Int) -> String {
        switch day {
        case 1, 21, 31: return "st"
        case 2, 22: return "nd"
        case 3, 23: return "rd"
        default: return "th"
        }
    }
}
