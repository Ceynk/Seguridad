package com.antiphishing.smishguard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Inventory2
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
        title = "Inicio",
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield
    )

    data object Quarantine : Screen(
        route = "quarantine",
        title = "Cuarentena",
        selectedIcon = Icons.Filled.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2
    )

    data object ManualAnalyzer : Screen(
        route = "analyzer",
        title = "Analizador",
        selectedIcon = Icons.Filled.FindInPage,
        unselectedIcon = Icons.Outlined.FindInPage
    )

    data object Settings : Screen(
        route = "settings",
        title = "Ajustes",
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
