package com.antiphishing.smishguard.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

val LightBackground = Color(0xFFF1F4F9)
val LightSurfaceHeader = Color(0xFFE7ECF4)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightSurfaceSubtle = Color(0xFFF6F8FB)
val LightSurfaceRecessed = Color(0xFFEAEEF5)
val LightBorder = Color(0xFFD5DCE6)
val LightBorderStrong = Color(0xFFB8C4D4)
val LightDivider = Color(0xFFE3E8F0)
val LightOutline = Color(0xFF8A99AD)
val LightTextPrimary = Color(0xFF0B1324)
val LightTextSecondary = Color(0xFF3E4C63)
val LightTextMuted = Color(0xFF64748B)

val LightPrimary = Color(0xFF1E40AF)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE0E8F9)
val LightOnPrimaryContainer = Color(0xFF172E6E)

val LightSafe = Color(0xFF0F766E)
val LightSafeContainer = Color(0xFFE3F5F2)
val LightSafeBorder = Color(0xFFA3DBD3)

val LightWarning = Color(0xFF9A5B06)
val LightWarningContainer = Color(0xFFFDF3E1)
val LightWarningBorder = Color(0xFFF2D199)

val LightDanger = Color(0xFFA61B29)
val LightDangerContainer = Color(0xFFFCE8EA)
val LightDangerBorder = Color(0xFFF2B6BD)

val DarkBackground = Color(0xFF0A0E14)
val DarkSurfaceHeader = Color(0xFF0F1520)
val DarkSurface = Color(0xFF121924)
val DarkSurfaceElevated = Color(0xFF182230)
val DarkSurfaceSubtle = Color(0xFF0E141D)
val DarkSurfaceRecessed = Color(0xFF080B10)
val DarkBorder = Color(0xFF212C3D)
val DarkBorderStrong = Color(0xFF314159)
val DarkDivider = Color(0xFF1A2332)
val DarkOutline = Color(0xFF4B5C73)
val DarkTextPrimary = Color(0xFFF0F4F8)
val DarkTextSecondary = Color(0xFF9FB0C6)
val DarkTextMuted = Color(0xFF687A92)

val DarkPrimary = Color(0xFF4C8DFF)
val DarkOnPrimary = Color(0xFF071021)
val DarkPrimaryContainer = Color(0xFF15294A)
val DarkOnPrimaryContainer = Color(0xFFC7DCFF)

val DarkSafe = Color(0xFF2EB88A)
val DarkSafeContainer = Color(0xFF0D261E)
val DarkSafeBorder = Color(0xFF1B4D3D)

val DarkWarning = Color(0xFFD99B26)
val DarkWarningContainer = Color(0xFF291E09)
val DarkWarningBorder = Color(0xFF523B12)

val DarkDanger = Color(0xFFE54858)
val DarkDangerContainer = Color(0xFF2B1216)
val DarkDangerBorder = Color(0xFF592229)

@Immutable
data class SmishGuardColors(
    val background: Color,
    val surfaceHeader: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val surfaceRecessed: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
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
    surfaceHeader = LightSurfaceHeader,
    surface = LightSurface,
    surfaceElevated = LightSurfaceElevated,
    surfaceSubtle = LightSurfaceSubtle,
    surfaceRecessed = LightSurfaceRecessed,
    border = LightBorder,
    borderStrong = LightBorderStrong,
    divider = LightDivider,
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
    surfaceHeader = DarkSurfaceHeader,
    surface = DarkSurface,
    surfaceElevated = DarkSurfaceElevated,
    surfaceSubtle = DarkSurfaceSubtle,
    surfaceRecessed = DarkSurfaceRecessed,
    border = DarkBorder,
    borderStrong = DarkBorderStrong,
    divider = DarkDivider,
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
