package com.antiphishing.smishguard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Protección", Icons.Default.Security)
    object Quarantine : Screen("quarantine", "Bóveda Honeypot", Icons.Default.Lock)
    object ManualAnalyzer : Screen("analyzer", "Analizador", Icons.Default.BugReport)
    object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
    object Whitelist : Screen("whitelist", "Lista Blanca")
    object CustomRules : Screen("custom_rules", "Reglas Propias")

    companion object {
        val bottomNavItems = listOf(Dashboard, Quarantine, ManualAnalyzer, Settings)
    }
}
