package com.antiphishing.smishguard.ui.quarantine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antiphishing.smishguard.domain.model.SmsMessageData
import com.antiphishing.smishguard.domain.model.ThreatLevel
import com.antiphishing.smishguard.ui.components.DateTimeFormats
import com.antiphishing.smishguard.ui.components.EmptyStateView
import com.antiphishing.smishguard.ui.components.MonospaceBlock
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.RiskScaleBar
import com.antiphishing.smishguard.ui.components.SectionLabel
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.SemanticTone
import com.antiphishing.smishguard.ui.components.SeverityBars
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
    LaunchedEffect(Unit) {
        viewModel.ensureInitialized()
    }
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
        Column(modifier = Modifier.fillMaxSize()) {
            VaultControlHeader(
                activeTab = activeTab,
                smsCount = messages.size,
                callsCount = callRecords.size,
                hasActiveTabItems = hasActiveTabItems,
                searchQuery = searchQuery,
                smsFilter = smsFilter,
                callFilter = callFilter,
                onTabSelect = { index ->
                    activeTab = index
                    searchQuery = ""
                },
                onSearchChange = { searchQuery = it },
                onSmsFilterChange = { smsFilter = it },
                onCallFilterChange = { callFilter = it }
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeTab == 0) {
                    if (filteredMessages.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Outlined.Inbox,
                                title = if (messages.isEmpty()) "Sin SMS en cuarentena" else "Sin resultados",
                                subtitle = if (messages.isEmpty()) {
                                    "Los mensajes con enlaces maliciosos o suplantación se aislarán aquí."
                                } else {
                                    "Ningún mensaje coincide con el criterio seleccionado."
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
                                OutlinedButton(
                                    onClick = viewModel::loadMoreMessages,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Text(
                                        text = "Cargar registros anteriores",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.accent
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (filteredCalls.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Outlined.PhoneDisabled,
                                title = if (callRecords.isEmpty()) "Sin llamadas filtradas" else "Sin resultados",
                                subtitle = if (callRecords.isEmpty()) {
                                    "Las llamadas bloqueadas o marcadas como sospechosas aparecerán aquí."
                                } else {
                                    "Ningún registro telefónico coincide con la búsqueda."
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
    }

    val senderToTrust = pendingWhitelistSender
    if (senderToTrust != null) {
        AlertDialog(
            onDismissRequest = { pendingWhitelistSender = null },
            title = {
                Text(
                    text = "Autorizar remitente",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "El siguiente origen se registrará en la lista blanca y no volverá a ser bloqueado:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    MonospaceBlock(
                        text = senderToTrust,
                        label = "ORIGEN DE CONFIANZA"
                    )
                }
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
                    shape = MaterialTheme.shapes.small
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
            shape = MaterialTheme.shapes.large
        )
    }
}

@Composable
private fun VaultControlHeader(
    activeTab: Int,
    smsCount: Int,
    callsCount: Int,
    hasActiveTabItems: Boolean,
    searchQuery: String,
    smsFilter: String,
    callFilter: String,
    onTabSelect: (Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onSmsFilterChange: (String) -> Unit,
    onCallFilterChange: (String) -> Unit
) {
    val colors = SmishGuardTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "Bóveda de cuarentena",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary
                )
                Text(
                    text = "Triaje de mensajes aislados y llamadas interceptadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            StatusTag(
                text = "${smsCount + callsCount} total",
                tone = if (smsCount + callsCount > 0) SemanticTone.WARNING else SemanticTone.NEUTRAL
            )
        }

        HorizontalDivider(thickness = 1.dp, color = colors.divider)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            VaultTabItem(
                icon = Icons.Outlined.Sms,
                title = "SMS aislados",
                count = smsCount,
                selected = activeTab == 0,
                onClick = { onTabSelect(0) },
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(colors.divider)
            )
            VaultTabItem(
                icon = Icons.Outlined.CallEnd,
                title = "Llamadas filtradas",
                count = callsCount,
                selected = activeTab == 1,
                onClick = { onTabSelect(1) },
                modifier = Modifier.weight(1f)
            )
        }

        if (hasActiveTabItems) {
            HorizontalDivider(thickness = 1.dp, color = colors.divider)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceSubtle)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = if (activeTab == 0) "Filtrar por remitente o contenido..."
                            else "Filtrar por número, origen o motivo...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = smishGuardTextFieldColors()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (activeTab == 0) {
                        TriageFilterPill(
                            selected = smsFilter == "ALL",
                            label = "Todos ($smsCount)",
                            onClick = { onSmsFilterChange("ALL") }
                        )
                        TriageFilterPill(
                            selected = smsFilter == "CRITICAL",
                            label = "Críticos ≥60%",
                            onClick = { onSmsFilterChange("CRITICAL") }
                        )
                        TriageFilterPill(
                            selected = smsFilter == "SUSPICIOUS",
                            label = "Sospechosos",
                            onClick = { onSmsFilterChange("SUSPICIOUS") }
                        )
                    } else {
                        TriageFilterPill(
                            selected = callFilter == "ALL",
                            label = "Todas ($callsCount)",
                            onClick = { onCallFilterChange("ALL") }
                        )
                        TriageFilterPill(
                            selected = callFilter == "BLOCKED",
                            label = "Bloqueadas",
                            onClick = { onCallFilterChange("BLOCKED") }
                        )
                        TriageFilterPill(
                            selected = callFilter == "WARN",
                            label = "Alertas",
                            onClick = { onCallFilterChange("WARN") }
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = colors.border)
    }
}

@Composable
private fun VaultTabItem(
    icon: ImageVector,
    title: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (selected) colors.surface else colors.surfaceSubtle)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.accent else colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .background(
                        color = if (selected) colors.accentContainer else colors.surfaceRecessed,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) colors.accent.copy(alpha = 0.3f) else colors.border,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = if (selected) colors.onAccentContainer else colors.textMuted
                )
            }
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(colors.accent)
            )
        }
    }
}

