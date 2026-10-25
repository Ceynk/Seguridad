package com.antiphishing.smishguard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Dashboard : Screen("dashboard", "Protección", Icons.Default.Security)
    data object Quarantine : Screen("quarantine", "Cuarentena", Icons.Default.Lock)
    data object ManualAnalyzer : Screen("analyzer", "Analizador", Icons.Default.BugReport)
    data object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
    data object Whitelist : Screen("whitelist", "Lista Blanca")
    data object CustomRules : Screen("custom_rules", "Reglas Propias")
    data object CommunityBlacklist : Screen("community_blacklist", "Lista Negra Comunitaria")
    data object BlockedUrl : Screen("blocked_url", "Enlace Bloqueado")

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Dashboard, Quarantine, ManualAnalyzer, Settings)
    }
}
