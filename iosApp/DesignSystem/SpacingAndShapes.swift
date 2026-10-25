import SwiftUI

// =============================================================================
//  EXPENSE & SALARY TRACKER — iOS Design System: Spacing & Shapes Tokens
// =============================================================================

struct AppSpacing {
    static let xxs: CGFloat = 2
    static let xs: CGFloat = 4
    static let sm: CGFloat = 8
    static let md: CGFloat = 12
    static let lg: CGFloat = 16
    static let xl: CGFloat = 24
    static let xxl: CGFloat = 32
}

struct AppShapes {
    static let small = RoundedRectangle(cornerRadius: 8, style: .continuous)
    static let medium = RoundedRectangle(cornerRadius: 12, style: .continuous)
    static let large = RoundedRectangle(cornerRadius: 16, style: .continuous)
    static let extraLarge = RoundedRectangle(cornerRadius: 24, style: .continuous)
    static let pill = Capsule(style: .continuous)
}

struct SpacingTokens {
    static let xxs: CGFloat = 2
    static let xs: CGFloat = 4
    static let sm: CGFloat = 8
    static let md: CGFloat = 12
    static let lg: CGFloat = 16
    static let xl: CGFloat = 24
    static let xxl: CGFloat = 32
    static let screenPadding: CGFloat = 16
    static let cardPadding: CGFloat = 16
}

struct ShapeTokens {
    static let buttonRadius: CGFloat = 12
    static let cardRadius: CGFloat = 16
    static let small: CGFloat = 8
    static let medium: CGFloat = 12
    static let large: CGFloat = 16
}
