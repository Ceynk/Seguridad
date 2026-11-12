package com.antiphishing.smishguard.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antiphishing.smishguard.data.repository.SecuritySettingsRepository
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.components.PanelHeaderBand
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.SectionLabel
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.SemanticTone
import com.antiphishing.smishguard.ui.components.StatusTag
import com.antiphishing.smishguard.ui.components.TopScreenBand
import com.antiphishing.smishguard.ui.components.smishGuardSwitchColors
import com.antiphishing.smishguard.ui.theme.SmishGuardDimensions
import com.antiphishing.smishguard.ui.theme.SmishGuardSpacing
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToCustomRules: () -> Unit,
    onNavigateToCommunityBlacklist: () -> Unit
) {
    val threshold by viewModel.quarantineThreshold.collectAsState()
    val aggressiveMode by viewModel.aggressiveMode.collectAsState()
    val silentDismiss by viewModel.silentDismissNotif.collectAsState()
    val blockPrivateCalls by viewModel.blockPrivateCalls.collectAsState()
    val blockInvalidCalls by viewModel.blockInvalidCalls.collectAsState()
    val internationalPolicy by viewModel.internationalCallPolicy.collectAsState()
    val communitySyncEnabled by viewModel.communitySyncEnabled.collectAsState()
    val whitelist by viewModel.whitelist.collectAsState()
    val customRules by viewModel.customRules.collectAsState()
    val communityEntries by viewModel.communityEntries.collectAsState()
    val userReports by viewModel.userReports.collectAsState()

    val colors = SmishGuardTheme.colors
    val context = LocalContext.current
    var sliderValue by remember(threshold) { mutableFloatStateOf(threshold.toFloat()) }
    var clearDialogType by remember { mutableStateOf<String?>(null) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showOriginalExportDialog by remember { mutableStateOf(false) }
    var exportContent by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val content = exportContent
        if (uri != null && content != null) {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(content.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, "Reportes exportados.", Toast.LENGTH_SHORT).show()
        }
        exportContent = null
    }

    val displayThreshold = sliderValue.roundToInt()

    androidx.compose.runtime.LaunchedEffect(communitySyncEnabled) {
        while (communitySyncEnabled) {
            viewModel.sincronizarComunidadSiNecesario()
            kotlinx.coroutines.delay(5_000L)
        }
    }
    val sensitivityProfile = when {
        displayThreshold <= 35 -> "Estricto"
        displayThreshold <= 55 -> "Equilibrado"
        else -> "Conservador"
    }

    val activeCustomRulesCount = remember(customRules) {
        customRules.count { it.isEnabled }
    }

    ResponsiveContentBox {
        Column(modifier = Modifier.fillMaxSize()) {
            TopScreenBand(
                title = "Parámetros del sistema",
                subtitle = "Calibración heurística, listas de control y mantenimiento",
                badgeText = "LOCAL"
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(text = "Motor de detección")
                        SectionPanel {
                            PanelHeaderBand(
                                title = "Calibración de sensibilidad",
                                trailing = {
                                    StatusTag(
                                        text = "$displayThreshold% · $sensitivityProfile",
                                        tone = SemanticTone.ACCENT
                                    )
                                }
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Umbral de cuarentena",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Aísla desde $displayThreshold pts",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.accent
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    SensitivityZoneChip(
                                        label = "Estricto (20-35%)",
                                        active = displayThreshold <= 35,
                                        modifier = Modifier.weight(1f)
                                    )
                                    SensitivityZoneChip(
                                        label = "Equilibrado (36-55%)",
                                        active = displayThreshold in 36..55,
                                        modifier = Modifier.weight(1f)
                                    )
                                    SensitivityZoneChip(
                                        label = "Conservador (56+)",
                                        active = displayThreshold > 55,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Slider(
                                    value = sliderValue,
                                    onValueChange = { sliderValue = it },
                                    onValueChangeFinished = {
                                        viewModel.setQuarantineThreshold(sliderValue.roundToInt())
                                    },
                                    valueRange = 20f..80f,
                                    steps = 11,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colors.accent,
                                        activeTrackColor = colors.accent,
                                        inactiveTrackColor = colors.surfaceRecessed,
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent
                                    )
                                )
                            }

                            PanelDivider()

                            SettingToggleItem(
                                title = "Suprimir notificaciones de phishing",
                                subtitle = "Oculta avisos de mensajería cuando el SMS entra en cuarentena",
                                checked = silentDismiss,
                                onCheckedChange = viewModel::setSilentDismissNotif
                            )

                            PanelDivider()

                            SettingToggleItem(
                                title = "Heurística estricta",
                                subtitle = "Mayor penalización en dominios recientes y acortadores",
                                checked = aggressiveMode,
                                onCheckedChange = viewModel::setAggressiveMode
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(text = "Protección telefónica")
                        SectionPanel {
                            SettingToggleItem(
                                title = "Bloquear números privados",
                                subtitle = "Rechaza llamadas con identificador oculto",
                                checked = blockPrivateCalls,
                                onCheckedChange = viewModel::setBlockPrivateCalls
                            )

                            PanelDivider()

                            SettingToggleItem(
                                title = "Bloquear numeración inválida",
                                subtitle = "Filtra orígenes que no cumplen el formato telefónico",
                                checked = blockInvalidCalls,
                                onCheckedChange = viewModel::setBlockInvalidCalls
                            )

                            PanelDivider()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Política de llamadas internacionales",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Acción aplicada a numeración procedente de otros países",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.surfaceRecessed, RoundedCornerShape(6.dp))
                                        .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    InternationalPolicyOption(
                                        label = "Permitir",
                                        selected = internationalPolicy == SecuritySettingsRepository.InternationalCallPolicy.ALLOW,
                                        onClick = {
                                            viewModel.setInternationalCallPolicy(
                                                SecuritySettingsRepository.InternationalCallPolicy.ALLOW
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    InternationalPolicyOption(
                                        label = "Sospechosas",
                                        selected = internationalPolicy == SecuritySettingsRepository.InternationalCallPolicy.BLOCK_SUSPICIOUS,
                                        onClick = {
                                            viewModel.setInternationalCallPolicy(
                                                SecuritySettingsRepository.InternationalCallPolicy.BLOCK_SUSPICIOUS
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    InternationalPolicyOption(
                                        label = "Bloquear todas",
                                        selected = internationalPolicy == SecuritySettingsRepository.InternationalCallPolicy.BLOCK_ALL,
                                        onClick = {
                                            viewModel.setInternationalCallPolicy(
                                                SecuritySettingsRepository.InternationalCallPolicy.BLOCK_ALL
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(text = "Listas e inteligencia")
                        SectionPanel {
                            NavigationSettingItem(
                                icon = Icons.Outlined.VerifiedUser,
                                title = "Lista blanca de remitentes",
                                subtitle = "Contactos y números exentos de filtrado",
                                countText = "${whitelist.size}",
                                onClick = onNavigateToWhitelist
                            )

                            PanelDivider()

                            NavigationSettingItem(
                                icon = Icons.AutoMirrored.Outlined.Rule,
                                title = "Reglas personalizadas",
                                subtitle = "Palabras clave y patrones Regex propios",
                                countText = "$activeCustomRulesCount/${customRules.size}",
                                onClick = onNavigateToCustomRules
                            )

                            PanelDivider()

                            NavigationSettingItem(
                                icon = Icons.Outlined.Public,
                                title = "Lista negra comunitaria",
                                subtitle = "Indicadores compartidos sin datos personales",
                                countText = "${communityEntries.size}",
                                onClick = onNavigateToCommunityBlacklist
                            )

                            PanelDivider()

                            SettingToggleItem(
                                title = "Sincronización comunitaria",
                                subtitle = "Sincroniza indicadores normalizados en segundo plano",
                                checked = communitySyncEnabled,
                                onCheckedChange = viewModel::setCommunitySyncEnabled
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(text = "Auditoría de reportes (${userReports.size})")
                        SectionPanel {
                            if (userReports.isEmpty()) {
                                Text(
                                    text = "No hay correcciones de clasificación registradas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                                )
                            } else {
                                PanelHeaderBand(
                                    title = "Corrección · Huella SHA",
                                    trailing = {
                                        Text(
                                            text = "ACCIÓN",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textMuted
                                        )
                                    }
                                )
                                userReports.forEachIndexed { index, report ->
                                    if (index > 0) {
                                        PanelDivider()
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = report.assignedLevel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = colors.textSecondary
                                                )
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                                    contentDescription = null,
                                                    tint = colors.textMuted,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = report.userLevel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textPrimary
                                                )
                                            }
                                            Text(
                                                text = "ID: ${report.messageHash.take(14)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = colors.textMuted
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteUserReport(report.id) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "Borrar reporte",
                                                tint = colors.danger,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            PanelDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceSubtle)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showOriginalExportDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 38.dp),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, colors.borderStrong),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.surface,
                                        contentColor = colors.textPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FileDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Exportar CSV", style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = {
                                        userReports.forEach { viewModel.deleteUserReport(it.id) }
                                    },
                                    enabled = userReports.isNotEmpty(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 38.dp),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, colors.border),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.surface,
                                        contentColor = colors.danger
                                    )
                                ) {
                                    Text("Limpiar lista", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(text = "Almacenamiento y privacidad")
                        SectionPanel {
                            NavigationSettingItem(
                                icon = Icons.Outlined.PrivacyTip,
                                title = "Privacidad y retención de datos",
                                subtitle = "Arquitectura de cifrado local y ciclo de purga",
                                countText = null,
                                onClick = { showPrivacyDialog = true }
                            )

                            PanelDivider()

                            MaintenanceActionItem(
                                icon = Icons.Outlined.CleaningServices,
                                title = "Vaciar bóveda de cuarentena",
                                subtitle = "Elimina todos los mensajes actualmente aislados",
                                tint = colors.warning,
                                onClick = { clearDialogType = "VAULT" }
                            )

                            PanelDivider()

                            MaintenanceActionItem(
                                icon = Icons.Outlined.DeleteSweep,
                                title = "Restablecer registros locales",
                                subtitle = "Borra contadores e historial de SMS y llamadas",
                                tint = colors.danger,
                                onClick = { clearDialogType = "ALL" }
                            )
                        }
                    }
                }

                item {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = colors.surfaceSubtle,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = colors.safe,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Inspección 100% en el dispositivo · Purga automática cada 30 días",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    val dialogType = clearDialogType
    if (dialogType != null) {
        val isVault = dialogType == "VAULT"
        AlertDialog(
            onDismissRequest = { clearDialogType = null },
            title = {
                Text(
                    text = if (isVault) "Vaciar cuarentena" else "Borrar historial completo",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = if (isVault) {
                        "Se eliminarán permanentemente todos los mensajes almacenados en cuarentena."
                    } else {
                        "Se eliminarán todos los registros locales de mensajes y llamadas analizados."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isVault) {
                            viewModel.clearQuarantined()
                        } else {
                            viewModel.clearAllRecords()
                        }
                        clearDialogType = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.danger,
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.heightIn(min = SmishGuardDimensions.minTouchTarget)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { clearDialogType = null }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = MaterialTheme.shapes.large
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text(
                    text = "Privacidad y datos",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "El texto de los SMS se analiza en este dispositivo. " +
                        "Los registros se guardan en el almacenamiento privado de la app. " +
                        "Los mensajes en cuarentena se conservan hasta que los borres o hasta " +
                        "la purga configurada; los demás registros se purgan automáticamente " +
                        "después de 30 días. La sincronización comunitaria está desactivada " +
                        "hasta que la actives y no envía el texto completo del SMS.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Cerrar", color = colors.accent)
                }
            },
            containerColor = colors.surface,
            shape = MaterialTheme.shapes.large
        )
    }

    if (showOriginalExportDialog) {
        AlertDialog(
            onDismissRequest = { showOriginalExportDialog = false },
            title = {
                Text(
                    text = "Exportar reportes",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Puedes exportar las huellas sin texto. El texto original solo se incluirá " +
                        "si lo conservaste con consentimiento al crear el reporte.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOriginalExportDialog = false
                        viewModel.exportUserReports(false) {
                            exportContent = it
                            exportLauncher.launch("smishguard-reportes.csv")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Solo huellas")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showOriginalExportDialog = false
                        viewModel.exportUserReports(true) {
                            exportContent = it
                            exportLauncher.launch("smishguard-reportes-con-texto.csv")
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, colors.borderStrong)
                ) {
                    Text("Incluir texto", color = colors.textPrimary)
                }
            },
            containerColor = colors.surface,
            shape = MaterialTheme.shapes.large
        )
    }
}

@Composable
private fun SensitivityZoneChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    Box(
        modifier = modifier
            .background(
                color = if (active) colors.accentContainer else colors.surfaceRecessed,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = if (active) colors.accent.copy(alpha = 0.45f) else colors.border,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) colors.onAccentContainer else colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InternationalPolicyOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    Box(
        modifier = modifier
            .background(
                color = if (selected) colors.surface else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .then(
                if (selected) {
                    Modifier.border(1.dp, colors.borderStrong, RoundedCornerShape(4.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) colors.accent else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = SmishGuardTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = smishGuardSwitchColors()
        )
    }
}

@Composable
private fun NavigationSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    countText: String?,
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
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(colors.surfaceRecessed, RoundedCornerShape(6.dp))
                .border(1.dp, colors.border, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (countText != null) {
            StatusTag(text = countText, tone = SemanticTone.NEUTRAL)
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MaintenanceActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
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
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(colors.surfaceRecessed, RoundedCornerShape(6.dp))
                .border(1.dp, colors.border, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = tint
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}
