package com.antiphishing.smishguard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antiphishing.smishguard.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToCustomRules: () -> Unit
) {
    val threshold by viewModel.quarantineThreshold.collectAsState()
    val aggressiveMode by viewModel.aggressiveMode.collectAsState()
    val silentDismiss by viewModel.silentDismissNotif.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Ajustes de Seguridad",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CyberCyan
                )
                Text(
                    text = "Personaliza el motor de detección y niveles de aislamiento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sensibilidad de Aislamiento",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Umbral: $threshold%",
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }

                    Text(
                        text = "Los SMS con una puntuación de riesgo igual o mayor a $threshold% serán enviados a la Bóveda Honeypot.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Slider(
                        value = threshold.toFloat(),
                        onValueChange = { viewModel.setQuarantineThreshold(it.toInt()) },
                        valueRange = 20f..80f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan
                        )
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingToggleRow(
                        title = "Suprimir Notificaciones de Phishing",
                        subtitle = "Cancela silenciosamente la notificación si el SMS es malicioso.",
                        checked = silentDismiss,
                        onCheckedChange = { viewModel.setSilentDismissNotif(it) }
                    )

                    HorizontalDivider(color = DarkSurfaceCard)

                    SettingToggleRow(
                        title = "Modo Heurístico Agresivo",
                        subtitle = "Aumenta la penalización a dominios nuevos o enlaces acortados desconocidos.",
                        checked = aggressiveMode,
                        onCheckedChange = { viewModel.setAggressiveMode(it) }
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RuleNavCard(
                    title = "Lista Blanca de Remitentes",
                    subtitle = "Contactos y números de confianza que nunca serán bloqueados.",
                    icon = Icons.Default.VerifiedUser,
                    onClick = onNavigateToWhitelist
                )

                RuleNavCard(
                    title = "Reglas Personalizadas (Regex o Palabras)",
                    subtitle = "Añade tus propias palabras clave o patrones de bloqueo.",
                    icon = Icons.AutoMirrored.Filled.Rule,
                    onClick = onNavigateToCustomRules
                )
            }
        }

        item {
            var showClearDialog by remember { mutableStateOf(false) }
            var clearType by remember { mutableStateOf("") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Mantenimiento y Almacenamiento",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )

                    OutlinedButton(
                        onClick = {
                            clearType = "VAULT"
                            showClearDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThreatWarningYellow),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vaciar Bóveda Honeypot")
                    }

                    OutlinedButton(
                        onClick = {
                            clearType = "ALL"
                            showClearDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThreatDangerRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Limpiar Todo el Historial de Análisis")
                    }
                }
            }

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = {
                        Text(
                            text = if (clearType == "VAULT") "Vaciar Bóveda" else "Limpiar Historial",
                            color = TextPrimary
                        )
                    },
                    text = {
                        Text(
                            text = if (clearType == "VAULT")
                                "¿Deseas eliminar permanentemente todos los mensajes aislados en la Bóveda Honeypot?"
                            else
                                "¿Deseas eliminar todos los registros analizados e historial de la base de datos local?",
                            color = TextSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (clearType == "VAULT") viewModel.clearQuarantined()
                                else viewModel.clearAllRecords()
                                showClearDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThreatDangerRed, contentColor = TextPrimary)
                        ) {
                            Text("Confirmar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text("Cancelar", color = TextSecondary)
                        }
                    },
                    containerColor = DarkSurface
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = null,
                        tint = ThreatSafeGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Compromiso de Privacidad Total",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "SmishGuard opera 100% en tu dispositivo. Ningún mensaje, número, enlace o estadística se envía a servidores externos.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
            Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberCyan,
                checkedTrackColor = CyberCyanDark.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun RuleNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}
