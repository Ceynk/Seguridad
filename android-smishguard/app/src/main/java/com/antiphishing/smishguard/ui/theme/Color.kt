package com.antiphishing.smishguard.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Light palette
val LightBackground = Color(0xFFF6F7F9)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceSubtle = Color(0xFFEEF1F5)
val LightBorder = Color(0xFFDDE2EA)
val LightOutline = Color(0xFF94A3B8)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextMuted = Color(0xFF64748B)

val LightPrimary = Color(0xFF1D4ED8)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDBEAFE)
val LightOnPrimaryContainer = Color(0xFF1E3A8A)

val LightSafe = Color(0xFF15803D)
val LightSafeContainer = Color(0xFFDCFCE7)
val LightSafeBorder = Color(0xFFBBF7D0)

val LightWarning = Color(0xFFB45309)
val LightWarningContainer = Color(0xFFFEF3C7)
val LightWarningBorder = Color(0xFFFDE68A)

val LightDanger = Color(0xFFB91C1C)
val LightDangerContainer = Color(0xFFFEE2E2)
val LightDangerBorder = Color(0xFFFECACA)

// Dark palette
val DarkBackground = Color(0xFF0D1117)
val DarkSurface = Color(0xFF151B23)
val DarkSurfaceSubtle = Color(0xFF1C2430)
val DarkBorder = Color(0xFF263040)
val DarkOutline = Color(0xFF4B586E)
val DarkTextPrimary = Color(0xFFEDF2F7)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextMuted = Color(0xFF6E7E94)

val DarkPrimary = Color(0xFF5899FA)
val DarkOnPrimary = Color(0xFF081226)
val DarkPrimaryContainer = Color(0xFF172B4D)
val DarkOnPrimaryContainer = Color(0xFFBFD8FF)

val DarkSafe = Color(0xFF3FB950)
val DarkSafeContainer = Color(0xFF122B1B)
val DarkSafeBorder = Color(0xFF1E4B2D)

val DarkWarning = Color(0xFFD29922)
val DarkWarningContainer = Color(0xFF2E220B)
val DarkWarningBorder = Color(0xFF543D12)

val DarkDanger = Color(0xFFF85149)
val DarkDangerContainer = Color(0xFF311517)
val DarkDangerBorder = Color(0xFF5C2124)

@Immutable
data class SmishGuardColors(
    val background: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val safe: Color,
    val safeContainer: Color,
    val safeBorder: Color,
    val warning: Color,
    val warningContainer: Color,
    val warningBorder: Color,
    val danger: Color,
    val dangerContainer: Color,
    val dangerBorder: Color
)

val LightSmishGuardColors = SmishGuardColors(
    background = LightBackground,
    surface = LightSurface,
    surfaceSubtle = LightSurfaceSubtle,
    border = LightBorder,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    accent = LightPrimary,
    onAccent = LightOnPrimary,
    accentContainer = LightPrimaryContainer,
    onAccentContainer = LightOnPrimaryContainer,
    safe = LightSafe,
    safeContainer = LightSafeContainer,
    safeBorder = LightSafeBorder,
    warning = LightWarning,
    warningContainer = LightWarningContainer,
    warningBorder = LightWarningBorder,
    danger = LightDanger,
    dangerContainer = LightDangerContainer,
    dangerBorder = LightDangerBorder
)

val DarkSmishGuardColors = SmishGuardColors(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceSubtle = DarkSurfaceSubtle,
    border = DarkBorder,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    accent = DarkPrimary,
    onAccent = DarkOnPrimary,
    accentContainer = DarkPrimaryContainer,
    onAccentContainer = DarkOnPrimaryContainer,
    safe = DarkSafe,
    safeContainer = DarkSafeContainer,
    safeBorder = DarkSafeBorder,
    warning = DarkWarning,
    warningContainer = DarkWarningContainer,
    warningBorder = DarkWarningBorder,
    danger = DarkDanger,
    dangerContainer = DarkDangerContainer,
    dangerBorder = DarkDangerBorder
)