@Composable
private fun TriageFilterPill(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = if (selected) colors.accentContainer else colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) colors.accent.copy(alpha = 0.45f) else colors.border
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.onAccentContainer else colors.textSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
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

    val edgeColor = when (message.threatLevel) {
        ThreatLevel.PHISHING -> colors.danger
        ThreatLevel.SUSPICIOUS -> colors.warning
        ThreatLevel.SAFE -> colors.safe
    }

    SectionPanel(accentEdgeColor = edgeColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = DateTimeFormats.formatFull(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }

                ThreatStatusBadge(
                    level = message.threatLevel,
                    score = message.threatScore
                )
            }

            MonospaceBlock(
                text = if (showOriginal && originalText != null) originalText!! else message.body,
                label = if (showOriginal && originalText != null) {
                    "TEXTO ORIGINAL DESCIFRADO"
                } else {
                    "CONTENIDO NEUTRALIZADO"
                },
                badgeText = if (showOriginal && originalText != null) "DESCIFRADO" else "ENLACES INACTIVOS",
                badgeTone = if (showOriginal && originalText != null) SemanticTone.WARNING else SemanticTone.SAFE
            )
        }

        PanelDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceSubtle)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onRestore,
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.surface,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restaurar", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onRequestWhitelist,
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.surface,
                        contentColor = colors.textSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confiar", style = MaterialTheme.typography.labelMedium)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Eliminar mensaje",
                        tint = colors.danger,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            TextButton(
                onClick = { isExpanded = !isExpanded },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 36.dp)
            ) {
                Text(
                    text = if (isExpanded) "Ocultar" else "Inspección (${message.threatFactors.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (isExpanded) {
            PanelDivider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RiskScaleBar(
                    score = message.threatScore,
                    level = message.threatLevel
                )

                if (message.threatFactors.isNotEmpty()) {
                    SectionLabel(text = "Factores detectados (${message.threatFactors.size})")
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
                        text = "Aislado por regla personalizada o umbral de seguridad.",
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
                            .heightIn(min = 40.dp),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.borderStrong),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (showOriginal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
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
                            .heightIn(min = 40.dp),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.borderStrong),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (reportCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
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
    val edgeColor = if (call.isBlocked) colors.danger else colors.warning

    SectionPanel(accentEdgeColor = edgeColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        text = call.phoneNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusTag(
                        text = if (call.isBlocked) "BLOQUEADA · ${call.threatScore}%" else "ALERTA · ${call.threatScore}%",
                        tone = if (call.isBlocked) SemanticTone.DANGER else SemanticTone.WARNING
                    )
                    SeverityBars(
                        activeBars = if (call.isBlocked) 3 else 2,
                        activeColor = edgeColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceRecessed, RoundedCornerShape(4.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = call.summaryReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary
                )
            }
        }

        PanelDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceSubtle)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (call.phoneNumber != "Número oculto") {
                    OutlinedButton(
                        onClick = onRequestWhitelist,
                        shape = MaterialTheme.shapes.extraSmall,
                        border = BorderStroke(1.dp, colors.borderStrong),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surface,
                            contentColor = colors.textSecondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confiar número", style = MaterialTheme.typography.labelMedium)
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Eliminar registro",
                        tint = colors.danger,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (call.factors.isNotEmpty()) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Ocultar" else "Factores (${call.factors.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (isExpanded && call.factors.isNotEmpty()) {
            PanelDivider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
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
