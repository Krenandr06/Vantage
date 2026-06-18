package com.vantage.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VantageColorScheme = lightColorScheme(
    primary = Graphite,
    onPrimary = Bone,
    primaryContainer = Cream,
    onPrimaryContainer = Graphite,
    secondary = Clay,
    onSecondary = Bone,
    secondaryContainer = ClaySoft,
    onSecondaryContainer = Graphite,
    tertiary = Sage,
    onTertiary = Bone,
    background = Bone,
    onBackground = Graphite,
    surface = Surface,
    onSurface = Graphite,
    surfaceVariant = Cream,
    onSurfaceVariant = GraphiteSoft,
    outline = Hair,
    outlineVariant = GraphiteMute,
)

@Composable
fun VantageTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = VantageColorScheme,
        typography = VantageTypography,
        content = content,
    )
}
