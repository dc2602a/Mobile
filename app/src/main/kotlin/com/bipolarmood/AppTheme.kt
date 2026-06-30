package com.bipolarmood

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object AppTheme {
    val CreamBackground = Color(0xFFF7F4EF)
    val CreamSurface = Color(0xFFFFFCF8)
    val SoftLilac = Color(0xFF8B7FD4)
    val SoftTeal = Color(0xFF5BB5A2)
    val WarmAmber = Color(0xFFE8A849)
    val SoftCoral = Color(0xFFE07A7A)
    val InkPrimary = Color(0xFF2D3142)
    val InkSecondary = Color(0xFF6B7280)
    val InkMuted = Color(0xFF9CA3AF)
    val PaperNote = Color(0xFFFFF8E7)
    val NightBackground = Color(0xFF1A1D26)
    val NightSurface = Color(0xFF242936)

    fun lightScheme() = lightColorScheme(
        primary = SoftLilac,
        onPrimary = Color.White,
        secondary = SoftTeal,
        onSecondary = Color.White,
        tertiary = WarmAmber,
        background = CreamBackground,
        surface = CreamSurface,
        surfaceVariant = Color(0xFFEDE8E3),
        onBackground = InkPrimary,
        onSurface = InkPrimary,
        onSurfaceVariant = InkSecondary,
        error = SoftCoral
    )

    fun darkScheme() = darkColorScheme(
        primary = Color(0xFFB8AEEB),
        onPrimary = Color(0xFF1F1635),
        secondary = Color(0xFF7FD4C3),
        onSecondary = Color(0xFF0F2A24),
        tertiary = WarmAmber,
        background = NightBackground,
        surface = NightSurface,
        surfaceVariant = Color(0xFF2E3340),
        onBackground = Color(0xFFF3F1EC),
        onSurface = Color(0xFFF3F1EC),
        onSurfaceVariant = Color(0xFFB8BBC6),
        error = SoftCoral
    )
}
