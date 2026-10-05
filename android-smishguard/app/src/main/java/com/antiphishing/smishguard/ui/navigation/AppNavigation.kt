package com.antiphishing.smishguard.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.antiphishing.smishguard.SmishGuardApp
import com.antiphishing.smishguard.ui.analyzer.ManualAnalyzerScreen
import com.antiphishing.smishguard.ui.analyzer.ManualAnalyzerViewModel
import com.antiphishing.smishguard.ui.dashboard.DashboardScreen
import com.antiphishing.smishguard.ui.dashboard.DashboardViewModel
import com.antiphishing.smishguard.ui.quarantine.QuarantineScreen
import com.antiphishing.smishguard.ui.quarantine.QuarantineViewModel
import com.antiphishing.smishguard.ui.settings.CustomRulesScreen
import com.antiphishing.smishguard.ui.settings.SettingsScreen
import com.antiphishing.smishguard.ui.settings.SettingsViewModel
import com.antiphishing.smishguard.ui.settings.WhitelistScreen
import com.antiphishing.smishguard.ui.theme.CyberCyan
import com.antiphishing.smishguard.ui.theme.DarkBackground
import com.antiphishing.smishguard.ui.theme.DarkSurface
import com.antiphishing.smishguard.ui.theme.TextSecondary
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antiphishing.smishguard.ui.theme.ThreatDangerRed

@Composable
fun AppNavigation(
    navController: NavHostController,
    app: SmishGuardApp,
    startDestination: String = Screen.Dashboard.route,
    initialSharedSender: String = "",
    initialSharedBody: String = ""
) {
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(app.smsRepository, app.settingsRepository) as T
            }
        }
    )
    val quarantineViewModel: QuarantineViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuarantineViewModel(app.smsRepository) as T
            }
        }
    )
    val analyzerViewModel: ManualAnalyzerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ManualAnalyzerViewModel(app.smsRepository) as T
            }
        }
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(app.settingsRepository, app.smsRepository, app.callRepository) as T
            }
        }
    )

    LaunchedEffect(initialSharedBody) {
        if (initialSharedBody.isNotBlank()) {
            analyzerViewModel.setSharedInput(initialSharedSender, initialSharedBody)
        }
    }

    val quarantinedCount by dashboardViewModel.quarantinedCount.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (Screen.bottomNavItems.any { it.route == currentRoute }) {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (screen == Screen.Quarantine && quarantinedCount > 0) {
                                            Badge(
                                                containerColor = ThreatDangerRed,
                                                contentColor = Color.White
                                            ) {
                                                Text("$quarantinedCount", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                ) {
                                    screen.icon?.let {
                                        Icon(
                                            imageVector = it,
                                            contentDescription = screen.title
                                        )
                                    }
                                }
                            },
                            label = { Text(screen.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkBackground,
                                indicatorColor = CyberCyan,
                                selectedTextColor = CyberCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToQuarantine = { navController.navigate(Screen.Quarantine.route) },
                    onNavigateToAnalyzer = { navController.navigate(Screen.ManualAnalyzer.route) }
                )
            }
            composable(Screen.Quarantine.route) {
                QuarantineScreen(viewModel = quarantineViewModel)
            }
            composable(Screen.ManualAnalyzer.route) {
                ManualAnalyzerScreen(viewModel = analyzerViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToWhitelist = { navController.navigate(Screen.Whitelist.route) },
                    onNavigateToCustomRules = { navController.navigate(Screen.CustomRules.route) }
                )
            }
            composable(Screen.Whitelist.route) {
                WhitelistScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.CustomRules.route) {
                CustomRulesScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
