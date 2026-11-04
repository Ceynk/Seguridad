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
import androidx.compose.material.icons.automirrored.outlined.PhoneCallback
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.antiphishing.smishguard.data.local.entity.CallRecordEntity
import com.antiphishing.smishguard.domain.model.SmsMessageData
import com.antiphishing.smishguard.ui.components.DateTimeFormats
import com.antiphishing.smishguard.ui.components.EmptyStateView
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.ScreenHeader
import com.antiphishing.smishguard.ui.components.SectionLabel
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.SemanticTone
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
    val colors = SmishGuardTheme.colors

    var coverage by remember { mutableStateOf(checkSystemCoverage(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        coverage = checkSystemCoverage(context)
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
        coverage = checkSystemCoverage(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncSettings()
                coverage = checkSystemCoverage(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val recentIncidents = remember(recentQuarantined, recentFlaggedCalls) {
        buildList {
            recentQuarantined.take(4).forEach { add(RecentIncidentItem.SmsIncident(it)) }
            recentFlaggedCalls.take(4).forEach { add(RecentIncidentItem.CallIncident(it)) }
        }.sortedByDescending { it.timestamp }.take(4)
    }

    val totalThreatsDetected = phishingCount + blockedCallsCount

    ResponsiveContentBox {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeader(
                    title = "SmishGuard",
                    subtitle = "Protección local contra smishing y llamadas fraudulentas",
                    trailing = {
                        StatusTag(
                            text = if (uiState.isProtectionActive) "Activo" else "Pausado",
                            tone = if (uiState.isProtectionActive) SemanticTone.SAFE else SemanticTone.WARNING
                        )
                    }
                )
            }

            item {
                ProtectionControlPanel(
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
                                arrayOf(
                                    Manifest.permission.READ_PHONE_STATE
                                )
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
                TelemetryStrip(
                    totalScanned = totalScanned,
                    threatsDetected = totalThreatsDetected,
                    quarantinedCount = quarantinedCount,
                    blockedCallsCount = blockedCallsCount,
                    onQuarantineClick = { onNavigateToQuarantine(0) },
                    onCallsClick = { onNavigateToQuarantine(1) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToAnalyzer,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FindInPage,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analizar texto",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { onNavigateToQuarantine(0) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surface,
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (quarantinedCount > 0) colors.warning else colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (quarantinedCount > 0) "Cuarentena ($quarantinedCount)" else "Cuarentena",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item {
                SectionLabel(
                    text = "Actividad reciente",
                    trailing = if (recentIncidents.isNotEmpty()) {
                        {
                            Text(
                                text = "Ver registro",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.accent,
                                modifier = Modifier.clickable { onNavigateToQuarantine(0) }
                            )
                        }
                    } else null
                )
            }

            if (recentIncidents.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.VerifiedUser,
                        title = "Sin incidentes registrados",
                        subtitle = "No hay mensajes en cuarentena ni llamadas maliciosas recientes."
                    )
                }
            } else {
                item {
                    SectionPanel {
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

@Composable
private fun ProtectionControlPanel(
    isActive: Boolean,
    isSilentDismissEnabled: Boolean,
    coverage: SystemCoverageStatus,
    onToggle: (Boolean) -> Unit,
    onRequestSmsPermission: () -> Unit,
    onRequestCallProtection: () -> Unit,
    onOpenNotificationAccess: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    val borderColor = when {
        !isActive -> colors.dangerBorder
        else -> colors.border
    }

    SectionPanel(borderColor = borderColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        .size(40.dp)
                        .background(
                            color = if (isActive) colors.safeContainer else colors.dangerContainer,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Outlined.Shield else Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = if (isActive) colors.safe else colors.danger,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isActive) "Escudo en tiempo real" else "Protección en pausa",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (isActive) {
                            "Inspección automática en el dispositivo"
                        } else {
                            "El filtrado automático de SMS y llamadas está desactivado"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) colors.textSecondary else colors.danger
                    )
                }
            }

            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = smishGuardSwitchColors()
            )
        }

        if (isActive) {
            val missingSms = !coverage.smsGranted
            val missingCalls = !coverage.callReady
            val missingNotif = isSilentDismissEnabled && !coverage.notifListenerEnabled

            PanelDivider()

            if (!missingSms && !missingCalls && !missingNotif) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(colors.safe, CircleShape)
                    )
                    Text(
                        text = "Canales activos: SMS · Llamadas · Notificaciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (missingSms) {
                        CoverageActionRow(
                            icon = Icons.Outlined.Sms,
                            title = "Permiso de lectura de SMS pendiente",
                            actionLabel = "Conceder",
                            onClick = onRequestSmsPermission
                        )
                    }
                    if (missingCalls) {
                        if (missingSms) PanelDivider()
                        CoverageActionRow(
                            icon = Icons.AutoMirrored.Outlined.PhoneCallback,
                            title = "Identificador de llamadas fraudulentas inactivo",
                            actionLabel = "Activar",
                            onClick = onRequestCallProtection
                        )
                    }
                    if (missingNotif) {
                        if (missingSms || missingCalls) PanelDivider()
                        CoverageActionRow(
                            icon = Icons.Outlined.NotificationsActive,
                            title = "Filtro silencioso de notificaciones sin acceso",
                            actionLabel = "Configurar",
                            onClick = onOpenNotificationAccess
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverageActionRow(
    icon: ImageVector,
    title: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.warning,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent
            )
        }
    }
}

@Composable
private fun TelemetryStrip(
    totalScanned: Int,
    threatsDetected: Int,
    quarantinedCount: Int,
    blockedCallsCount: Int,
    onQuarantineClick: () -> Unit,
    onCallsClick: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    SectionPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            TelemetryCell(
                label = "Analizados",
                value = totalScanned.toString(),
                valueColor = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 1.dp,
                color = colors.border
            )
            TelemetryCell(
                label = "Amenazas",
                value = threatsDetected.toString(),
                valueColor = if (threatsDetected > 0) colors.danger else colors.textPrimary,
                subtitle = if (blockedCallsCount > 0) "$blockedCallsCount llamada(s)" else null,
                onClick = if (blockedCallsCount > 0) onCallsClick else null,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 1.dp,
                color = colors.border
            )
            TelemetryCell(
                label = "Cuarentena",
                value = quarantinedCount.toString(),
                valueColor = if (quarantinedCount > 0) colors.warning else colors.textPrimary,
                onClick = onQuarantineClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TelemetryCell(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = SmishGuardTheme.colors
    val clickableMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .then(clickableMod)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentSmsIncidentRow(
    threat: SmsMessageData,
    onClick: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.MarkEmailUnread,
            contentDescription = null,
            tint = colors.danger,
            modifier = Modifier.size(20.dp)
        )

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
            tone = scoreToSemanticTone(threat.threatScore)
        )

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun RecentCallIncidentRow(
    call: CallRecordEntity,
    onClick: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    val tone = if (call.isBlocked) SemanticTone.DANGER else SemanticTone.WARNING
    val displayNumber = call.phoneNumber.ifBlank { "Número oculto" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CallEnd,
            contentDescription = null,
            tint = if (call.isBlocked) colors.danger else colors.warning,
            modifier = Modifier.size(20.dp)
        )

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
            modifier = Modifier.size(18.dp)
        )
    }
}
