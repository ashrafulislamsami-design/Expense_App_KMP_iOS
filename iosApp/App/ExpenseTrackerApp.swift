import SwiftUI

@main
struct ExpenseTrackerApp: App {
    @StateObject private var securityManager = BiometricSecurityManager.shared
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding: Bool = false
    @Environment(\.scenePhase) private var scenePhase
    
    @State private var showingPrivacyShield: Bool = false
    
    var body: some Scene {
        WindowGroup {
            ZStack {
                if !hasCompletedOnboarding {
                    OnboardingFlowView(onCompleted: {
                        hasCompletedOnboarding = true
                    })
                } else {
                    AppNavigation()
                }
                
                // Biometric Lock Screen Overlay
                if securityManager.isLocked && securityManager.isBiometricsEnabled {
                    BiometricLockOverlayView(securityManager: securityManager)
                        .transition(.opacity)
                }
                
                // Multitasking Privacy Shield
                if showingPrivacyShield {
                    PrivacyShieldOverlayView()
                        .transition(.opacity)
                }
            }
            .onChange(of: scenePhase) { newPhase in
                switch newPhase {
                case .active:
                    withAnimation(.easeOut(duration: 0.2)) {
                        showingPrivacyShield = false
                    }
                    if securityManager.isBiometricsEnabled && securityManager.isLocked {
                        securityManager.authenticate()
                    }
                case .inactive, .background:
                    withAnimation(.easeIn(duration: 0.15)) {
                        showingPrivacyShield = true
                    }
                    if securityManager.isBiometricsEnabled {
                        securityManager.lock()
                    }
                @unknown default:
                    break
                }
            }
        }
    }
}

struct BiometricLockOverlayView: View {
    @ObservedObject var securityManager: BiometricSecurityManager
    
    var body: some View {
        ZStack {
            AppColors.background
                .ignoresSafeArea()
            
            VStack(spacing: SpacingTokens.lg) {
                Spacer()
                
                ZStack {
                    Circle()
                        .fill(AppColors.brandPrimary.opacity(0.12))
                        .frame(width: 96, height: 96)
                    Image(systemName: "lock.shield.fill")
                        .font(.system(size: 48))
                        .foregroundColor(AppColors.brandPrimary)
                }
                
                VStack(spacing: SpacingTokens.xs) {
                    Text("Expense & Salary Tracker")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(AppColors.textPrimary)
                    
                    Text("Financial Ledger Locked")
                        .font(.subheadline)
                        .foregroundColor(AppColors.textSecondary)
                }
                
                if let error = securityManager.authErrorMessage {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(AppColors.expenseRed)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
                
                Spacer()
                
                Button(action: {
                    securityManager.authenticate()
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: "faceid")
                            .font(.system(size: 20))
                        Text("Unlock with Face ID")
                            .font(.headline)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(AppColors.brandPrimary)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: ShapeTokens.buttonRadius, style: .continuous))
                }
                .padding(.horizontal, SpacingTokens.screenPadding)
                .padding(.bottom, SpacingTokens.xl)
            }
        }
    }
}

struct PrivacyShieldOverlayView: View {
    var body: some View {
        ZStack {
            AppColors.background
                .ignoresSafeArea()
            
            VStack(spacing: 12) {
                Image(systemName: "shield.fill")
                    .font(.system(size: 40))
                    .foregroundColor(AppColors.brandPrimary)
                Text("Expense & Salary Tracker")
                    .font(.headline)
                    .foregroundColor(AppColors.textPrimary)
            }
        }
    }
}
