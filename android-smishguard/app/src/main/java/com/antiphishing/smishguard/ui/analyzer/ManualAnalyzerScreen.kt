package com.antiphishing.smishguard.ui.analyzer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antiphishing.smishguard.domain.model.AnalysisResult
import com.antiphishing.smishguard.domain.model.ThreatLevel
import com.antiphishing.smishguard.ui.block.BlockedUrlScreen
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.components.PanelHeaderBand
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.RiskScaleBar
import com.antiphishing.smishguard.ui.components.SectionLabel
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.SemanticTone
import com.antiphishing.smishguard.ui.components.StatusTag
import com.antiphishing.smishguard.ui.components.ThreatFactorRow
import com.antiphishing.smishguard.ui.components.ThreatStatusBadge
import com.antiphishing.smishguard.ui.components.TopScreenBand
import com.antiphishing.smishguard.ui.components.smishGuardTextFieldColors
import com.antiphishing.smishguard.ui.theme.SmishGuardDimensions
import com.antiphishing.smishguard.ui.theme.SmishGuardSpacing
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme

@Composable
fun ManualAnalyzerScreen(
    viewModel: ManualAnalyzerViewModel
) {
    val sender by viewModel.senderInput.collectAsState()
    val body by viewModel.bodyInput.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val blockedDecision by viewModel.blockedDecision.collectAsState()
    val safeUrlToOpen by viewModel.safeUrlToOpen.collectAsState()
    val reportMessage by viewModel.reportMessage.collectAsState()
    val canUndoReport by viewModel.canUndoReport.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val colors = SmishGuardTheme.colors

    LaunchedEffect(reportMessage) {
        if (reportMessage != null) {
            Toast.makeText(context, reportMessage, Toast.LENGTH_SHORT).show()
            viewModel.consumeReportMessage()
        }
    }

    val currentBlocked = blockedDecision
    if (currentBlocked != null) {
        BlockedUrlScreen(
            decision = currentBlocked,
            onBack = viewModel::dismissBlockedDecision
        )
        return
    }

    LaunchedEffect(safeUrlToOpen) {
        val url = safeUrlToOpen
        if (url != null) {
            val normalized = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "http://$url"
            } else {
                url
            }
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized))
                context.startActivity(browserIntent)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "No se encontró un navegador para abrir el enlace",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                viewModel.consumeSafeUrl()
            }
        }
    }

    ResponsiveContentBox {
        Column(modifier = Modifier.fillMaxSize()) {
            TopScreenBand(
                title = "Inspector de amenazas",
                subtitle = "Evaluación heurística de SMS, remitentes y enlaces",
                trailing = {
                    OutlinedButton(
                        onClick = {
                            val clip = clipboardManager.getText()
                            if (clip != null && clip.text.isNotBlank()) {
                                viewModel.onBodyChange(clip.text)
                                focusManager.clearFocus()
                                viewModel.analyze()
                            } else {
                                Toast.makeText(
                                    context,
                                    "El portapapeles está vacío",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, colors.borderStrong),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.surfaceSubtle,
                            contentColor = colors.accent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Pegar",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (canUndoReport) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = colors.accentContainer,
                            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reporte de clasificación guardado localmente",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onAccentContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = viewModel::undoLastReport,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.heightIn(min = 32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Deshacer",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accent
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(text = "Escenarios de prueba")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(
                                items = viewModel.presets,
                                key = { it.title }
                            ) { preset ->
                                val isSelected = sender == preset.sender && body == preset.text
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) colors.accentContainer else colors.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) colors.accent.copy(alpha = 0.5f) else colors.border
                                    ),
                                    modifier = Modifier.clickable {
                                        focusManager.clearFocus()
                                        viewModel.loadPreset(preset)
                                    }
                                ) {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) colors.onAccentContainer else colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SectionPanel {
                        PanelHeaderBand(
                            title = "Parámetros de entrada",
                            trailing = {
                                if (body.isNotEmpty()) {
                                    Text(
                                        text = "${body.length} caracteres",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = sender,
                                onValueChange = viewModel::onSenderChange,
                                label = { Text("Remitente o identificador (opcional)") },
                                placeholder = { Text("Ej: Bancolombia, Nequi o +573001234567") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                colors = smishGuardTextFieldColors()
                            )

                            OutlinedTextField(
                                value = body,
                                onValueChange = viewModel::onBodyChange,
                                label = { Text("Contenido del SMS o dirección URL") },
                                placeholder = { Text("Introduce el texto sospechoso o enlace web...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 6,
                                shape = MaterialTheme.shapes.small,
                                colors = smishGuardTextFieldColors()
                            )
                        }

                        PanelDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceSubtle)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (sender.isNotBlank() || body.isNotBlank() || analysisResult != null) {
                                OutlinedButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.clear()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 42.dp),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, colors.borderStrong),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.surface,
                                        contentColor = colors.textSecondary
                                    )
                                ) {
                                    Text("Limpiar", style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.analyze()
                                },
                                modifier = Modifier
                                    .weight(2f)
                                    .heightIn(min = 42.dp),
                                enabled = body.isNotBlank() && !isAnalyzing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.onAccent
                                ),
                                shape = MaterialTheme.shapes.small,
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.onAccent
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Policy,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isAnalyzing) "Analizando..." else "Ejecutar análisis",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                val currentResult = analysisResult
                if (currentResult != null) {
                    item {
                        AnalysisResultPanel(
                            result = currentResult,
                            onOpenUrl = viewModel::attemptOpenUrl,
                            onReport = viewModel::reportClassification
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassificationReportChip(
    text: String,
    icon: ImageVector,
    contentColor: Color,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AnalysisResultPanel(
    result: AnalysisResult,
    onOpenUrl: (String) -> Unit,
    onReport: (ThreatLevel, Boolean) -> Unit
) {
    val colors = SmishGuardTheme.colors
    val clipboardManager = LocalClipboardManager.current
    var copied by remember(result) { mutableStateOf(false) }
    var showFullUrls by remember(result) { mutableStateOf(false) }
    var keepOriginalForExport by remember(result) { mutableStateOf(false) }

    val edgeColor = when (result.level) {
        ThreatLevel.PHISHING -> colors.danger
        ThreatLevel.SUSPICIOUS -> colors.warning
        ThreatLevel.SAFE -> colors.safe
    }
    val categoryLabel = when (result.categoriaMensaje) {
        "SMISHING" -> "SMISHING"
        "SPAM" -> "SPAM COMERCIAL"
        else -> "LEGÍTIMO"
    }

    SectionPanel(accentEdgeColor = edgeColor) {
        PanelHeaderBand(
            title = "Dictamen técnico",
            trailing = {
                StatusTag(
                    text = categoryLabel,
                    tone = when (result.categoriaMensaje) {
                        "SMISHING" -> SemanticTone.DANGER
                        "SPAM" -> SemanticTone.WARNING
                        else -> SemanticTone.SAFE
                    }
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${result.threatScore}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = edgeColor
                    )
                    Text(
                        text = "/ 100 RIESGO",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textMuted
                    )
                }

                ThreatStatusBadge(level = result.level)
            }

            RiskScaleBar(
                score = result.threatScore,
                level = result.level
            )

            Text(
                text = result.summaryReason,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary
            )

            if (result.homoglyphsDetected.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = colors.warningContainer,
                    border = BorderStroke(1.dp, colors.warningBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = colors.warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Ofuscación detectada: ${result.homoglyphsDetected.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.warning
                        )
                    }
                }
            }

            if (result.extractedUrls.isNotEmpty()) {
                PanelDivider()
                SectionLabel(text = "Enlaces detectados (${result.extractedUrls.size})")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (url in result.extractedUrls) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceRecessed, MaterialTheme.shapes.small)
                                .border(1.dp, colors.border, MaterialTheme.shapes.small)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = url,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    maxLines = if (showFullUrls) Int.MAX_VALUE else 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val isPhishing = result.level == ThreatLevel.PHISHING
                                OutlinedButton(
                                    onClick = { onOpenUrl(url) },
                                    shape = MaterialTheme.shapes.extraSmall,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isPhishing) colors.dangerBorder else colors.borderStrong
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isPhishing) colors.dangerContainer else colors.surface,
                                        contentColor = if (isPhishing) colors.danger else colors.textPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.heightIn(min = 36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPhishing) "Verificar bloqueo" else "Abrir",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            if (url.length > 48) {
                                Text(
                                    text = if (showFullUrls) "Ocultar enlace completo" else "Ver enlace completo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accent,
                                    modifier = Modifier.clickable { showFullUrls = !showFullUrls }
                                )
                            }
                        }
                    }
                }
            }

            if (result.factors.isNotEmpty()) {
                PanelDivider()
                SectionLabel(text = "Factores de riesgo (${result.factors.size})")

                Column(modifier = Modifier.fillMaxWidth()) {
                    result.factors.forEachIndexed { idx, factor ->
                        if (idx > 0) {
                            PanelDivider()
                        }
                        ThreatFactorRow(factor = factor)
                    }
                }
            }
        }

        PanelDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceSubtle)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CORREGIR CLASIFICACIÓN",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "Ajusta el veredicto en este dispositivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }

                OutlinedButton(
                    onClick = {
                        val report = buildString {
                            appendLine("Veredicto: ${result.level.label} (${result.threatScore}/100)")
                            appendLine("Clasificación: $categoryLabel")
                            appendLine("Resumen: ${result.summaryReason}")
                            if (result.extractedUrls.isNotEmpty()) {
                                appendLine("Enlaces: ${result.extractedUrls.joinToString(", ")}")
                            }
                        }
                        clipboardManager.setText(AnnotatedString(report))
                        copied = true
                    },
                    shape = MaterialTheme.shapes.extraSmall,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 34.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.surface,
                        contentColor = if (copied) colors.safe else colors.textPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (copied) "Copiado" else "Copiar dictamen",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ClassificationReportChip(
                    text = "Legítimo",
                    icon = Icons.Outlined.CheckCircle,
                    contentColor = colors.safe,
                    containerColor = colors.safeContainer,
                    borderColor = colors.safeBorder,
                    onClick = { onReport(ThreatLevel.SAFE, keepOriginalForExport) },
                    modifier = Modifier.weight(1f)
                )
                ClassificationReportChip(
                    text = "Sospechoso",
                    icon = Icons.Outlined.WarningAmber,
                    contentColor = colors.warning,
                    containerColor = colors.warningContainer,
                    borderColor = colors.warningBorder,
                    onClick = { onReport(ThreatLevel.SUSPICIOUS, keepOriginalForExport) },
                    modifier = Modifier.weight(1f)
                )
                ClassificationReportChip(
                    text = "Malicioso",
                    icon = Icons.Outlined.GppBad,
                    contentColor = colors.danger,
                    containerColor = colors.dangerContainer,
                    borderColor = colors.dangerBorder,
                    onClick = { onReport(ThreatLevel.PHISHING, keepOriginalForExport) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { keepOriginalForExport = !keepOriginalForExport },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = keepOriginalForExport,
                    onCheckedChange = { keepOriginalForExport = it }
                )
                Text(
                    text = "Conservar texto cifrado localmente para exportación CSV",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
