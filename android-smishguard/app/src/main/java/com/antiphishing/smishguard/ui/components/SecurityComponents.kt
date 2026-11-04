package com.antiphishing.smishguard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antiphishing.smishguard.domain.model.ThreatFactor
import com.antiphishing.smishguard.domain.model.ThreatLevel
import com.antiphishing.smishguard.ui.theme.SmishGuardSpacing
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeFormats {
    private val esLocale: Locale = Locale.forLanguageTag("es-CO")
    private val shortFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM, HH:mm", esLocale)
    private val fullFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", esLocale)

    fun formatShort(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(shortFormatter)
    }

    fun formatFull(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(fullFormatter)
    }
}

enum class SemanticTone {
    NEUTRAL,
    ACCENT,
    SAFE,
    WARNING,
    DANGER
}

fun ThreatLevel.toSemanticTone(): SemanticTone = when (this) {
    ThreatLevel.SAFE -> SemanticTone.SAFE
    ThreatLevel.SUSPICIOUS -> SemanticTone.WARNING
    ThreatLevel.PHISHING -> SemanticTone.DANGER
}

fun scoreToSemanticTone(score: Int): SemanticTone = when {
    score >= 60 -> SemanticTone.DANGER
    score >= 30 -> SemanticTone.WARNING
    else -> SemanticTone.SAFE
}

@Composable
fun StatusTag(
    text: String,
    tone: SemanticTone,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    val (bg, fg, border) = when (tone) {
        SemanticTone.NEUTRAL -> Triple(colors.surfaceSubtle, colors.textSecondary, colors.border)
        SemanticTone.ACCENT -> Triple(colors.accentContainer, colors.onAccentContainer, colors.accent.copy(alpha = 0.28f))
        SemanticTone.SAFE -> Triple(colors.safeContainer, colors.safe, colors.safeBorder)
        SemanticTone.WARNING -> Triple(colors.warningContainer, colors.warning, colors.warningBorder)
        SemanticTone.DANGER -> Triple(colors.dangerContainer, colors.danger, colors.dangerBorder)
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(
                horizontal = SmishGuardSpacing.sm,
                vertical = SmishGuardSpacing.xs
            )
        )
    }
}

@Composable
fun ThreatStatusBadge(
    level: ThreatLevel,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    val (label, icon, tone) = when (level) {
        ThreatLevel.SAFE -> Triple("LEGÍTIMO", Icons.Outlined.CheckCircle, SemanticTone.SAFE)
        ThreatLevel.SUSPICIOUS -> Triple("SOSPECHOSO", Icons.Outlined.WarningAmber, SemanticTone.WARNING)
        ThreatLevel.PHISHING -> Triple("MALICIOSO", Icons.Outlined.GppBad, SemanticTone.DANGER)
    }
    val tint = when (tone) {
        SemanticTone.SAFE -> colors.safe
        SemanticTone.WARNING -> colors.warning
        SemanticTone.DANGER -> colors.danger
        else -> colors.textPrimary
    }
    val container = when (tone) {
        SemanticTone.SAFE -> colors.safeContainer
        SemanticTone.WARNING -> colors.warningContainer
        SemanticTone.DANGER -> colors.dangerContainer
        else -> colors.surfaceSubtle
    }
    val border = when (tone) {
        SemanticTone.SAFE -> colors.safeBorder
        SemanticTone.WARNING -> colors.warningBorder
        SemanticTone.DANGER -> colors.dangerBorder
        else -> colors.border
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = SmishGuardSpacing.sm,
                vertical = SmishGuardSpacing.xs
            ),
            horizontalArrangement = Arrangement.spacedBy(SmishGuardSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tint
            )
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = SmishGuardTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
        if (trailing != null) {
            Box(modifier = Modifier.padding(start = 12.dp)) {
                trailing()
            }
        }
    }
}

@Composable
fun SectionLabel(
    text: String,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = SmishGuardTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted
        )
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun SectionPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = SmishGuardTheme.colors.surface,
    borderColor: Color = SmishGuardTheme.colors.border,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun PanelDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = SmishGuardTheme.colors.border
    )
}

@Composable
fun MonospaceBlock(
    text: String,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val colors = SmishGuardTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceSubtle, MaterialTheme.shapes.small)
            .padding(horizontal = SmishGuardSpacing.md, vertical = SmishGuardSpacing.sm)
    ) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(SmishGuardSpacing.xs))
        }
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = colors.textPrimary
        )
    }
}

@Composable
fun ThreatFactorRow(
    factor: ThreatFactor,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    val tone = when {
        factor.severityScore >= 30 -> SemanticTone.DANGER
        factor.severityScore >= 15 -> SemanticTone.WARNING
        else -> SemanticTone.NEUTRAL
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SmishGuardSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = SmishGuardSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = factor.category,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
            Text(
                text = factor.description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            val snippet = factor.matchedSnippet
            if (!snippet.isNullOrBlank()) {
                Text(
                    text = snippet,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    color = colors.accent,
                    modifier = Modifier.padding(top = SmishGuardSpacing.xs)
                )
            }
        }
        StatusTag(
            text = "+${factor.severityScore}",
            tone = tone
        )
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    SectionPanel(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SmishGuardSpacing.lg, vertical = SmishGuardSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ResponsiveContentBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SmishGuardTheme.colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            content()
        }
    }
}

@Composable
fun smishGuardTextFieldColors(): TextFieldColors {
    val colors = SmishGuardTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.border,
        errorBorderColor = colors.danger,
        focusedLabelColor = colors.accent,
        unfocusedLabelColor = colors.textSecondary,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        focusedPlaceholderColor = colors.textMuted,
        unfocusedPlaceholderColor = colors.textMuted,
        cursorColor = colors.accent,
        focusedContainerColor = colors.surface,
        unfocusedContainerColor = colors.surface
    )
}

@Composable
fun smishGuardSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = SmishGuardTheme.colors.onAccent,
    checkedTrackColor = SmishGuardTheme.colors.accent,
    uncheckedThumbColor = SmishGuardTheme.colors.textMuted,
    uncheckedTrackColor = SmishGuardTheme.colors.surfaceSubtle,
    uncheckedBorderColor = SmishGuardTheme.colors.border
)
