package com.expenseapp.salarytracker.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.presentation.theme.expenseTextColor
import com.expenseapp.salarytracker.presentation.theme.incomeTextColor

/**
 * Displays a formatted monetary amount.
 *
 * When [showSignColor] is true, positive amounts use [AppIncomeText] and
 * negative amounts use [AppExpenseText] — both AA-contrast-safe on white/light
 * backgrounds.  An explicit [color] override takes precedence over sign colouring.
 */
@Composable
fun MoneyDisplay(
    amount: MinorUnits,
    modifier: Modifier = Modifier,
    currencySymbol: String = "\u09F3",
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color? = null,
    showSignColor: Boolean = false
) {
    val formatted = amount.format(currencySymbol)
    val displayColor = color ?: if (showSignColor) {
        when {
            amount.isPositive() -> incomeTextColor()
            amount.isNegative() -> expenseTextColor()
            else                -> MaterialTheme.colorScheme.onSurface
        }
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Text(
        text       = formatted,
        style      = textStyle,
        fontWeight = fontWeight,
        color      = displayColor,
        modifier   = modifier.semantics {
            contentDescription = "$formatted ${if (amount.isNegative()) "negative" else ""}"
        }
    )
}
