package com.antiphishing.smishguard.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antiphishing.smishguard.ui.components.EmptyStateView
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.components.PanelHeaderBand
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.SectionLabel
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.SemanticTone
import com.antiphishing.smishguard.ui.components.StatusTag
import com.antiphishing.smishguard.ui.components.smishGuardTextFieldColors
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme
import java.util.Locale

@Composable
fun CommunityBlacklistScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val entradas by viewModel.communityEntries.collectAsState()
    val colors = SmishGuardTheme.colors
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var valor by remember { mutableStateOf("") }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.sincronizarComunidad {}
    }

    fun enviar() {
        val cleanValue = valor.trim()
        focusManager.clearFocus()
        viewModel.reportarEnComunidad(cleanValue) { mensaje ->
            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
        }
        valor = ""
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Atrás",
                            tint = colors.textPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Lista negra comunitaria",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.textPrimary
                            )
                            StatusTag(
                                text = "${entradas.size}",
                                tone = SemanticTone.NEUTRAL
                            )
                        }
                        Text(
                            text = "Inteligencia colectiva de dominios, enlaces y teléfonos",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = colors.border)
            }
        },
        containerColor = colors.background
    ) { padding ->
        ResponsiveContentBox(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SectionPanel {
                        PanelHeaderBand(
                            title = "Reportar indicador malicioso",
                            trailing = {
                                StatusTag(
                                    text = "UMBRAL 5/5",
                                    tone = SemanticTone.ACCENT
                                )
                            }
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Solo se transmite el indicador normalizado (sin texto de SMS ni datos personales).",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )

                            OutlinedTextField(
                                value = valor,
                                onValueChange = { valor = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Dominio, URL o número telefónico") },
                                placeholder = { Text("Ej: banco-falso.xyz o +573001112233") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                colors = smishGuardTextFieldColors()
                            )

                            Button(
                                onClick = {
                                    if (viewModel.debeConfirmarReporte()) {
                                        mostrarConfirmacion = true
                                    } else {
                                        enviar()
                                    }
                                },
                                enabled = valor.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 42.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.onAccent
                                ),
                                shape = MaterialTheme.shapes.small,
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Añadir y reportar",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            TextButton(
                                onClick = {
                                    viewModel.sincronizarComunidad { mensaje ->
                                        Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Sincronizar ahora",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.accent
                                )
                            }
                        }
                    }
                }

                item {
                    SectionLabel(text = "Indicadores en caché (${entradas.size})")
                }

                if (entradas.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.Public,
                            title = "Sin indicadores almacenados",
                            subtitle = "Los dominios, enlaces o teléfonos reportados aparecerán aquí."
                        )
                    }
                } else {
                    item {
                        SectionPanel {
                            PanelHeaderBand(
                                title = "Indicador · Tipo",
                                trailing = {
                                    Text(
                                        text = "CONFIRMACIONES",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                            )
                            entradas.forEachIndexed { index, entrada ->
                                if (index > 0) {
                                    PanelDivider()
                                }
                                val edgeColor = when {
                                    entrada.esMalicioso -> colors.danger
                                    entrada.pendienteSincronizacion -> colors.accent
                                    else -> colors.warning
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
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
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = entrada.indicador,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                            val tipoLabel = entrada.tipo.uppercase(Locale.getDefault())
                                            val syncSuffix = if (entrada.pendienteSincronizacion) " · En cola local" else ""
                                            Text(
                                                text = "$tipoLabel · ${entrada.reportes} reporte(s)$syncSuffix",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textMuted
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            StatusTag(
                                                text = when {
                                                    entrada.esMalicioso -> "MALICIOSO"
                                                    entrada.pendienteSincronizacion -> "LOCAL"
                                                    else -> "${entrada.reportes}/5"
                                                },
                                                tone = when {
                                                    entrada.esMalicioso -> SemanticTone.DANGER
                                                    entrada.pendienteSincronizacion -> SemanticTone.ACCENT
                                                    else -> SemanticTone.WARNING
                                                }
                                            )
                                            ConfirmationStepsBar(
                                                count = entrada.reportes,
                                                isMalicious = entrada.esMalicioso
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

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = {
                Text(
                    text = "Confirmar reporte comunitario",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Se enviará únicamente el indicador normalizado de esta entrada. No se comparte ningún mensaje SMS, contacto ni identificador del dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacion = false
                        viewModel.confirmarReporteMostrado()
                        enviar()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = MaterialTheme.shapes.large
        )
    }
}

@Composable
private fun ConfirmationStepsBar(
    count: Int,
    isMalicious: Boolean
) {
    val colors = SmishGuardTheme.colors
    val filled = if (isMalicious) 5 else count.coerceIn(0, 5)
    val activeColor = if (isMalicious || filled >= 5) colors.danger else colors.warning
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(4.dp)
                    .background(
                        color = if (i <= filled) activeColor else colors.surfaceRecessed,
                        shape = RoundedCornerShape(1.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = if (i <= filled) activeColor else colors.border,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}
