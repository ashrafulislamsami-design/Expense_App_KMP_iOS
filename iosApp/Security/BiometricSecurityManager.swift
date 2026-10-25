import Foundation
import LocalAuthentication

class BiometricSecurityManager: ObservableObject {
    static let shared = BiometricSecurityManager()
    
    @Published var isLocked: Bool = false
    @Published var isBiometricsAvailable: Bool = false
    @Published var isBiometricsEnabled: Bool = false
    @Published var biometricType: LABiometryType = .none
    @Published var authErrorMessage: String? = nil
    
    private let biometricsEnabledKey = "isBiometricsEnabledKey"
    
    private init() {
        self.isBiometricsEnabled = UserDefaults.standard.bool(forKey: biometricsEnabledKey)
        checkBiometricAvailability()
        if isBiometricsEnabled {
            self.isLocked = true
        }
    }
    
    func checkBiometricAvailability() {
        let context = LAContext()
        var error: NSError?
        
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            isBiometricsAvailable = true
            biometricType = context.biometryType
        } else {
            isBiometricsAvailable = false
            biometricType = .none
        }
    }
    
    func setBiometricsEnabled(_ enabled: Bool) {
        isBiometricsEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: biometricsEnabledKey)
        if !enabled {
            isLocked = false
            authErrorMessage = nil
        }
    }
    
    func lock() {
        if isBiometricsEnabled {
            isLocked = true
            authErrorMessage = nil
        }
    }
    
    func authenticate() {
        authenticate { [weak self] success, errorMessage in
            DispatchQueue.main.async {
                if success {
                    self?.isLocked = false
                    self?.authErrorMessage = nil
                } else {
                    self?.authErrorMessage = errorMessage ?? "Authentication failed"
                }
            }
        }
    }
    
    func authenticate(completion: @escaping (Bool, String?) -> Void) {
        let context = LAContext()
        context.localizedCancelTitle = "Use Passcode"
        
        let reason = "Unlock your Expense & Salary Tracker records"
        context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason) { success, error in
            DispatchQueue.main.async {
                if success {
                    self.isLocked = false
                    self.authErrorMessage = nil
                    completion(true, nil)
                } else {
                    let msg = error?.localizedDescription
                    self.authErrorMessage = msg
                    completion(false, msg)
                }
            }
        }
    }
}
