package com.expenseapp.salarytracker.presentation.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =============================================================================
//  DESIGN SYSTEM — Material 3 Theme  (Phase 3D-A)
//  Single entry point for all Compose UI. References Color, Shape, and
//  Typography tokens defined in the same package.
//  Light mode is the primary design; dark is supported via DarkColorScheme.
// =============================================================================

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity      -> this
    is ContextWrapper -> baseContext.findActivity()
    else             -> null
}

// -- Light colour scheme (primary design) -------------------------------------

private val LightColorScheme = lightColorScheme(
    primary                = AppPrimary,
    onPrimary              = AppOnPrimary,
    primaryContainer       = AppPrimaryContainer,
    onPrimaryContainer     = AppPrimary,          // dark text on light container

    secondary              = AppSecondary,
    onSecondary            = AppOnPrimary,
    secondaryContainer     = AppSecondaryContainer,
    onSecondaryContainer   = AppSecondary,

    background             = AppBackground,
    onBackground           = AppOnSurface,

    surface                = AppSurface,
    onSurface              = AppOnSurface,
    surfaceVariant         = AppSurfaceVariant,
    onSurfaceVariant       = AppOnSurfaceVariant,

    outline                = AppBorder,
    outlineVariant         = AppBorder,

    error                  = AppError,
    onError                = Color.White
)

// -- Dark colour scheme -------------------------------------------------------

private val DarkColorScheme = darkColorScheme(
    primary                = DarkPrimary,
    onPrimary              = DarkOnPrimary,
    primaryContainer       = DarkPrimaryContainer,
    onPrimaryContainer     = DarkPrimary,

    secondary              = DarkSecondary,
    onSecondary            = DarkBackground,
    secondaryContainer     = DarkSecondaryContainer,
    onSecondaryContainer   = DarkSecondary,

    background             = DarkBackground,
    onBackground           = DarkOnSurface,

    surface                = DarkSurface,
    onSurface              = DarkOnSurface,
    surfaceVariant         = DarkSurfaceVariant,
    onSurfaceVariant       = DarkOnSurfaceVariant,

    outline                = DarkBorder,
    outlineVariant         = DarkBorder,

    error                  = AppError,
    onError                = Color.White
)

// -- Theme composable ---------------------------------------------------------

@Composable
fun ExpenseAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                runCatching {
                    val window = activity.window
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars    = !darkTheme
                    insetsController.isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content
    )
}
