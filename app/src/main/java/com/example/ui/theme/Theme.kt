package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
