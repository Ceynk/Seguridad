package com.antiphishing.smishguard.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                ArchitecturalBottomBar(
                    items = Screen.bottomNavItems,
                    currentRoute = currentRoute,
                    quarantinedCount = quarantinedCount,
                    onSelect = { screen ->
                        if (screen == Screen.Quarantine) {
                            quarantineTab = 0
                        }
                        navigateToTopLevel(screen.route)
                    }
                )
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
                ArchitecturalNavigationRail(
                    items = Screen.bottomNavItems,
                    currentRoute = currentRoute,
                    quarantinedCount = quarantinedCount,
                    onSelect = { screen ->
                        if (screen == Screen.Quarantine) {
                            quarantineTab = 0
                        }
                        navigateToTopLevel(screen.route)
                    }
                )
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
private fun ArchitecturalBottomBar(
    items: List<Screen>,
    currentRoute: String?,
    quarantinedCount: Int,
    onSelect: (Screen) -> Unit
) {
    val colors = SmishGuardTheme.colors
    Surface(
        color = colors.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            HorizontalDivider(thickness = 1.dp, color = colors.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (screen in items) {
                    val selected = currentRoute == screen.route
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onSelect(screen) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .width(36.dp)
                                    .height(2.5.dp)
                                    .background(
                                        color = colors.accent,
                                        shape = RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)
                                    )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            NavIconBadgeBox(
                                screen = screen,
                                selected = selected,
                                quarantinedCount = quarantinedCount
                            )
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) colors.textPrimary else colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchitecturalNavigationRail(
    items: List<Screen>,
    currentRoute: String?,
    quarantinedCount: Int,
    onSelect: (Screen) -> Unit
) {
    val colors = SmishGuardTheme.colors
    Column(
        modifier = Modifier
            .width(88.dp)
            .fillMaxHeight()
            .background(colors.surface)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.accentContainer, RoundedCornerShape(8.dp))
                .border(1.dp, colors.accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = "SmishGuard",
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = colors.divider
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (screen in items) {
                val selected = currentRoute == screen.route
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable { onSelect(screen) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(3.dp)
                                .height(32.dp)
                                .background(
                                    color = colors.accent,
                                    shape = RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)
                                )
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        NavIconBadgeBox(
                            screen = screen,
                            selected = selected,
                            quarantinedCount = quarantinedCount
                        )
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) colors.textPrimary else colors.textMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NavIconBadgeBox(
    screen: Screen,
    selected: Boolean,
    quarantinedCount: Int
) {
    val colors = SmishGuardTheme.colors
    val icon = if (selected) screen.selectedIcon else screen.unselectedIcon
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(26.dp)
                .background(
                    color = if (selected) colors.accentContainer else Color.Transparent,
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = screen.title,
                    tint = if (selected) colors.accent else colors.textMuted,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        if (screen == Screen.Quarantine && quarantinedCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-3).dp)
                    .background(colors.danger, RoundedCornerShape(4.dp))
                    .border(1.dp, colors.surface, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (quarantinedCount > 99) "99+" else "$quarantinedCount",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.5.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
