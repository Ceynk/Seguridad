package com.antiphishing.smishguard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antiphishing.smishguard.ui.components.EmptyStateView
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.components.PanelHeaderBand
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.SemanticTone
import com.antiphishing.smishguard.ui.components.StatusTag
import com.antiphishing.smishguard.ui.components.smishGuardSwitchColors
import com.antiphishing.smishguard.ui.components.smishGuardTextFieldColors
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme

@Composable
fun CustomRulesScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val customRules by viewModel.customRules.collectAsState()
    val colors = SmishGuardTheme.colors

    var showAddDialog by remember { mutableStateOf(false) }
    var patternInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val activeCount = remember(customRules) { customRules.count { it.isEnabled } }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Atrás",
                                tint = colors.textPrimary
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Reglas personalizadas",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.textPrimary
                                )
                                StatusTag(
                                    text = "$activeCount/${customRules.size}",
                                    tone = SemanticTone.ACCENT
                                )
                            }
                            Text(
                                text = "Patrones locales para forzar el aislamiento de SMS",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            validationError = null
                            showAddDialog = true
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .heightIn(min = 36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Crear",
                            style = MaterialTheme.typography.labelLarge
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (customRules.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.AutoMirrored.Outlined.Rule,
                            title = "Sin reglas definidas",
                            subtitle = "Pulsa «Crear» para añadir una frase exacta o expresión regular."
                        )
                    }
                } else {
                    item {
                        SectionPanel(accentEdgeColor = colors.accent) {
                            PanelHeaderBand(
                                title = "Patrón · Tipo",
                                trailing = {
                                    Text(
                                        text = "ESTADO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                            )
                            customRules.forEachIndexed { index, rule ->
                                if (index > 0) {
                                    PanelDivider()
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = rule.pattern,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rule.isEnabled) colors.textPrimary else colors.textMuted
                                            )
                                            StatusTag(
                                                text = if (rule.isRegex) "REGEX" else "TEXTO",
                                                tone = if (rule.isEnabled) SemanticTone.ACCENT else SemanticTone.NEUTRAL
                                            )
                                        }
                                        if (rule.description.isNotBlank()) {
                                            Text(
                                                text = rule.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = rule.isEnabled,
                                            onCheckedChange = { viewModel.toggleCustomRule(rule) },
                                            colors = smishGuardSwitchColors()
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteCustomRule(rule) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "Eliminar regla",
                                                tint = colors.danger,
                                                modifier = Modifier.size(19.dp)
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

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = {
                    Text(
                        text = "Nueva regla de bloqueo",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = patternInput,
                            onValueChange = {
                                patternInput = it
                                validationError = null
                            },
                            label = { Text(if (isRegex) "Expresión regular" else "Palabra o frase") },
                            placeholder = {
                                Text(if (isRegex) "Ej: bono\\s+solidario" else "Ej: premio exclusivo")
                            },
                            singleLine = true,
                            isError = validationError != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            colors = smishGuardTextFieldColors()
                        )

                        val currentError = validationError
                        if (currentError != null) {
                            Text(
                                text = currentError,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.danger
                            )
                        }

                        OutlinedTextField(
                            value = descInput,
                            onValueChange = { descInput = it },
                            label = { Text("Nota descriptiva (opcional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            colors = smishGuardTextFieldColors()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isRegex = !isRegex
                                    validationError = null
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Usar expresión regular (Regex)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Verificado contra expresiones vulnerables (ReDoS)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                            Switch(
                                checked = isRegex,
                                onCheckedChange = {
                                    isRegex = it
                                    validationError = null
                                },
                                colors = smishGuardSwitchColors()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addCustomRule(patternInput, isRegex, descInput) { err ->
                                if (err != null) {
                                    validationError = err
                                } else {
                                    patternInput = ""
                                    descInput = ""
                                    isRegex = false
                                    validationError = null
                                    showAddDialog = false
                                }
                            }
                        },
                        enabled = patternInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Crear regla")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancelar", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface,
                shape = MaterialTheme.shapes.large
            )
        }
    }
}
