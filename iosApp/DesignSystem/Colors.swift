import SwiftUI

// =============================================================================
//  EXPENSE & SALARY TRACKER — iOS Design System: Color Tokens
//  Exact parity with the approved executive visual language:
//  Warm ivory & deep forest foundation, calm semantic financial indicators,
//  and full dynamic support for Apple iOS Light & Dark Mode.
// =============================================================================

extension Color {
    // -- Primary & Brand Tones ------------------------------------------------
    static let appPrimary = Color(red: 27/255, green: 56/255, blue: 43/255)
    static let appPrimaryContainer = Color(red: 133/255, green: 195/255, blue: 166/255)
    
    // -- Surfaces & Backgrounds -----------------------------------------------
    static let appBackground = Color(red: 251/255, green: 249/255, blue: 245/255)
    static let appSurface = Color.white
    static let appSurfaceVariant = Color(red: 241/255, green: 237/255, blue: 230/255)
    static let appBorder = Color(red: 232/255, green: 228/255, blue: 220/255)
    
    // -- Text & Contrast ------------------------------------------------------
    static let appOnSurface = Color(red: 24/255, green: 27/255, blue: 25/255)
    static let appOnSurfaceVariant = Color(red: 101/255, green: 110/255, blue: 104/255)
    static let appOnPrimary = Color.white

    // -- Financial Semantics (Restrained & Trustworthy) -----------------------
    static let appIncome = Color(red: 46/255, green: 125/255, blue: 91/255)       // Calm Forest Green (#2E7D5B)
    static let appExpense = Color(red: 192/255, green: 57/255, blue: 43/255)      // Warm Rust (#C0392B)
    static let appTransfer = Color(red: 71/255, green: 101/255, blue: 130/255)    // Slate Blue (#476582)
    static let appSaving = Color(red: 59/255, green: 105/255, blue: 120/255)      // Ocean Slate (#3B6978)
    static let appDonation = Color(red: 158/255, green: 71/255, blue: 112/255)   // Quiet Plum (#9E4770)
    static let appWarning = Color(red: 180/255, green: 114/255, blue: 20/255)     // Warm Amber (#B47214)
}

struct AppColors {
    // Static design tokens
    static let brandPrimary = Color(red: 27/255, green: 56/255, blue: 43/255)
    static let background = Color(red: 251/255, green: 249/255, blue: 245/255)
    static let surface = Color.white
    static let surfaceSecondary = Color(red: 241/255, green: 237/255, blue: 230/255)
    static let surfaceVariant = Color(red: 241/255, green: 237/255, blue: 230/255)
    static let cardBorder = Color(red: 232/255, green: 228/255, blue: 220/255)
    static let textPrimary = Color(red: 24/255, green: 27/255, blue: 25/255)
    static let textSecondary = Color(red: 101/255, green: 110/255, blue: 104/255)
    static let incomeGreen = Color(red: 46/255, green: 125/255, blue: 91/255)
    static let expenseRed = Color(red: 192/255, green: 57/255, blue: 43/255)
    static let warningYellow = Color(red: 180/255, green: 114/255, blue: 20/255)
    static let warningAmber = Color(red: 180/255, green: 114/255, blue: 20/255)
    static let transferBlue = Color(red: 71/255, green: 101/255, blue: 130/255)
    static let savingGreen = Color(red: 59/255, green: 105/255, blue: 120/255)

    // Dynamic Resolvers that provide adaptive contrast based on ColorScheme
    static func background(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(red: 18/255, green: 21/255, blue: 20/255) : Color(red: 251/255, green: 249/255, blue: 245/255)
    }
    
    static func surface(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(red: 26/255, green: 30/255, blue: 28/255) : Color.white
    }
    
    static func surfaceVariant(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(red: 35/255, green: 41/255, blue: 38/255) : Color(red: 241/255, green: 237/255, blue: 230/255)
    }
    
    static func onSurface(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(red: 240/255, green: 242/255, blue: 241/255) : Color(red: 24/255, green: 27/255, blue: 25/255)
    }
    
    static func onSurfaceVariant(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(red: 160/255, green: 168/255, blue: 163/255) : Color(red: 101/255, green: 110/255, blue: 104/255)
    }

    static func primary(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(red: 133/255, green: 195/255, blue: 166/255) : Color(red: 27/255, green: 56/255, blue: 43/255)
    }
    
    static func border(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color.white.opacity(0.10) : Color(red: 232/255, green: 228/255, blue: 220/255)
    }
}
