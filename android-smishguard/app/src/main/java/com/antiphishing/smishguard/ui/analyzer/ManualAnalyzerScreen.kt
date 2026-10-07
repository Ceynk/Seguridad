package com.antiphishing.smishguard.ui.analyzer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antiphishing.smishguard.domain.model.AnalysisResult
import com.antiphishing.smishguard.domain.model.ThreatLevel
import com.antiphishing.smishguard.ui.quarantine.ThreatFactorItem
import com.antiphishing.smishguard.ui.theme.*

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable
fun ManualAnalyzerScreen(
    viewModel: ManualAnalyzerViewModel
) {
    val sender by viewModel.senderInput.collectAsState()
    val body by viewModel.bodyInput.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val clipboardManager = LocalClipboardManager.current

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
                    text = "Analizador de Smishing",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CyberCyan
                )
                Text(
                    text = "Inspecciona SMS, números o enlaces sospechosos de forma 100% local.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cargar Ejemplos de Prueba:",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.presets) { preset ->
                        AssistChip(
                            onClick = { viewModel.loadPreset(preset) },
                            label = { Text(preset.title, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberCyan)
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DarkSurface,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = sender,
                        onValueChange = { viewModel.onSenderChange(it) },
                        label = { Text("Remitente o Número") },
                        placeholder = { Text("Ej: Banco Santander, Nequi o +573001234567") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkSurfaceCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = body,
                        onValueChange = { viewModel.onBodyChange(it) },
                        label = { Text("Cuerpo del Mensaje SMS") },
                        placeholder = { Text("Pega el texto a analizar aquí...") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val clip = clipboardManager.getText()
                                if (clip != null) {
                                    val text = clip.text
                                    if (text.isNotBlank()) {
                                        viewModel.onBodyChange(text)
                                    }
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Pegar", tint = CyberCyan)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkSurfaceCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clear() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Limpiar")
                        }

                        Button(
                            onClick = { viewModel.analyze() },
                            modifier = Modifier.weight(2f),
                            enabled = body.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkBackground),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analizar Amenaza")
                        }
                    }
                }
            }
        }

        val currentResult = analysisResult
        if (currentResult != null) {
            item {
                AnalysisResultCard(result = currentResult)
            }
        }
    }
}

@Composable
fun AnalysisResultCard(result: AnalysisResult) {
    val levelColor = when (result.level) {
        ThreatLevel.PHISHING -> ThreatDangerRed
        ThreatLevel.SUSPICIOUS -> ThreatWarningYellow
        ThreatLevel.SAFE -> ThreatSafeGreen
    }
    val levelBg = when (result.level) {
        ThreatLevel.PHISHING -> ThreatDangerRedBg
        ThreatLevel.SUSPICIOUS -> Color(0x33FFD600)
        ThreatLevel.SAFE -> ThreatSafeGreenBg
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusIcon = if (result.level == ThreatLevel.SAFE) Icons.Default.CheckCircle else Icons.Default.Warning
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = levelColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = result.level.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = levelColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = levelBg
                ) {
                    Text(
                        text = "Nivel: ${result.threatScore} / 100",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = levelColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Text(
                text = result.summaryReason,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )

            if (result.extractedUrls.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "ENLACES DETECTADOS:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = CyberCyan
                        )
                        for (url in result.extractedUrls) {
                            Text(
                                text = url,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            if (result.factors.isNotEmpty()) {
                HorizontalDivider(color = DarkSurfaceCard)
                Text(
                    text = "Factores de Riesgo Activados (${result.factors.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )

                for (factor in result.factors) {
                    ThreatFactorItem(factor = factor)
                }
            }

            var resultCopied by remember { mutableStateOf(false) }
            val clipboardManager = LocalClipboardManager.current

            OutlinedButton(
                onClick = {
                    val reportBuilder = StringBuilder()
                    reportBuilder.append("=== DICTAMEN FORENSE SMISHGUARD ===\n")
                    reportBuilder.append("Diagnóstico: ").append(result.level.label)
                        .append(" (").append(result.threatScore).append("/100)\n")
                    reportBuilder.append("Motivo: ").append(result.summaryReason).append("\n")
                    if (result.extractedUrls.isNotEmpty()) {
                        reportBuilder.append("Enlaces detectados: ").append(result.extractedUrls.joinToString(", ")).append("\n")
                    }
                    if (result.factors.isNotEmpty()) {
                        reportBuilder.append("Factores activados:\n")
                        for (f in result.factors) {
                            reportBuilder.append(" - [").append(f.category).append("] ")
                                .append(f.description).append(" (+")
                                .append(f.severityScore).append(" pts)\n")
                        }
                    }
                    clipboardManager.setText(AnnotatedString(reportBuilder.toString()))
                    resultCopied = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                val copyIcon = if (resultCopied) Icons.Default.Check else Icons.Default.ContentCopy
                Icon(
                    copyIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (resultCopied) "Dictamen Copiado" else "Copiar Dictamen Forense", fontSize = 12.sp)
            }
        }
    }
}
