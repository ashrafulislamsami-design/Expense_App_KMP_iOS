import SwiftUI

/**
 * Type-safe currency formatter for integer minor units (poisha / cents).
 * Invariant: 0 Float/Double currency conversions.
 */
struct MoneyText: View {
    let minorUnits: Int64
    var currencySymbol: String
    var decimals: Int
    var isCompact: Bool
    var showSign: Bool
    var style: Font
    var weight: Font.Weight
    var color: Color?
    
    init(
        minorUnits: Int64,
        currencySymbol: String = "৳",
        decimals: Int = 2,
        isCompact: Bool = false,
        showSign: Bool = false,
        style: Font = .body,
        font: Font? = nil,
        weight: Font.Weight = .regular,
        color: Color? = nil
    ) {
        self.minorUnits = minorUnits
        self.currencySymbol = currencySymbol
        self.decimals = decimals
        self.isCompact = isCompact
        self.showSign = showSign
        self.style = font ?? style
        self.weight = weight
        self.color = color
    }
    
    init(
        minorUnits: Int,
        currencySymbol: String = "৳",
        decimals: Int = 2,
        isCompact: Bool = false,
        showSign: Bool = false,
        style: Font = .body,
        font: Font? = nil,
        weight: Font.Weight = .regular,
        color: Color? = nil
    ) {
        self.init(
            minorUnits: Int64(minorUnits),
            currencySymbol: currencySymbol,
            decimals: decimals,
            isCompact: isCompact,
            showSign: showSign,
            style: style,
            font: font,
            weight: weight,
            color: color
        )
    }
    
    var body: some View {
        Text(formattedString)
            .font(style)
            .fontWeight(weight)
            .foregroundColor(color)
    }
    
    private var formattedString: String {
        let isNegative = minorUnits < 0
        let absVal = isNegative ? -minorUnits : minorUnits
        let divisor: Int64 = (decimals == 0) ? 1 : (decimals == 1 ? 10 : 100)
        let whole = absVal / divisor
        let fraction = absVal % divisor
        
        let numberFormatter = NumberFormatter()
        numberFormatter.numberStyle = .decimal
        numberFormatter.groupingSeparator = ","
        let wholeStr = numberFormatter.string(from: NSNumber(value: whole)) ?? "\(whole)"
        
        var fractionStr = ""
        if decimals > 0 && !(isCompact && fraction == 0) {
            fractionStr = String(format: ".%0*lld", decimals, fraction)
        }
        
        let prefix: String
        if isNegative {
            prefix = "-"
        } else if showSign && minorUnits > 0 {
            prefix = "+"
        } else {
            prefix = ""
        }
        
        return "\(prefix)\(currencySymbol)\(wholeStr)\(fractionStr)"
    }
}
