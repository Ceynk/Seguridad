package com.antiphishing.smishguard.ui.quarantine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antiphishing.smishguard.domain.model.SmsMessageData
import com.antiphishing.smishguard.domain.model.ThreatFactor
import com.antiphishing.smishguard.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable
fun QuarantineScreen(
    viewModel: QuarantineViewModel
) {
    val messages by viewModel.quarantinedMessages.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredMessages = remember(messages, searchQuery, selectedFilter) {
        val hasSearch = searchQuery.isNotBlank()
        messages.filter { msg ->
            val matchesSearch = !hasSearch ||
                msg.sender.contains(searchQuery, ignoreCase = true) ||
                msg.body.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "CRITICAL" -> msg.threatScore >= 60
                "SUSPICIOUS" -> msg.threatScore < 60
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

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
                    text = "Cuarentena de Mensajes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CyberCyan
                )
                Text(
                    text = "Mensajes sospechosos aislados preventivamente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        if (messages.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por remitente o contenido...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkSurfaceCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("Todos (${messages.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan,
                                selectedLabelColor = DarkBackground,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            )
                        )
                        FilterChip(
                            selected = selectedFilter == "CRITICAL",
                            onClick = { selectedFilter = "CRITICAL" },
                            label = { Text("Críticos >=60%", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThreatDangerRed,
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            )
                        )
                        FilterChip(
                            selected = selectedFilter == "SUSPICIOUS",
                            onClick = { selectedFilter = "SUSPICIOUS" },
                            label = { Text("Sospechosos", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThreatWarningYellow,
                                selectedLabelColor = DarkBackground,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }

        if (filteredMessages.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (messages.isEmpty()) "Bóveda Vacía" else "Sin Coincidencias",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (messages.isEmpty()) "No hay mensajes en cuarentena en este momento."
                            else "No se encontraron mensajes que coincidan con la búsqueda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredMessages, key = { it.id }) { message ->
                QuarantinedMessageCard(
                    message = message,
                    onRestore = { viewModel.restoreMessage(message.id) },
                    onDelete = { viewModel.deleteMessage(message.id) },
                    onWhitelist = { viewModel.addSenderToWhitelist(message.sender) }
                )
            }
        }
    }
}

@Composable
fun QuarantinedMessageCard(
    message: SmsMessageData,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onWhitelist: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val clipboardManager = LocalClipboardManager.current
    var reportCopied by remember { mutableStateOf(false) }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ThreatDangerRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThreatDangerRedBg
                ) {
                    Text(
                        text = "Amenaza: ${message.threatScore}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = ThreatDangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Interceptado: ${dateFormat.format(Date(message.timestamp))}",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "CONTENIDO DEL MENSAJE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }
            }

            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(0.dp)
            ) {
                val buttonText = if (isExpanded) {
                    "Ocultar detalles"
                } else {
                    "Ver factores de riesgo (${message.threatFactors.size})"
                }
                Text(
                    text = buttonText,
                    color = CyberCyan,
                    fontSize = 12.sp
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = DarkSurfaceCard)
                    Text(
                        text = "Veredicto Forense:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )

                    for (factor in message.threatFactors) {
                        ThreatFactorItem(factor = factor)
                    }

                    OutlinedButton(
                        onClick = {
                            val reportBuilder = StringBuilder()
                            reportBuilder.append("=== REPORTE DE INCIDENTE SMISHGUARD ===\n")
                            reportBuilder.append("Fecha: ").append(dateFormat.format(Date(message.timestamp))).append("\n")
                            reportBuilder.append("Remitente: ").append(message.sender).append("\n")
                            reportBuilder.append("Puntuación de Amenaza: ").append(message.threatScore).append("% (").append(message.threatLevel.name).append(")\n")
                            reportBuilder.append("Mensaje Neutralizado: ").append(message.body).append("\n")
                            if (message.threatFactors.isNotEmpty()) {
                                reportBuilder.append("Factores Detectados:\n")
                                for (f in message.threatFactors) {
                                    reportBuilder.append(" - [").append(f.category).append("] ")
                                        .append(f.description).append(" (+")
                                        .append(f.severityScore).append(" pts)\n")
                                }
                            }
                            clipboardManager.setText(AnnotatedString(reportBuilder.toString()))
                            reportCopied = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        val iconVector = if (reportCopied) Icons.Default.Check else Icons.Default.ContentCopy
                        Icon(
                            iconVector,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (reportCopied) "Reporte Copiado" else "Copiar Reporte Forense", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ThreatDangerRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onWhitelist,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ThreatSafeGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lista Blanca", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onRestore,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkBackground),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restaurar", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreatFactorItem(factor: ThreatFactor) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = factor.category,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ThreatDangerRed
                )
                Text(
                    text = factor.description,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                if (factor.matchedSnippet != null) {
                    Text(
                        text = "Detectado: \"${factor.matchedSnippet}\"",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ThreatDangerRedBg
            ) {
                Text(
                    text = "+${factor.severityScore}",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = ThreatDangerRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
