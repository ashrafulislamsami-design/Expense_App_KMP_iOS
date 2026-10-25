package com.expenseapp.salarytracker.presentation.theme

import androidx.compose.ui.unit.dp

// =============================================================================
//  DESIGN SYSTEM — Spacing Tokens  (Phase 3D-A)
//  Use only these values for padding, margin, gap, and spacer sizes.
//  Never use bare .dp literals with arbitrary values in composables.
// =============================================================================

object AppSpacing {
    /** 4dp — tight: icon-to-label, badge padding. */
    val xs    = 4.dp

    /** 8dp — close: chip padding, row gap between related items. */
    val sm    = 8.dp

    /** 12dp — moderate: inner card padding, list item vertical gap. */
    val md    = 12.dp

    /** 16dp — standard: screen horizontal margin, card inner padding. */
    val lg    = 16.dp

    /** 24dp — section: vertical gap between major screen sections. */
    val xl    = 24.dp

    /** 32dp — wide: empty-state padding, generous content separation. */
    val xxl   = 32.dp

    /** 40dp — extra: hero card internal top padding. */
    val xxxl  = 40.dp

    /** 48dp — bottom nav clearance + FAB safe padding at list end. */
    val navClearance = 88.dp
}
