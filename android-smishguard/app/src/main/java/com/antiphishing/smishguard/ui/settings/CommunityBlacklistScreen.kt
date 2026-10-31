package com.antiphishing.smishguard.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.SectionLabel
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.SemanticTone
import com.antiphishing.smishguard.ui.components.StatusTag
import com.antiphishing.smishguard.ui.components.smishGuardTextFieldColors
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Lista negra comunitaria",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Atrás",
                                tint = colors.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
                )
                PanelDivider()
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Reportar indicador",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Solo se transmite el dominio, enlace o número normalizado (sin contenido del SMS ni datos del teléfono). Con 5 confirmaciones se marca como malicioso.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )

                            OutlinedTextField(
                                value = valor,
                                onValueChange = { valor = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Dominio, URL o teléfono") },
                                placeholder = { Text("Ej: banco-falso.xyz o +573001112233") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
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
                                    .heightIn(min = 44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.onAccent
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Añadir y reportar",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                item {
                    SectionLabel(text = "Indicadores registrados (${entradas.size})")
                }

                if (entradas.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.Public,
                            title = "Sin indicadores en caché",
                            subtitle = "Los dominios, enlaces o números reportados se mostrarán aquí."
                        )
                    }
                } else {
                    item {
                        SectionPanel {
                            entradas.forEachIndexed { index, entrada ->
                                if (index > 0) {
                                    PanelDivider()
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = entrada.indicador,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                        val tipoLabel = entrada.tipo.replaceFirstChar {
                                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                        }
                                        val syncSuffix = if (entrada.pendienteSincronizacion) " · Pendiente de envío" else ""
                                        Text(
                                            text = "$tipoLabel · ${entrada.reportes} reporte(s)$syncSuffix",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textMuted
                                        )
                                    }

                                    StatusTag(
                                        text = when {
                                            entrada.esMalicioso -> "Malicioso"
                                            entrada.pendienteSincronizacion -> "Local"
                                            else -> "${entrada.reportes}/5"
                                        },
                                        tone = when {
                                            entrada.esMalicioso -> SemanticTone.DANGER
                                            entrada.pendienteSincronizacion -> SemanticTone.ACCENT
                                            else -> SemanticTone.WARNING
                                        }
                                    )
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
                    shape = RoundedCornerShape(8.dp)
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
            shape = RoundedCornerShape(12.dp)
        )
    }
}
