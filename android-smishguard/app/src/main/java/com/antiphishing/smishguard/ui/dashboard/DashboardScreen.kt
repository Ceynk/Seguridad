package com.antiphishing.smishguard.ui.dashboard

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.PhoneCallback
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.antiphishing.smishguard.data.local.entity.CallRecordEntity
import com.antiphishing.smishguard.domain.model.SmsMessageData
import com.antiphishing.smishguard.ui.components.DateTimeFormats
import com.antiphishing.smishguard.ui.components.EmptyStateView
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.components.PanelHeaderBand
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.SectionLabel
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.SemanticTone
import com.antiphishing.smishguard.ui.components.SeverityBars
import com.antiphishing.smishguard.ui.components.StatusTag
import com.antiphishing.smishguard.ui.components.scoreToSemanticTone
import com.antiphishing.smishguard.ui.components.smishGuardSwitchColors
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme

@Immutable
private data class SystemCoverageStatus(
    val smsGranted: Boolean,
    val callReady: Boolean,
    val callRoleAvailable: Boolean,
    val notifListenerEnabled: Boolean
)

private sealed interface RecentIncidentItem {
    val key: String
    val timestamp: Long

    data class SmsIncident(val data: SmsMessageData) : RecentIncidentItem {
        override val key: String = "sms_${data.id}"
        override val timestamp: Long = data.timestamp
    }

    data class CallIncident(val data: CallRecordEntity) : RecentIncidentItem {
        override val key: String = "call_${data.id}"
        override val timestamp: Long = data.timestamp
    }
}

