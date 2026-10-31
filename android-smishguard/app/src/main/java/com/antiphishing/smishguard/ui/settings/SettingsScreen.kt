package com.antiphishing.smishguard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.ScreenHeader
import com.antiphishing.smishguard.ui.components.SectionLabel
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.SemanticTone
import com.antiphishing.smishguard.ui.components.StatusTag
import com.antiphishing.smishguard.ui.components.smishGuardSwitchColors
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
    val whitelist by viewModel.whitelist.collectAsState()
    val customRules by viewModel.customRules.collectAsState()
    val communityEntries by viewModel.communityEntries.collectAsState()

    val colors = SmishGuardTheme.colors
    var sliderValue by remember(threshold) { mutableFloatStateOf(threshold.toFloat()) }
    var clearDialogType by remember { mutableStateOf<String?>(null) }

    val displayThreshold = sliderValue.roundToInt()
    val sensitivityProfile = when {
        displayThreshold <= 35 -> "Estricto"
        displayThreshold <= 55 -> "Equilibrado"
        else -> "Conservador"
    }

    val activeCustomRulesCount = remember(customRules) {
        customRules.count { it.isEnabled }
    }

    ResponsiveContentBox {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Ajustes",
                    subtitle = "Parámetros de detección, listas y almacenamiento"
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(text = "Motor de detección")
                    SectionPanel {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                StatusTag(
                                    text = "$displayThreshold% · $sensitivityProfile",
                                    tone = SemanticTone.ACCENT
                                )
                            }

                            Text(
                                text = "Aísla mensajes con riesgo igual o superior al $displayThreshold%.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )

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
                                    inactiveTrackColor = colors.surfaceSubtle,
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                )
                            )
                        }

                        PanelDivider()

                        SettingToggleItem(
                            title = "Suprimir notificaciones de phishing",
                            subtitle = "Cancela el aviso de mensajería cuando el SMS es puesto en cuarentena.",
                            checked = silentDismiss,
                            onCheckedChange = viewModel::setSilentDismissNotif
                        )

                        PanelDivider()

                        SettingToggleItem(
                            title = "Heurística estricta",
                            subtitle = "Incrementa la penalización en dominios recientes y acortadores.",
                            checked = aggressiveMode,
                            onCheckedChange = viewModel::setAggressiveMode
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(text = "Listas y reglas")
                    SectionPanel {
                        NavigationSettingItem(
                            icon = Icons.Outlined.VerifiedUser,
                            title = "Lista blanca de remitentes",
                            subtitle = "Contactos y números exentos de bloqueo",
                            countText = if (whitelist.isNotEmpty()) "${whitelist.size}" else null,
                            onClick = onNavigateToWhitelist
                        )

                        PanelDivider()

                        NavigationSettingItem(
                            icon = Icons.AutoMirrored.Outlined.Rule,
                            title = "Reglas personalizadas",
                            subtitle = "Palabras clave y expresiones regulares propias",
                            countText = if (customRules.isNotEmpty()) "$activeCustomRulesCount/${customRules.size}" else null,
                            onClick = onNavigateToCustomRules
                        )

                        PanelDivider()

                        NavigationSettingItem(
                            icon = Icons.Outlined.Public,
                            title = "Lista negra comunitaria",
                            subtitle = "Indicadores reportados sin exponer datos personales",
                            countText = if (communityEntries.isNotEmpty()) "${communityEntries.size}" else null,
                            onClick = onNavigateToCommunityBlacklist
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(text = "Datos locales")
                    SectionPanel {
                        MaintenanceActionItem(
                            icon = Icons.Outlined.CleaningServices,
                            title = "Vaciar cuarentena",
                            subtitle = "Elimina los mensajes actualmente aislados",
                            tint = colors.warning,
                            onClick = { clearDialogType = "VAULT" }
                        )

                        PanelDivider()

                        MaintenanceActionItem(
                            icon = Icons.Outlined.DeleteSweep,
                            title = "Borrar historial completo",
                            subtitle = "Elimina contadores y registros locales de SMS y llamadas",
                            tint = colors.danger,
                            onClick = { clearDialogType = "ALL" }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Procesamiento 100% local en el dispositivo · Purga automática cada 30 días",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
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
                    shape = RoundedCornerShape(8.dp)
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
            shape = RoundedCornerShape(12.dp)
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(21.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
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
                color = colors.textSecondary
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
