package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyanSlateAccent,
    onPrimary = DeepSpaceBackground,
    secondary = NeonEmeraldGreen,
    onSecondary = DeepSpaceBackground,
    tertiary = NeonCrimsonRed,
    onTertiary = OffWhiteText,
    background = DeepSpaceBackground,
    onBackground = OffWhiteText,
    surface = DarkSurface,
    onSurface = OffWhiteText,
    surfaceVariant = DarkElevatedSurface,
    onSurfaceVariant = OffWhiteText,
    outline = DarkBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
