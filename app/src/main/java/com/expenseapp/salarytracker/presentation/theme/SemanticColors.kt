package com.expenseapp.salarytracker.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// =============================================================================
//  DESIGN SYSTEM — Semantic Colour Resolvers  (Phase 3D-A)
//
//  These @Composable functions return the correct semantic text colour for
//  the current light/dark mode.  Use them instead of referencing AppXxxText
//  or DarkXxxText directly.
//
//  Usage:
//      color = incomeTextColor()      // income amount labels
//      color = expenseTextColor()     // expense amount labels
//      color = warningTextColor()     // warning labels / badges
//      color = savingTextColor()      // saving labels
// =============================================================================

/** Returns the AA-safe income text colour for the current theme. */
@Composable
fun incomeTextColor(): Color =
    if (isSystemInDarkTheme()) DarkIncomeText else AppIncomeText

/** Returns the AA-safe expense text colour for the current theme. */
@Composable
fun expenseTextColor(): Color =
    if (isSystemInDarkTheme()) DarkExpenseText else AppExpenseText

/** Returns the AA-safe saving text colour for the current theme. */
@Composable
fun savingTextColor(): Color =
    if (isSystemInDarkTheme()) DarkSavingText else AppSavingText

/** Returns the AA-safe warning text colour for the current theme. */
@Composable
fun warningTextColor(): Color =
    if (isSystemInDarkTheme()) DarkWarningText else AppWarningText
