package com.antiphishing.smishguard.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.VerifiedUser
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
import com.antiphishing.smishguard.ui.components.smishGuardTextFieldColors
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme

@Composable
fun WhitelistScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val whitelist by viewModel.whitelist.collectAsState()
    val colors = SmishGuardTheme.colors

    var showAddDialog by remember { mutableStateOf(false) }
    var senderInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

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
                                    text = "Lista blanca",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.textPrimary
                                )
                                StatusTag(
                                    text = "${whitelist.size}",
                                    tone = SemanticTone.SAFE
                                )
                            }
                            Text(
                                text = "Orígenes exentos de cuarentena y bloqueo",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { showAddDialog = true },
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
                            text = "Añadir",
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
                if (whitelist.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.VerifiedUser,
                            title = "Sin remitentes de confianza",
                            subtitle = "Pulsa «Añadir» para registrar números o identificadores verificados."
                        )
                    }
                } else {
                    item {
                        SectionPanel(accentEdgeColor = colors.safe) {
                            PanelHeaderBand(
                                title = "Identificador autorizado",
                                trailing = {
                                    Text(
                                        text = "ESTADO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                            )
                            whitelist.forEachIndexed { index, entry ->
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
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = entry.senderOrNumber,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        if (!entry.contactName.isNullOrBlank()) {
                                            Text(
                                                text = entry.contactName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        StatusTag(
                                            text = "CONFIABLE",
                                            tone = SemanticTone.SAFE
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeWhitelistEntry(entry.senderOrNumber) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "Eliminar",
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
                        text = "Nuevo remitente de confianza",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = senderInput,
                            onValueChange = { senderInput = it },
                            label = { Text("Número, dominio o identificador") },
                            placeholder = { Text("Ej: +573001234567 o 890001") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            colors = smishGuardTextFieldColors()
                        )
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Etiqueta descriptiva (opcional)") },
                            placeholder = { Text("Ej: Entidad bancaria o familiar") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            colors = smishGuardTextFieldColors()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (senderInput.isNotBlank()) {
                                val cleanSender = senderInput.trim()
                                val cleanName = nameInput.trim().ifEmpty { null }
                                viewModel.addWhitelistEntry(cleanSender, cleanName)
                                senderInput = ""
                                nameInput = ""
                                showAddDialog = false
                            }
                        },
                        enabled = senderInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Guardar")
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
