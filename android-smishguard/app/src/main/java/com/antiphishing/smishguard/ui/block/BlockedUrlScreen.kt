package com.antiphishing.smishguard.ui.block

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antiphishing.smishguard.domain.detector.UrlDecision
import com.antiphishing.smishguard.domain.detector.UrlThreatAnalyzer
import com.antiphishing.smishguard.domain.model.ThreatLevel
import com.antiphishing.smishguard.ui.components.MonospaceBlock
import com.antiphishing.smishguard.ui.components.PanelDivider
import com.antiphishing.smishguard.ui.components.PanelHeaderBand
import com.antiphishing.smishguard.ui.components.ResponsiveContentBox
import com.antiphishing.smishguard.ui.components.RiskScaleBar
import com.antiphishing.smishguard.ui.components.SectionLabel
import com.antiphishing.smishguard.ui.components.SectionPanel
import com.antiphishing.smishguard.ui.components.SemanticTone
import com.antiphishing.smishguard.ui.components.StatusTag
import com.antiphishing.smishguard.ui.components.ThreatFactorRow
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme

@Composable
fun BlockedUrlScreen(
    decision: UrlDecision,
    onBack: () -> Unit
) {
    val colors = SmishGuardTheme.colors
    val targetDomain = remember(decision.url) {
        UrlThreatAnalyzer.extraerDominioDeEnlace(decision.url).ifBlank { decision.url }
    }

    ResponsiveContentBox {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(colors.danger)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(colors.dangerContainer, RoundedCornerShape(6.dp))
                            .border(1.dp, colors.dangerBorder, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GppBad,
                            contentDescription = "Acceso bloqueado",
                            tint = colors.danger,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Navegación bloqueada",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.danger
                            )
                            StatusTag(
                                text = "CONTENCIÓN",
                                tone = SemanticTone.DANGER
                            )
                        }
                        Text(
                            text = "El destino presenta indicadores activos de suplantación o fraude.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = colors.border)
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SectionPanel(
                        borderColor = colors.dangerBorder,
                        accentEdgeColor = colors.danger
                    ) {
                        PanelHeaderBand(
                            title = "Análisis del destino interceptado",
                            trailing = {
                                StatusTag(
                                    text = "${decision.threatScore}% · RIESGO ALTO",
                                    tone = SemanticTone.DANGER
                                )
                            }
                        )

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
                                    text = "DOMINIO IDENTIFICADO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = targetDomain,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.danger
                                )
                            }

                            MonospaceBlock(
                                text = decision.url,
                                label = "DIRECCIÓN COMPLETA",
                                badgeText = "BLOQUEADO",
                                badgeTone = SemanticTone.DANGER
                            )

                            RiskScaleBar(
                                score = decision.threatScore,
                                level = ThreatLevel.PHISHING
                            )

                            if (decision.summaryReason.isNotBlank()) {
                                Text(
                                    text = decision.summaryReason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }

                if (decision.factors.isNotEmpty()) {
                    item {
                        SectionLabel(text = "Reglas activadas (${decision.factors.size})")
                    }
                    item {
                        SectionPanel {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                decision.factors.forEachIndexed { index, factor ->
                                    if (index > 0) {
                                        PanelDivider()
                                    }
                                    ThreatFactorRow(factor = factor)
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                color = colors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(thickness = 1.dp, color = colors.border)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.onAccent
                            ),
                            shape = MaterialTheme.shapes.small,
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Volver a zona segura",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
