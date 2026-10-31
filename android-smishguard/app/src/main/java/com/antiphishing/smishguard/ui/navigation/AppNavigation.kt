package com.antiphishing.smishguard.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.antiphishing.smishguard.SmishGuardApp
import com.antiphishing.smishguard.ui.analyzer.ManualAnalyzerScreen
import com.antiphishing.smishguard.ui.analyzer.ManualAnalyzerViewModel
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.dashboard.DashboardScreen
import com.antiphishing.smishguard.ui.dashboard.DashboardViewModel
import com.antiphishing.smishguard.ui.quarantine.QuarantineScreen
import com.antiphishing.smishguard.ui.quarantine.QuarantineViewModel
import com.antiphishing.smishguard.ui.settings.CommunityBlacklistScreen
import com.antiphishing.smishguard.ui.settings.CustomRulesScreen
import com.antiphishing.smishguard.ui.settings.SettingsScreen
import com.antiphishing.smishguard.ui.settings.SettingsViewModel
import com.antiphishing.smishguard.ui.settings.WhitelistScreen
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme

class AppViewModelFactory(private val app: SmishGuardApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(app.smsRepository, app.settingsRepository, app.callRepository) as T
            modelClass.isAssignableFrom(QuarantineViewModel::class.java) ->
                QuarantineViewModel(app.smsRepository, app.callRepository) as T
            modelClass.isAssignableFrom(ManualAnalyzerViewModel::class.java) ->
                ManualAnalyzerViewModel(app.smsRepository, app.settingsRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    app.settingsRepository,
                    app.smsRepository,
                    app.callRepository,
                    app.communityApiClient
                ) as T
            else -> throw IllegalArgumentException("ViewModel no reconocido: ${modelClass.name}")
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
    val factory = remember(app) { AppViewModelFactory(app) }
    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val quarantineViewModel: QuarantineViewModel = viewModel(factory = factory)
    val analyzerViewModel: ManualAnalyzerViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    var quarantineTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(initialSharedSender, initialSharedBody) {
        if (initialSharedBody.isNotBlank() || initialSharedSender.isNotBlank()) {
            analyzerViewModel.setSharedInput(initialSharedSender, initialSharedBody)
        }
    }

    val quarantinedCount by dashboardViewModel.quarantinedCount.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colors = SmishGuardTheme.colors

    val configuration = LocalConfiguration.current
    val useNavigationRail = configuration.screenWidthDp >= 600
    val isTopLevelRoute = Screen.bottomNavItems.any { it.route == currentRoute }

    val navigateToTopLevel: (String) -> Unit = remember(navController) {
        { route ->
            if (currentRoute != route) {
                navController.navigate(route) {
                    val startDestId = navController.graph.findStartDestination().id
                    popUpTo(startDestId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (!useNavigationRail && isTopLevelRoute) {
                Column {
                    PanelDivider()
                    NavigationBar(
                        containerColor = colors.surface,
                        tonalElevation = 0.dp
                    ) {
                        for (screen in Screen.bottomNavItems) {
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (screen == Screen.Quarantine) {
                                        quarantineTab = 0
                                    }
                                    navigateToTopLevel(screen.route)
                                },
                                icon = {
                                    NavIconWithBadge(
                                        screen = screen,
                                        selected = selected,
                                        quarantinedCount = quarantinedCount
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.onAccentContainer,
                                    indicatorColor = colors.accentContainer,
                                    selectedTextColor = colors.accent,
                                    unselectedIconColor = colors.textMuted,
                                    unselectedTextColor = colors.textMuted
                                )
                            )
                        }
                    }
                }
            }
        },
        containerColor = colors.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (useNavigationRail && isTopLevelRoute) {
                NavigationRail(
                    containerColor = colors.surface
                ) {
                    Spacer(modifier = Modifier.weight(0.15f))
                    for (screen in Screen.bottomNavItems) {
                        val selected = currentRoute == screen.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                if (screen == Screen.Quarantine) {
                                    quarantineTab = 0
                                }
                                navigateToTopLevel(screen.route)
                            },
                            icon = {
                                NavIconWithBadge(
                                    screen = screen,
                                    selected = selected,
                                    quarantinedCount = quarantinedCount
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = colors.onAccentContainer,
                                indicatorColor = colors.accentContainer,
                                selectedTextColor = colors.accent,
                                unselectedIconColor = colors.textMuted,
                                unselectedTextColor = colors.textMuted
                            )
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = colors.border
                )
            }

            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToQuarantine = { tab ->
                            quarantineTab = tab
                            navigateToTopLevel(Screen.Quarantine.route)
                        },
                        onNavigateToAnalyzer = {
                            navigateToTopLevel(Screen.ManualAnalyzer.route)
                        }
                    )
                }
                composable(Screen.Quarantine.route) {
                    QuarantineScreen(
                        viewModel = quarantineViewModel,
                        initialTab = quarantineTab
                    )
                }
                composable(Screen.ManualAnalyzer.route) {
                    ManualAnalyzerScreen(viewModel = analyzerViewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateToWhitelist = { navController.navigate(Screen.Whitelist.route) },
                        onNavigateToCustomRules = { navController.navigate(Screen.CustomRules.route) },
                        onNavigateToCommunityBlacklist = {
                            navController.navigate(Screen.CommunityBlacklist.route)
                        }
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
}

@Composable
private fun NavIconWithBadge(
    screen: Screen,
    selected: Boolean,
    quarantinedCount: Int
) {
    val colors = SmishGuardTheme.colors
    val icon = if (selected) screen.selectedIcon else screen.unselectedIcon
    BadgedBox(
        badge = {
            if (screen == Screen.Quarantine && quarantinedCount > 0) {
                Badge(
                    containerColor = colors.danger,
                    contentColor = Color.White
                ) {
                    Text(
                        text = if (quarantinedCount > 99) "99+" else "$quarantinedCount",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = screen.title
            )
        }
    }
}
