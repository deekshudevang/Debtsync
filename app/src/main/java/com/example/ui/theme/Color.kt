package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkMode = compositionLocalOf { true }

val DeepSpaceBackground: Color
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFF030305) else Color(0xFFF4F6F8)

val DarkSurface: Color
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFF121217) else Color(0xFFFFFFFF)

val DarkElevatedSurface: Color
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFF1E1E26) else Color(0xFFE8EAF6)

val NeonEmeraldGreen: Color
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFF00E676) else Color(0xFF00A251)

val LightEmeraldGreen: Color
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFF00E676) else Color(0xFF00A251)

val NeonEmeraldGreenBg: Color 
    @Composable get() = if (LocalIsDarkMode.current) Color(0x3300E676) else Color(0x3300A251)

val NeonCrimsonRed: Color
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFFFF3D00) else Color(0xFFD50000)

val LightCrimsonRed: Color
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFFFF3D00) else Color(0xFFD50000)

val NeonCrimsonRedBg: Color 
    @Composable get() = if (LocalIsDarkMode.current) Color(0x33FF3D00) else Color(0x33D50000)

val CyanSlateAccent: Color
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFF40C4FF) else Color(0xFF0091EA)

val MatteGoldAccent: Color
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFFD4AF37) else Color(0xFFF57F17)

val GradientStart: Color
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFF0D47A1) else Color(0xFF1976D2)

val GradientEnd: Color 
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFF1A237E) else Color(0xFF0D47A1)

val OffWhiteText: Color 
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFFF0F0F5) else Color(0xFF1F1F2C)

val MutedSlateText: Color 
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFF8E8E9E) else Color(0xFF5A5A68)

val DarkBorder: Color
    @Composable get() = if (LocalIsDarkMode.current) Color(0xFF2C2C35) else Color(0xFFD1D1E0)
