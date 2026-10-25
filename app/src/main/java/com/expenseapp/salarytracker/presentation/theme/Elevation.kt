package com.expenseapp.salarytracker.presentation.theme

import androidx.compose.ui.unit.dp

// =============================================================================
//  DESIGN SYSTEM — Elevation Tokens  (Phase 3D-A)
//  Prefer tonal surface differences and subtle borders over heavy shadows.
//  Use elevation sparingly — the UI should feel grounded, not floating.
// =============================================================================

object AppElevation {
    /** Flat — use a border or tonal surface instead. */
    val none   = 0.dp

    /** Subtle lift — most cards sit at this level. */
    val low    = 1.dp

    /** Moderate lift — dialogs, bottom sheets. */
    val medium = 4.dp

    /** Maximum — FAB, modal surfaces. */
    val high   = 8.dp
}
