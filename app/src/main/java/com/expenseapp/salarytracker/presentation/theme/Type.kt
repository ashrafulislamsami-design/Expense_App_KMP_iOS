package com.expenseapp.salarytracker.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// =============================================================================
//  DESIGN SYSTEM — Typography (Phase 3D-VISUAL RESET)
//  Refined, calm, contemporary typography hierarchy.
//  Uses system SansSerif.
//  Sentence casing throughout.
// =============================================================================

val AppTypography = Typography(

    // ── Display — hero financial balances ────────────────────────────────────
    displayLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 36.sp,
        lineHeight    = 44.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 30.sp,
        lineHeight    = 38.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 26.sp,
        lineHeight    = 34.sp,
        letterSpacing = 0.sp
    ),

    // ── Headline — screen & key section titles ───────────────────────────────
    headlineLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Bold,
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 20.sp,
        lineHeight    = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 17.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp
    ),

    // ── Title — card headings and group labels ───────────────────────────────
    titleLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 18.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Medium,
        fontSize      = 15.sp,
        lineHeight    = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Medium,
        fontSize      = 13.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.sp
    ),

    // ── Body — reading text and metadata ─────────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Normal,
        fontSize      = 15.sp,
        lineHeight    = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Normal,
        fontSize      = 13.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.sp
    ),

    // ── Label — buttons, chips, indicators ───────────────────────────────────
    labelLarge = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Medium,
        fontSize      = 13.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = FontFamily.SansSerif,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 14.sp,
        letterSpacing = 0.1.sp
    )
)
