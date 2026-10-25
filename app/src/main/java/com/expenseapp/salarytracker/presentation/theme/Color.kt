package com.expenseapp.salarytracker.presentation.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
//  DESIGN SYSTEM — Color Tokens  (Phase 3D-VISUAL RESET)
//  Executive, calm, trustworthy visual language.
//  Warm-neutral foundation, deep forest green primary, muted slate/teal secondary.
//  Zero neon, zero harsh saturated cards, zero artificial glow.
// =============================================================================

// -- Light Palette Primitives (Warm Ivory & Deep Forest) ----------------------

/** Root background: warm ivory / stone canvas with breathing room. */
val AppBackground          = Color(0xFFFBF9F5)

/** Card / container surface: pure soft white. */
val AppSurface             = Color(0xFFFFFFFF)

/** Elevated surface tint (search bars, chip backgrounds, tonal fills). */
val AppSurfaceVariant      = Color(0xFFF1EDE6)

/** Deep forest green — primary brand, active nav, primary buttons. */
val AppPrimary             = Color(0xFF1B382B)

/** Restrained soft sage tint — selected chip / subtle container background. */
val AppPrimaryContainer    = Color(0xFFE4EDE7)

/** Muted slate teal — secondary actions & metadata indicators. */
val AppSecondary           = Color(0xFF425C51)

/** Light slate tint — secondary container background. */
val AppSecondaryContainer  = Color(0xFFEBF0ED)

/** Deep warm charcoal — primary high-contrast text (17:1 on AppSurface). */
val AppOnSurface           = Color(0xFF181B19)

/** Muted neutral slate gray — secondary / meta / footnote text (6.2:1 AA). */
val AppOnSurfaceVariant    = Color(0xFF656E68)

/** White — text/icon on primary-coloured surfaces. */
val AppOnPrimary           = Color(0xFFFFFFFF)

/** Extremely subtle warm gray border (separates surfaces quietly). */
val AppBorder              = Color(0xFFE8E4DC)

// -- Semantic / Financial Colours (Restrained & Calm) -------------------------

/** Income semantic fill / icon (calm forest green). */
val AppIncome              = Color(0xFF2E7D5B)

/** AA-accessible income text on light background. */
val AppIncomeText          = Color(0xFF1E6145)

/** Expense semantic fill / icon (warm terracotta / rust). */
val AppExpense             = Color(0xFFC0392B)

/** AA-accessible expense text on light background. */
val AppExpenseText         = Color(0xFF962D22)

/** Transfer semantic — calm slate blue. */
val AppTransfer            = Color(0xFF476582)

/** Refund semantic — subtle muted teal. */
val AppRefund              = Color(0xFF2B7A78)

/** Saving semantic — calm ocean slate. */
val AppSaving              = Color(0xFF3B6978)

/** AA-accessible saving text. */
val AppSavingText          = Color(0xFF204E5C)

/** Donation semantic — quiet rose / plum. */
val AppDonation            = Color(0xFF9E4770)

/** Debt semantic — muted amber-rust. */
val AppDebt                = Color(0xFFA8422B)

/** Warning semantic — warm ochre / amber. */
val AppWarning             = Color(0xFFB47214)

/** AA-accessible warning text. */
val AppWarningText         = Color(0xFF7D4E0B)

/** Error semantic — destructive actions, form errors. */
val AppError               = Color(0xFFB3261E)

// -- Dark Palette Primitives (Deep Charcoal Slate & Soft Jade) ----------------

/** Deep charcoal / slate foundation — luxurious and quiet, not pitch black. */
val DarkBackground         = Color(0xFF121514)

/** Soft elevated dark surface. */
val DarkSurface            = Color(0xFF1A1E1C)

/** Elevated dark surface variant. */
val DarkSurfaceVariant     = Color(0xFF232926)

/** Soft jade/sage primary for legibility on dark surfaces. */
val DarkPrimary            = Color(0xFF85C3A6)

/** Dark forest primary container. */
val DarkPrimaryContainer   = Color(0xFF1E382C)

/** Dark text on primary button/badge. */
val DarkOnPrimary          = Color(0xFF0B1F16)

/** Soft warm white text for dark mode. */
val DarkOnSurface          = Color(0xFFE8EBE9)

/** Muted slate gray for secondary meta text in dark mode. */
val DarkOnSurfaceVariant   = Color(0xFF95A09A)

/** Subtle dark border. */
val DarkBorder             = Color(0xFF2B332F)

/** Soft slate teal secondary. */
val DarkSecondary          = Color(0xFF8EABA0)

/** Secondary dark container. */
val DarkSecondaryContainer = Color(0xFF22322B)

// Dark-mode semantic text variants (all >= 4.5:1 on DarkSurface)

/** Income text on dark surfaces (soft sage). */
val DarkIncomeText         = Color(0xFF8FD4B5)

/** Expense text on dark surfaces (soft terracotta rose). */
val DarkExpenseText        = Color(0xFFE5989B)

/** Saving text on dark surfaces (soft sky slate). */
val DarkSavingText         = Color(0xFF90C2DE)

/** Warning text on dark surfaces (soft warm ochre). */
val DarkWarningText        = Color(0xFFE3B873)

// -- Category Icon Tints (Subtle & Organic) -----------------------------------

/** Warm amber-orange — Food category icon. */
val CategoryFoodIcon       = Color(0xFFD97706)

/** Muted slate blue — Transport category icon. */
val CategoryTransportIcon  = Color(0xFF3B82F6)

/** Olive sage green — Grocery/Home category icon. */
val CategoryGroceryIcon    = Color(0xFF059669)

/** Neutral slate — Default/general icon. */
val CategoryDefaultIcon    = Color(0xFF64748B)
