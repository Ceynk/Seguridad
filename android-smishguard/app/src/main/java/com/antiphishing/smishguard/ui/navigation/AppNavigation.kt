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
import com.antiphishing.smishguard.ui.settings.CommunityBlacklistScreen
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

class AppViewModelFactory(private val app: SmishGuardApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(app.smsRepository, app.settingsRepository) as T
            modelClass.isAssignableFrom(QuarantineViewModel::class.java) ->
                QuarantineViewModel(app.smsRepository) as T
            modelClass.isAssignableFrom(ManualAnalyzerViewModel::class.java) ->
                ManualAnalyzerViewModel(app.smsRepository, app.settingsRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(app.settingsRepository, app.smsRepository, app.callRepository, app.communityApiClient) as T
            else -> throw IllegalArgumentException("ViewModel no reconocido")
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    app: SmishGuardApp,
    startDestination: String = Screen.Dashboard.route,
    initialSharedSender: String = "",
    initialSharedBody: String = ""
) {
    val factory = remember { AppViewModelFactory(app) }
    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val quarantineViewModel: QuarantineViewModel = viewModel(factory = factory)
    val analyzerViewModel: ManualAnalyzerViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

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
            val isBottomNavScreen = Screen.bottomNavItems.any { it.route == currentRoute }
            if (isBottomNavScreen) {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp
                ) {
                    for (screen in Screen.bottomNavItems) {
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    val startDestId = navController.graph.findStartDestination().id
                                    popUpTo(startDestId) {
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
                                    if (screen.icon != null) {
                                        Icon(
                                            imageVector = screen.icon,
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
                    ,
                    onNavigateToCommunityBlacklist = { navController.navigate(Screen.CommunityBlacklist.route) }
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
            composable(Screen.CommunityBlacklist.route) {
                CommunityBlacklistScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
