package com.antiphishing.smishguard.ui.analyzer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antiphishing.smishguard.domain.model.AnalysisResult
import com.antiphishing.smishguard.domain.model.ThreatLevel
import com.antiphishing.smishguard.ui.block.BlockedUrlScreen
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.ScreenHeader
import com.antiphishing.smishguard.ui.components.SectionLabel
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.SemanticTone
import com.antiphishing.smishguard.ui.components.StatusTag
import com.antiphishing.smishguard.ui.components.ThreatFactorRow
import com.antiphishing.smishguard.ui.components.smishGuardTextFieldColors
import com.antiphishing.smishguard.ui.components.toSemanticTone
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

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val colors = SmishGuardTheme.colors

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeader(
                    title = "Analizador",
                    subtitle = "Evalúa mensajes SMS, remitentes o direcciones web"
                )
            }

            item {
                SectionPanel {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DATOS A VERIFICAR",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )

                            TextButton(
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
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.heightIn(min = 30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Pegar y analizar",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.accent
                                )
                            }
                        }

                        OutlinedTextField(
                            value = sender,
                            onValueChange = viewModel::onSenderChange,
                            label = { Text("Remitente (opcional)") },
                            placeholder = { Text("Ej: Bancolombia, Nequi o +573001234567") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = smishGuardTextFieldColors()
                        )

                        OutlinedTextField(
                            value = body,
                            onValueChange = viewModel::onBodyChange,
                            label = { Text("Mensaje o enlace") },
                            placeholder = { Text("Escribe o pega el texto del SMS o URL...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(8.dp),
                            colors = smishGuardTextFieldColors()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .heightIn(min = 44.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, colors.border),
                                    colors = ButtonDefaults.outlinedButtonColors(
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
                                    .heightIn(min = 44.dp),
                                enabled = body.isNotBlank() && !isAnalyzing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.onAccent
                                ),
                                shape = RoundedCornerShape(8.dp),
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
                                        imageVector = Icons.Outlined.Shield,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isAnalyzing) "Analizando..." else "Analizar",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(text = "Casos de prueba")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            items = viewModel.presets,
                            key = { it.title }
                        ) { preset ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.surface,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.clickable {
                                    focusManager.clearFocus()
                                    viewModel.loadPreset(preset)
                                }
                            ) {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            val currentResult = analysisResult
            if (currentResult != null) {
                item {
                    AnalysisResultPanel(
                        result = currentResult,
                        onOpenUrl = viewModel::attemptOpenUrl
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisResultPanel(
    result: AnalysisResult,
    onOpenUrl: (String) -> Unit
) {
    val colors = SmishGuardTheme.colors
    val clipboardManager = LocalClipboardManager.current
    var copied by remember(result) { mutableStateOf(false) }

    val tone = result.level.toSemanticTone()
    val levelColor = when (result.level) {
        ThreatLevel.PHISHING -> colors.danger
        ThreatLevel.SUSPICIOUS -> colors.warning
        ThreatLevel.SAFE -> colors.safe
    }
    val panelBorder = when (result.level) {
        ThreatLevel.PHISHING -> colors.dangerBorder
        ThreatLevel.SUSPICIOUS -> colors.warningBorder
        ThreatLevel.SAFE -> colors.safeBorder
    }
    val statusIcon = when (result.level) {
        ThreatLevel.PHISHING -> Icons.Outlined.ErrorOutline
        ThreatLevel.SUSPICIOUS -> Icons.Outlined.GppMaybe
        ThreatLevel.SAFE -> Icons.Outlined.CheckCircle
    }
    val categoryLabel = when (result.categoriaMensaje) {
        "SMISHING" -> "Clasificación: Smishing"
        "SPAM" -> "Clasificación: Spam comercial"
        else -> "Clasificación: Legítimo"
    }

    SectionPanel(borderColor = panelBorder) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = when (result.level) {
                                    ThreatLevel.PHISHING -> colors.dangerContainer
                                    ThreatLevel.SUSPICIOUS -> colors.warningContainer
                                    ThreatLevel.SAFE -> colors.safeContainer
                                },
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = levelColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = result.level.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = levelColor
                        )
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }

                StatusTag(
                    text = "${result.threatScore} / 100",
                    tone = tone
                )
            }

            Text(
                text = result.summaryReason,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary
            )

            if (result.homoglyphsDetected.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
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
                            modifier = Modifier.size(17.dp)
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                    .padding(end = 8.dp)
                            )

                            val isPhishing = result.level == ThreatLevel.PHISHING
                            OutlinedButton(
                                onClick = { onOpenUrl(url) },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isPhishing) colors.dangerBorder else colors.border
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isPhishing) colors.dangerContainer else colors.surface,
                                    contentColor = if (isPhishing) colors.danger else colors.textPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPhishing) "Verificar bloqueo" else "Abrir",
                                    style = MaterialTheme.typography.labelMedium
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

            PanelDivider()

            OutlinedButton(
                onClick = {
                    val report = buildString {
                        appendLine("Veredicto: ${result.level.label} (${result.threatScore}/100)")
                        appendLine(categoryLabel)
                        appendLine("Resumen: ${result.summaryReason}")
                        if (result.extractedUrls.isNotEmpty()) {
                            appendLine("Enlaces: ${result.extractedUrls.joinToString(", ")}")
                        }
                    }
                    clipboardManager.setText(AnnotatedString(report))
                    copied = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, colors.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (copied) colors.safe else colors.textPrimary
                )
            ) {
                Icon(
                    imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (copied) "Resultado copiado" else "Copiar dictamen",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
