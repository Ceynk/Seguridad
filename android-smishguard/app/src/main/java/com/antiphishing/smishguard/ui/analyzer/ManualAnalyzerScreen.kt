package com.antiphishing.smishguard.ui.analyzer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antiphishing.smishguard.domain.model.AnalysisResult
import com.antiphishing.smishguard.domain.model.ThreatFactor
import com.antiphishing.smishguard.domain.model.ThreatLevel
import com.antiphishing.smishguard.ui.block.BlockedUrlScreen
import com.antiphishing.smishguard.ui.theme.*

@Composable
fun ManualAnalyzerScreen(
    viewModel: ManualAnalyzerViewModel
) {
    val sender by viewModel.senderInput.collectAsState()
    val body by viewModel.bodyInput.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val blockedDecision by viewModel.blockedDecision.collectAsState()
    val safeUrlToOpen by viewModel.safeUrlToOpen.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentBlocked = blockedDecision
    if (currentBlocked != null) {
        BlockedUrlScreen(
            decision = currentBlocked,
            onBack = { viewModel.dismissBlockedDecision() }
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
            } catch (e: Exception) {
                Toast.makeText(context, "No se encontró un navegador para abrir el enlace", Toast.LENGTH_SHORT).show()
            } finally {
                viewModel.consumeSafeUrl()
            }
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
                    text = "Analizador de Seguridad",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CyberCyan
                )
                Text(
                    text = "Verifica si un mensaje o enlace representa un riesgo de phishing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ejemplos de prueba:",
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
                                Icon(
                                    Icons.Default.BugReport,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = CyberCyan
                                )
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
                        label = { Text("Remitente (Opcional)") },
                        placeholder = { Text("Ej: Bancolombia, Nequi o +573001234567") },
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
                        label = { Text("Mensaje o enlace a analizar") },
                        placeholder = { Text("Pega el texto o URL aquí...") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val clip = clipboardManager.getText()
                                if (clip != null && clip.text.isNotBlank()) {
                                    viewModel.onBodyChange(clip.text)
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analizar")
                        }
                    }
                }
            }
        }

        val currentResult = analysisResult
        if (currentResult != null) {
            item {
                AnalysisResultView(
                    result = currentResult,
                    onOpenUrl = { url -> viewModel.attemptOpenUrl(url) }
                )
            }
        }
    }
}

@Composable
fun AnalysisResultView(
    result: AnalysisResult,
    onOpenUrl: (String) -> Unit
) {
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

    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "Riesgo: ${result.threatScore} / 100",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = levelColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
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
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ENLACES DETECTADOS:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberCyan
                        )

                        for (url in result.extractedUrls) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = url,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onOpenUrl(url) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (result.level == ThreatLevel.PHISHING) ThreatDangerRed else CyberCyan,
                                        contentColor = if (result.level == ThreatLevel.PHISHING) Color.White else DarkBackground
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (result.level == ThreatLevel.PHISHING) "Comprobar y Bloquear" else "Abrir enlace",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (result.factors.isNotEmpty()) {
                HorizontalDivider(color = DarkSurfaceCard)
                Text(
                    text = "Factores identificados (${result.factors.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )

                for (factor in result.factors) {
                    FactorItem(factor = factor)
                }
            }

            OutlinedButton(
                onClick = {
                    val report = buildString {
                        append("Resultado: ").append(result.level.label)
                        append(" (").append(result.threatScore).append("/100)\n")
                        append("Motivo: ").append(result.summaryReason).append("\n")
                        if (result.extractedUrls.isNotEmpty()) {
                            append("Enlaces: ").append(result.extractedUrls.joinToString(", ")).append("\n")
                        }
                    }
                    clipboardManager.setText(AnnotatedString(report))
                    copied = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (copied) "Resultado Copiado" else "Copiar Resultado", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun FactorItem(factor: ThreatFactor) {
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
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = ThreatDangerRed
                )
                Text(
                    text = factor.description,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
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