private fun checkSystemCoverage(context: Context): SystemCoverageStatus {
    val smsGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED

    val phoneGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    var roleHeld = false
    var roleAvailable = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            roleAvailable = true
            roleHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        }
    }

    val notifEnabled = NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)

    return SystemCoverageStatus(
        smsGranted = smsGranted,
        callReady = roleHeld || phoneGranted,
        callRoleAvailable = roleAvailable && !roleHeld,
        notifListenerEnabled = notifEnabled
    )
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToQuarantine: (initialTab: Int) -> Unit,
    onNavigateToAnalyzer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalScanned by viewModel.totalScanned.collectAsState()
    val quarantinedCount by viewModel.quarantinedCount.collectAsState()
    val phishingCount by viewModel.phishingCount.collectAsState()
    val blockedCallsCount by viewModel.blockedCallsCount.collectAsState()
    val recentQuarantined by viewModel.recentQuarantined.collectAsState()
    val recentFlaggedCalls by viewModel.recentFlaggedCalls.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val colors = SmishGuardTheme.colors

    var coverage by remember {
        mutableStateOf(
            SystemCoverageStatus(
                smsGranted = true,
                callReady = true,
                callRoleAvailable = false,
                notifListenerEnabled = true
            )
        )
    }

    fun refreshCoverageAsync() {
        coroutineScope.launch {
            coverage = withContext(Dispatchers.Default) {
                checkSystemCoverage(context)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        refreshCoverageAsync()
        val deniedPermissions = permissions.filterValues { !it }.keys
        if (deniedPermissions.isNotEmpty() && context is Activity && deniedPermissions.all {
                !ActivityCompat.shouldShowRequestPermissionRationale(context, it)
            }) {
            val settingsIntent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(settingsIntent)
        }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshCoverageAsync()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncSettings()
                refreshCoverageAsync()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val recentIncidents = remember(recentQuarantined, recentFlaggedCalls) {
        buildList {
            recentQuarantined.take(4).forEach { add(RecentIncidentItem.SmsIncident(it)) }
            recentFlaggedCalls.take(4).forEach { add(RecentIncidentItem.CallIncident(it)) }
        }.sortedByDescending { it.timestamp }.take(5)
    }

    val totalThreatsDetected = phishingCount + blockedCallsCount

    ResponsiveContentBox {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OperationalPostureConsole(
                    isActive = uiState.isProtectionActive,
                    isSilentDismissEnabled = uiState.isSilentDismissEnabled,
                    coverage = coverage,
                    onToggle = viewModel::toggleProtection,
                    onRequestSmsPermission = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.RECEIVE_SMS)
                        )
                    },
                    onRequestCallProtection = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && coverage.callRoleAvailable) {
                            val roleManager = context.getSystemService(RoleManager::class.java)
                            if (roleManager != null) {
                                roleLauncher.launch(
                                    roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                )
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_PHONE_STATE)
                            )
                        }
                    },
                    onOpenNotificationAccess = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionLabel(text = "Telemetría y aislamiento")
                    TelemetryMatrix(
                        totalScanned = totalScanned,
                        threatsDetected = totalThreatsDetected,
                        phishingSmsCount = phishingCount,
                        quarantinedCount = quarantinedCount,
                        blockedCallsCount = blockedCallsCount,
                        onQuarantineClick = { onNavigateToQuarantine(0) },
                        onCallsClick = { onNavigateToQuarantine(1) }
                    )
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    QuickInspectorLaunchStrip(onClick = onNavigateToAnalyzer)
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionLabel(
                        text = "Últimas intercepciones",
                        trailing = if (recentIncidents.isNotEmpty()) {
                            {
                                Row(
                                    modifier = Modifier.clickable { onNavigateToQuarantine(0) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Ver cuarentena",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.accent
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        } else null
                    )

                    if (recentIncidents.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Outlined.VerifiedUser,
                            title = "Sin incidentes recientes",
                            subtitle = "Tráfico de SMS y llamadas libre de amenazas activas."
                        )
                    } else {
                        SectionPanel {
                            PanelHeaderBand(
                                title = "Canal · Origen",
                                trailing = {
                                    Text(
                                        text = "RIESGO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                            )
                            recentIncidents.forEachIndexed { index, incident ->
                                if (index > 0) {
                                    PanelDivider()
                                }
                                when (incident) {
                                    is RecentIncidentItem.SmsIncident -> {
                                        RecentSmsIncidentRow(
                                            threat = incident.data,
                                            onClick = { onNavigateToQuarantine(0) }
                                        )
                                    }
                                    is RecentIncidentItem.CallIncident -> {
                                        RecentCallIncidentRow(
                                            call = incident.data,
                                            onClick = { onNavigateToQuarantine(1) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationalPostureConsole(
    isActive: Boolean,
    isSilentDismissEnabled: Boolean,
    coverage: SystemCoverageStatus,
    onToggle: (Boolean) -> Unit,
    onRequestSmsPermission: () -> Unit,
    onRequestCallProtection: () -> Unit,
    onOpenNotificationAccess: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    val notifReady = !isSilentDismissEnabled || coverage.notifListenerEnabled
    val allChannelsReady = coverage.smsGranted && coverage.callReady && notifReady

    val topStripeColor = when {
        !isActive -> colors.danger
        !allChannelsReady -> colors.warning
        else -> colors.safe
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(topStripeColor)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = if (isActive) colors.safeContainer else colors.dangerContainer,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isActive) colors.safeBorder else colors.dangerBorder,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Outlined.Shield else Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = if (isActive) colors.safe else colors.danger,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "SmishGuard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            StatusTag(
                                text = "LOCAL",
                                tone = SemanticTone.NEUTRAL
                            )
                        }
                        Text(
                            text = if (!isActive) {
                                "Inspección automática desactivada"
                            } else if (allChannelsReady) {
                                "Escudo activo en todos los canales"
                            } else {
                                "Escudo activo · Requiere permisos de canal"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!isActive) colors.danger else colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusTag(
                        text = if (isActive) "ACTIVO" else "PAUSADO",
                        tone = if (isActive) SemanticTone.SAFE else SemanticTone.DANGER,
                        showDot = true
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = onToggle,
                        colors = smishGuardSwitchColors()
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = colors.surfaceRecessed,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    ChannelStatusCell(
                        icon = Icons.Outlined.Sms,
                        label = "Canal SMS",
                        isActive = isActive && coverage.smsGranted,
                        actionText = if (!isActive) "Pausado" else if (coverage.smsGranted) "Operativo" else "Conceder",
                        needsAction = isActive && !coverage.smsGranted,
                        onClick = if (isActive && !coverage.smsGranted) onRequestSmsPermission else null,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        thickness = 1.dp,
                        color = colors.divider
                    )
                    ChannelStatusCell(
                        icon = Icons.AutoMirrored.Outlined.PhoneCallback,
                        label = "Llamadas",
                        isActive = isActive && coverage.callReady,
                        actionText = if (!isActive) "Pausado" else if (coverage.callReady) "Operativo" else "Activar",
                        needsAction = isActive && !coverage.callReady,
                        onClick = if (isActive && !coverage.callReady) onRequestCallProtection else null,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        thickness = 1.dp,
                        color = colors.divider
                    )
                    ChannelStatusCell(
                        icon = Icons.Outlined.NotificationsActive,
                        label = "Avisos",
                        isActive = isActive && notifReady,
                        actionText = when {
                            !isActive -> "Pausado"
                            !isSilentDismissEnabled -> "Opcional"
                            coverage.notifListenerEnabled -> "Operativo"
                            else -> "Configurar"
                        },
                        needsAction = isActive && !notifReady,
                        onClick = if (isActive && !notifReady) onOpenNotificationAccess else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = colors.border)
    }
}

@Composable
private fun ChannelStatusCell(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    actionText: String,
    needsAction: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    val dotColor = when {
        isActive -> colors.safe
        needsAction -> colors.warning
        else -> colors.textMuted
    }
    val statusColor = when {
        needsAction -> colors.accent
        isActive -> colors.safe
        else -> colors.textMuted
    }

    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (needsAction) colors.warning else colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dotColor, CircleShape)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (needsAction) FontWeight.Bold else FontWeight.SemiBold,
                color = statusColor,
                maxLines = 1
            )
            if (needsAction) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun TelemetryMatrix(
    totalScanned: Int,
    threatsDetected: Int,
    phishingSmsCount: Int,
    quarantinedCount: Int,
    blockedCallsCount: Int,
    onQuarantineClick: () -> Unit,
    onCallsClick: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionPanel {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "TRÁFICO INSPECCIONADO",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = totalScanned.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "eventos",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = colors.divider
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AMENAZAS DETECTADAS",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                        SeverityBars(
                            activeBars = when {
                                threatsDetected >= 5 -> 3
                                threatsDetected >= 1 -> 2
                                else -> 1
                            },
                            activeColor = if (threatsDetected > 0) colors.danger else colors.safe
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = threatsDetected.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (threatsDetected > 0) colors.danger else colors.textPrimary
                        )
                        Text(
                            text = "$phishingSmsCount SMS · $blockedCallsCount voz",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InteractiveTelemetryTile(
                icon = Icons.Outlined.AdminPanelSettings,
                label = "SMS EN CUARENTENA",
                value = quarantinedCount.toString(),
                actionLabel = "Abrir bóveda",
                accentColor = if (quarantinedCount > 0) colors.warning else colors.safe,
                onClick = onQuarantineClick,
                modifier = Modifier.weight(1f)
            )

            InteractiveTelemetryTile(
                icon = Icons.Outlined.CallEnd,
                label = "LLAMADAS BLOQUEADAS",
                value = blockedCallsCount.toString(),
                actionLabel = "Ver registro",
                accentColor = if (blockedCallsCount > 0) colors.danger else colors.safe,
                onClick = onCallsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InteractiveTelemetryTile(
    icon: ImageVector,
    label: String,
    value: String,
    actionLabel: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            PanelDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceSubtle)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickInspectorLaunchStrip(
    onClick: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.borderStrong)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.accentContainer, RoundedCornerShape(6.dp))
                        .border(1.dp, colors.accent.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Policy,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Inspección manual de SMS o enlace",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Evalúa remitentes, texto o direcciones URL antes de abrirlos",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.heightIn(min = 38.dp)
            ) {
                Text(
                    text = "Analizar",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun RecentSmsIncidentRow(
    threat: SmsMessageData,
    onClick: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    val tone = scoreToSemanticTone(threat.threatScore)
    val edgeColor = when (tone) {
        SemanticTone.DANGER -> colors.danger
        SemanticTone.WARNING -> colors.warning
        else -> colors.safe
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(edgeColor)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(colors.surfaceRecessed, RoundedCornerShape(4.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "SMS",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textSecondary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = threat.sender,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DateTimeFormats.formatShort(threat.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
                Text(
                    text = threat.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            StatusTag(
                text = "${threat.threatScore}%",
                tone = tone
            )

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun RecentCallIncidentRow(
    call: CallRecordEntity,
    onClick: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    val tone = if (call.isBlocked) SemanticTone.DANGER else SemanticTone.WARNING
    val edgeColor = if (call.isBlocked) colors.danger else colors.warning
    val displayNumber = call.phoneNumber.ifBlank { "Número oculto" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(edgeColor)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(colors.surfaceRecessed, RoundedCornerShape(4.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "VOZ",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textSecondary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayNumber,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DateTimeFormats.formatShort(call.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
                Text(
                    text = call.summaryReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            StatusTag(
                text = if (call.isBlocked) "Bloqueada" else "${call.threatScore}%",
                tone = tone
            )

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
