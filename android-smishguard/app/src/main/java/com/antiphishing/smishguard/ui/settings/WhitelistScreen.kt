package com.antiphishing.smishguard.ui.settings

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.VerifiedUser
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antiphishing.smishguard.ui.components.EmptyStateView
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.smishGuardTextFieldColors
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme

@OptIn(ExperimentalMaterial3Api::class)
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
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Lista blanca",
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
                    actions = {
                        TextButton(onClick = { showAddDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Añadir",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.accent
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Los números o identificadores registrados aquí quedan exentos de bloqueo y cuarentena.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                if (whitelist.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.VerifiedUser,
                            title = "Sin remitentes de confianza",
                            subtitle = "Usa «Añadir» para autorizar números o remitentes verificados."
                        )
                    }
                } else {
                    item {
                        SectionPanel {
                            whitelist.forEachIndexed { index, entry ->
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
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = entry.senderOrNumber,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
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
                                    IconButton(
                                        onClick = { viewModel.removeWhitelistEntry(entry.senderOrNumber) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Eliminar",
                                            tint = colors.danger,
                                            modifier = Modifier.size(20.dp)
                                        )
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
                            label = { Text("Número o identificador") },
                            placeholder = { Text("Ej: +573001234567 o 890001") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = smishGuardTextFieldColors()
                        )
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Etiqueta (opcional)") },
                            placeholder = { Text("Ej: Contacto familiar") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
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
                        shape = RoundedCornerShape(8.dp)
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
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
