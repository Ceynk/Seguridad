package com.antiphishing.smishguard.ui.quarantine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.PhoneDisabled
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antiphishing.smishguard.domain.model.SmsMessageData
import com.antiphishing.smishguard.ui.components.DateTimeFormats
import com.antiphishing.smishguard.ui.components.EmptyStateView
import com.antiphishing.smishguard.ui.components.MonospaceBlock
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.ScreenHeader
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.SemanticTone
import com.antiphishing.smishguard.ui.components.StatusTag
import com.antiphishing.smishguard.ui.components.ThreatFactorRow
import com.antiphishing.smishguard.ui.components.ThreatStatusBadge
import com.antiphishing.smishguard.ui.components.smishGuardTextFieldColors
import com.antiphishing.smishguard.ui.theme.SmishGuardDimensions
import com.antiphishing.smishguard.ui.theme.SmishGuardSpacing
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme

@Composable
fun QuarantineScreen(
    viewModel: QuarantineViewModel,
    initialTab: Int = 0
) {
    val messages by viewModel.quarantinedMessages.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMoreMessages by viewModel.hasMoreMessages.collectAsState()
    val callRecords by viewModel.callRecords.collectAsState()
    val colors = SmishGuardTheme.colors

    var activeTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    LaunchedEffect(initialTab) {
        activeTab = initialTab.coerceIn(0, 1)
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var smsFilter by rememberSaveable { mutableStateOf("ALL") }
    var callFilter by rememberSaveable { mutableStateOf("ALL") }
    var pendingWhitelistSender by remember { mutableStateOf<String?>(null) }

    val filteredMessages = remember(messages, searchQuery, smsFilter) {
        val hasQuery = searchQuery.isNotBlank()
        messages.filter { msg ->
            val matchesQuery = !hasQuery ||
                msg.sender.contains(searchQuery, ignoreCase = true) ||
                msg.body.contains(searchQuery, ignoreCase = true)
            val matchesSeverity = when (smsFilter) {
                "CRITICAL" -> msg.threatScore >= 60
                "SUSPICIOUS" -> msg.threatScore < 60
                else -> true
            }
            matchesQuery && matchesSeverity
        }
    }

    val filteredCalls = remember(callRecords, searchQuery, callFilter) {
        val hasQuery = searchQuery.isNotBlank()
        callRecords.filter { call ->
            val matchesQuery = !hasQuery ||
                call.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                call.summaryReason.contains(searchQuery, ignoreCase = true) ||
                (call.countryOrOrigin?.contains(searchQuery, ignoreCase = true) == true)
            val matchesType = when (callFilter) {
                "BLOCKED" -> call.isBlocked
                "WARN" -> !call.isBlocked
                else -> true
            }
            matchesQuery && matchesType
        }
    }

    val hasActiveTabItems = if (activeTab == 0) messages.isNotEmpty() else callRecords.isNotEmpty()

    ResponsiveContentBox {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Cuarentena",
                    subtitle = "Mensajes aislados y llamadas fraudulentas interceptadas"
                )
            }

            item {
                SegmentedChannelSwitch(
                    selectedIndex = activeTab,
                    smsCount = messages.size,
                    callsCount = callRecords.size,
                    onSelect = { index ->
                        activeTab = index
                        searchQuery = ""
                    }
                )
            }

            if (hasActiveTabItems) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    if (activeTab == 0) "Buscar remitente o contenido..."
                                    else "Buscar número, país o motivo..."
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Clear,
                                            contentDescription = "Limpiar búsqueda",
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = smishGuardTextFieldColors()
                        )

                        if (activeTab == 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuarantineFilterChip(
                                    selected = smsFilter == "ALL",
                                    label = "Todos (${messages.size})",
                                    onClick = { smsFilter = "ALL" }
                                )
                                QuarantineFilterChip(
                                    selected = smsFilter == "CRITICAL",
                                    label = "Críticos ≥60%",
                                    onClick = { smsFilter = "CRITICAL" }
                                )
                                QuarantineFilterChip(
                                    selected = smsFilter == "SUSPICIOUS",
                                    label = "Sospechosos",
                                    onClick = { smsFilter = "SUSPICIOUS" }
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuarantineFilterChip(
                                    selected = callFilter == "ALL",
                                    label = "Todas (${callRecords.size})",
                                    onClick = { callFilter = "ALL" }
                                )
                                QuarantineFilterChip(
                                    selected = callFilter == "BLOCKED",
                                    label = "Bloqueadas",
                                    onClick = { callFilter = "BLOCKED" }
                                )
                                QuarantineFilterChip(
                                    selected = callFilter == "WARN",
                                    label = "Alertas",
                                    onClick = { callFilter = "WARN" }
                                )
                            }
                        }
                    }
                }
            }

            if (activeTab == 0) {
                if (filteredMessages.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.Inbox,
                            title = if (messages.isEmpty()) "Sin mensajes en cuarentena" else "Sin coincidencias",
                            subtitle = if (messages.isEmpty()) {
                                "Los SMS con enlaces neutralizados o suplantación aparecerán aquí."
                            } else {
                                "Ningún mensaje coincide con el filtro actual."
                            }
                        )
                    }
                } else {
                    items(
                        items = filteredMessages,
                        key = { "sms_${it.id}" },
                        contentType = { "sms_item" }
                    ) { message ->
                        QuarantinedMessagePanel(
                            message = message,
                            onRestore = { viewModel.restoreMessage(message.id) },
                            onDelete = { viewModel.deleteMessage(message.id) },
                            onRequestWhitelist = { pendingWhitelistSender = message.sender },
                            onLoadOriginal = { callback ->
                                viewModel.loadOriginalBody(message.id, callback)
                            }
                        )
                    }
                    if (filteredMessages.size == messages.size && hasMoreMessages && !isLoadingMore) {
                        item {
                            TextButton(
                                onClick = viewModel::loadMoreMessages,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cargar más mensajes")
                            }
                        }
                    }
                }
            } else {
                if (filteredCalls.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.PhoneDisabled,
                            title = if (callRecords.isEmpty()) "Sin llamadas registradas" else "Sin coincidencias",
                            subtitle = if (callRecords.isEmpty()) {
                                "Las llamadas bloqueadas o marcadas como sospechosas se listarán aquí."
                            } else {
                                "Ningún registro coincide con la búsqueda."
                            }
                        )
                    }
                } else {
                    items(
                        items = filteredCalls,
                        key = { "call_${it.id}" },
                        contentType = { "call_item" }
                    ) { call ->
                        QuarantinedCallPanel(
                            call = call,
                            onDelete = { viewModel.deleteCallRecord(call.id) },
                            onRequestWhitelist = {
                                if (call.phoneNumber.isNotBlank() && call.phoneNumber != "Número oculto") {
                                    pendingWhitelistSender = call.phoneNumber
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    val senderToTrust = pendingWhitelistSender
    if (senderToTrust != null) {
        AlertDialog(
            onDismissRequest = { pendingWhitelistSender = null },
            title = {
                Text(
                    text = "Confiar en remitente",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Se añadirá «$senderToTrust» a la lista blanca. Los mensajes y llamadas futuros de este origen no serán bloqueados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addSenderToWhitelist(senderToTrust)
                        pendingWhitelistSender = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Añadir a lista blanca")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingWhitelistSender = null }) {
                    Text("Cancelar", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun SegmentedChannelSwitch(
    selectedIndex: Int,
    smsCount: Int,
    callsCount: Int,
    onSelect: (Int) -> Unit
) {
    val colors = SmishGuardTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = colors.surfaceSubtle,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SegmentOption(
                title = "Mensajes ($smsCount)",
                selected = selectedIndex == 0,
                onClick = { onSelect(0) },
                modifier = Modifier.weight(1f)
            )
            SegmentOption(
                title = "Llamadas ($callsCount)",
                selected = selectedIndex == 1,
                onClick = { onSelect(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SegmentOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    Box(
        modifier = modifier
            .background(
                color = if (selected) colors.surface else androidx.compose.ui.graphics.Color.Transparent,
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick)
            .heightIn(min = SmishGuardDimensions.minTouchTarget)
            .padding(horizontal = SmishGuardSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuarantineFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        shape = MaterialTheme.shapes.small,
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.border,
            selectedBorderColor = colors.accent.copy(alpha = 0.4f)
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surface,
            labelColor = colors.textSecondary,
            selectedContainerColor = colors.accentContainer,
            selectedLabelColor = colors.onAccentContainer
        )
    )
}

@Composable
private fun QuarantinedMessagePanel(
    message: SmsMessageData,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onRequestWhitelist: () -> Unit,
    onLoadOriginal: ((String) -> Unit) -> Unit
) {
    val colors = SmishGuardTheme.colors
    val clipboardManager = LocalClipboardManager.current

    var isExpanded by rememberSaveable(message.id) { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }
    var originalText by remember { mutableStateOf<String?>(null) }
    var reportCopied by remember { mutableStateOf(false) }

    SectionPanel {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SmishGuardSpacing.md),
                verticalArrangement = Arrangement.spacedBy(SmishGuardSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = DateTimeFormats.formatFull(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }

                ThreatStatusBadge(level = message.threatLevel)
            }

            MonospaceBlock(
                text = if (showOriginal && originalText != null) originalText!! else message.body,
                label = if (showOriginal && originalText != null) {
                    "TEXTO ORIGINAL (DESCIFRADO LOCAL)"
                } else {
                    "MENSAJE NEUTRALIZADO"
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRestore,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = SmishGuardDimensions.minTouchTarget),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Restaurar", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onRequestWhitelist,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = SmishGuardDimensions.minTouchTarget),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.textSecondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Confiar", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(horizontal = SmishGuardSpacing.sm, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = SmishGuardDimensions.minTouchTarget)
                    ) {
                        Text(
                            text = if (isExpanded) "Ocultar" else "Análisis (${message.threatFactors.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accent
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(SmishGuardDimensions.minTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Eliminar mensaje",
                            tint = colors.danger,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (isExpanded) {
                PanelDivider()

                if (message.threatFactors.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        message.threatFactors.forEachIndexed { idx, factor ->
                            if (idx > 0) {
                                PanelDivider()
                            }
                            ThreatFactorRow(factor = factor)
                        }
                    }
                } else {
                    Text(
                        text = "Aislado por regla o umbral de seguridad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (showOriginal) {
                                showOriginal = false
                            } else if (originalText != null) {
                                showOriginal = true
                            } else {
                                onLoadOriginal { loaded ->
                                    originalText = loaded
                                    showOriginal = true
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SmishGuardDimensions.minTouchTarget),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (showOriginal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showOriginal) "Ver neutralizado" else "Texto original",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val formattedDate = DateTimeFormats.formatFull(message.timestamp)
                            val report = buildString {
                                appendLine("=== REPORTE DE INCIDENTE SMISHGUARD ===")
                                appendLine("Fecha: $formattedDate")
                                appendLine("Remitente: ${message.sender}")
                                appendLine("Riesgo: ${message.threatScore}% (${message.threatLevel.name})")
                                appendLine("Mensaje Neutralizado: ${message.body}")
                                if (message.threatFactors.isNotEmpty()) {
                                    appendLine("Factores:")
                                    for (f in message.threatFactors) {
                                        appendLine(" - [${f.category}] ${f.description} (+${f.severityScore})")
                                    }
                                }
                            }
                            clipboardManager.setText(AnnotatedString(report))
                            reportCopied = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SmishGuardDimensions.minTouchTarget),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.border),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (reportCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (reportCopied) colors.safe else colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (reportCopied) "Copiado" else "Copiar reporte",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (reportCopied) colors.safe else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuarantinedCallPanel(
    call: CallLogUiItem,
    onDelete: () -> Unit,
    onRequestWhitelist: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    var isExpanded by rememberSaveable(call.id) { mutableStateOf(false) }

    SectionPanel {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CallEnd,
                        contentDescription = null,
                        tint = if (call.isBlocked) colors.danger else colors.warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = call.phoneNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        val meta = buildString {
                            append(DateTimeFormats.formatFull(call.timestamp))
                            if (!call.countryOrOrigin.isNullOrBlank()) {
                                append(" · ")
                                append(call.countryOrOrigin)
                            }
                        }
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }

                StatusTag(
                    text = if (call.isBlocked) "Bloqueada · ${call.threatScore}%" else "Alerta · ${call.threatScore}%",
                    tone = if (call.isBlocked) SemanticTone.DANGER else SemanticTone.WARNING
                )
            }

            Text(
                text = call.summaryReason,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (call.phoneNumber != "Número oculto") {
                    OutlinedButton(
                        onClick = onRequestWhitelist,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, colors.border),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Confiar número", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (call.factors.isNotEmpty()) {
                        TextButton(
                            onClick = { isExpanded = !isExpanded },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "Ocultar" else "Factores (${call.factors.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.accent
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Eliminar registro",
                            tint = colors.danger,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (isExpanded && call.factors.isNotEmpty()) {
                PanelDivider()
                Column(modifier = Modifier.fillMaxWidth()) {
                    call.factors.forEachIndexed { idx, factor ->
                        if (idx > 0) {
                            PanelDivider()
                        }
                        ThreatFactorRow(factor = factor)
                    }
                }
            }
        }
    }
}
