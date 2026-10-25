package com.expenseapp.salarytracker.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =============================================================================
//  DESIGN SYSTEM — Shape Tokens (Phase 3D-VISUAL RESET)
//  Disciplined, moderate corner radii. Avoids oversized 28dp pill cards.
// =============================================================================

val AppShapes = Shapes(
    /** Compact elements: small chips, icon badges, mini buttons. */
    small      = RoundedCornerShape(6.dp),
    /** Standard cards, surface containers, dialogs. */
    medium     = RoundedCornerShape(10.dp),
    /** Elevated modal sheets, hero surface blocks. */
    large      = RoundedCornerShape(14.dp),
    /** Filter pills, FAB, action buttons. */
    extraLarge = RoundedCornerShape(18.dp)
)
