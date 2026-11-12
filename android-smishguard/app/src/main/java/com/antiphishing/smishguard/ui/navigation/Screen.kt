package com.antiphishing.smishguard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Estado",
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield
    )

    data object Quarantine : Screen(
        route = "quarantine",
        title = "Cuarentena",
        selectedIcon = Icons.Filled.AdminPanelSettings,
        unselectedIcon = Icons.Outlined.AdminPanelSettings
    )

    data object ManualAnalyzer : Screen(
        route = "analyzer",
        title = "Inspector",
        selectedIcon = Icons.Filled.Policy,
        unselectedIcon = Icons.Outlined.Policy
    )

    data object Settings : Screen(
        route = "settings",
        title = "Sistema",
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    )

    data object Whitelist : Screen("whitelist", "Lista blanca")
    data object CustomRules : Screen("custom_rules", "Reglas personalizadas")
    data object CommunityBlacklist : Screen("community_blacklist", "Lista negra comunitaria")

    companion object {
        val bottomNavItems: List<Screen> by lazy {
            listOf(Dashboard, Quarantine, ManualAnalyzer, Settings)
        }
    }
}
