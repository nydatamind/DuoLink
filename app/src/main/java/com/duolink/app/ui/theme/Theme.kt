package com.duolink.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DuoLinkColorScheme = darkColorScheme(
    primary = DuoLinkRedPrimary,
    onPrimary = DuoLinkTextWhite,
    primaryContainer = DuoLinkRedDark,
    secondary = DuoLinkRedPrimary,
    onSecondary = DuoLinkTextWhite,
    background = DuoLinkCharcoalBg,
    surface = DuoLinkSurface,
    surfaceVariant = DuoLinkPillInput,
    onBackground = DuoLinkTextWhite,
    onSurface = DuoLinkTextWhite,
    onSurfaceVariant = DuoLinkTextMuted,
    error = DuoLinkRedPrimary
)

@Composable
fun DuoLinkTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DuoLinkCharcoalBg.toArgb()
                window.navigationBarColor = DuoLinkCharcoalBg.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DuoLinkColorScheme,
        typography = DuoLinkTypography,
        content = content
    )
}
