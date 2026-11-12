package com.antiphishing.smishguard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
        DateTimeFormatter.ofPattern("dd MMM · HH:mm", esLocale)
    private val fullFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", esLocale)

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
fun toneColors(tone: SemanticTone): Triple<Color, Color, Color> {
    val colors = SmishGuardTheme.colors
    return when (tone) {
        SemanticTone.NEUTRAL -> Triple(colors.surfaceRecessed, colors.textSecondary, colors.border)
        SemanticTone.ACCENT -> Triple(colors.accentContainer, colors.onAccentContainer, colors.accent.copy(alpha = 0.35f))
        SemanticTone.SAFE -> Triple(colors.safeContainer, colors.safe, colors.safeBorder)
        SemanticTone.WARNING -> Triple(colors.warningContainer, colors.warning, colors.warningBorder)
        SemanticTone.DANGER -> Triple(colors.dangerContainer, colors.danger, colors.dangerBorder)
    }
}

@Composable
fun StatusTag(
    text: String,
    tone: SemanticTone,
    modifier: Modifier = Modifier,
    showDot: Boolean = false
) {
    val (bg, fg, border) = toneColors(tone)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(fg, CircleShape)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SeverityBars(
    activeBars: Int,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    val clamped = activeBars.coerceIn(1, 3)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 1..3) {
            val height = when (i) {
                1 -> 6.dp
                2 -> 9.dp
                else -> 12.dp
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height)
                    .background(
                        color = if (i <= clamped) activeColor else colors.borderStrong.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
fun ThreatStatusBadge(
    level: ThreatLevel,
    modifier: Modifier = Modifier,
    score: Int? = null
) {
    val colors = SmishGuardTheme.colors
    val (label, icon, bars) = when (level) {
        ThreatLevel.SAFE -> Triple("SEGURO", Icons.Outlined.CheckCircle, 1)
        ThreatLevel.SUSPICIOUS -> Triple("SOSPECHOSO", Icons.Outlined.WarningAmber, 2)
        ThreatLevel.PHISHING -> Triple("CRÍTICO", Icons.Outlined.GppBad, 3)
    }
    val tone = level.toSemanticTone()
    val (container, tint, border) = toneColors(tone)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = container,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (score != null) "$label · $score%" else label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            SeverityBars(
                activeBars = bars,
                activeColor = tint,
                modifier = Modifier.padding(start = 1.dp)
            )
        }
    }
}

@Composable
fun RiskScaleBar(
    score: Int,
    level: ThreatLevel,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    val clamped = score.coerceIn(0, 100)
    val activeColor = when (level) {
        ThreatLevel.SAFE -> colors.safe
        ThreatLevel.SUSPICIOUS -> colors.warning
        ThreatLevel.PHISHING -> colors.danger
    }
    val segments = 10
    val filledSegments = ((clamped + 9) / 10).coerceIn(if (clamped > 0) 1 else 0, segments)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (index in 0 until segments) {
                val isFilled = index < filledSegments
                val segmentColor = when {
                    !isFilled -> colors.surfaceRecessed
                    index < 3 -> if (level == ThreatLevel.SAFE) colors.safe else activeColor.copy(alpha = 0.65f)
                    index < 6 -> if (level == ThreatLevel.SUSPICIOUS) colors.warning else activeColor.copy(alpha = 0.82f)
                    else -> activeColor
                }
                val segmentBorder = if (isFilled) segmentColor else colors.border
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .background(segmentColor, RoundedCornerShape(2.dp))
                        .border(0.5.dp, segmentBorder, RoundedCornerShape(2.dp))
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0 · BAJO",
                style = MaterialTheme.typography.labelSmall,
                color = if (level == ThreatLevel.SAFE) colors.safe else colors.textMuted
            )
            Text(
                text = "30 · SOSPECHOSO",
                style = MaterialTheme.typography.labelSmall,
                color = if (level == ThreatLevel.SUSPICIOUS) colors.warning else colors.textMuted
            )
            Text(
                text = "60+ · CRÍTICO",
                style = MaterialTheme.typography.labelSmall,
                color = if (level == ThreatLevel.PHISHING) colors.danger else colors.textMuted
            )
        }
    }
}

@Composable
fun TopScreenBand(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    trailing: (@Composable () -> Unit)? = null
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!badgeText.isNullOrBlank()) {
                        StatusTag(
                            text = badgeText,
                            tone = SemanticTone.NEUTRAL
                        )
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailing != null) {
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    trailing()
                }
            }
        }
        PanelDivider()
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
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(11.dp)
                    .background(colors.accent, RoundedCornerShape(1.dp))
            )
            Text(
                text = text.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }
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
    accentEdgeColor: Color? = null,
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
        val columnModifier = if (accentEdgeColor != null) {
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = accentEdgeColor,
                        size = Size(width = 4.dp.toPx(), height = size.height)
                    )
                }
                .padding(start = 4.dp)
        } else {
            Modifier.fillMaxWidth()
        }
        Column(
            modifier = columnModifier,
            content = content
        )
    }
}

@Composable
fun PanelHeaderBand(
    title: String,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = SmishGuardTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceSubtle)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
            if (trailing != null) {
                trailing()
            }
        }
        PanelDivider()
    }
}

@Composable
fun PanelDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = SmishGuardTheme.colors.divider
    )
}

@Composable
fun MonospaceBlock(
    text: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    badgeText: String? = null,
    badgeTone: SemanticTone = SemanticTone.NEUTRAL
) {
    val colors = SmishGuardTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(colors.surfaceRecessed)
            .border(1.dp, colors.border, MaterialTheme.shapes.small)
    ) {
        if (!label.isNullOrBlank() || !badgeText.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceSubtle)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (label ?: "PAYLOAD").uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
                if (!badgeText.isNullOrBlank()) {
                    val (_, fg, _) = toneColors(badgeTone)
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = fg
                    )
                }
            }
            PanelDivider()
        }
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun ThreatFactorRow(
    factor: ThreatFactor,
    modifier: Modifier = Modifier
) {
    val colors = SmishGuardTheme.colors
    val (tone, bars) = when {
        factor.severityScore >= 30 -> SemanticTone.DANGER to 3
        factor.severityScore >= 15 -> SemanticTone.WARNING to 2
        else -> SemanticTone.NEUTRAL to 1
    }
    val (_, tint, _) = toneColors(tone)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SeverityBars(activeBars = bars, activeColor = tint)
                Text(
                    text = factor.category,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
            }
            Text(
                text = factor.description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            val snippet = factor.matchedSnippet
            if (!snippet.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .background(colors.surfaceRecessed, RoundedCornerShape(4.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = snippet,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = colors.accent,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceSubtle,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.surface, RoundedCornerShape(6.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
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
                .widthIn(max = 760.dp)
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
        unfocusedBorderColor = colors.borderStrong,
        errorBorderColor = colors.danger,
        focusedLabelColor = colors.accent,
        unfocusedLabelColor = colors.textSecondary,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        focusedPlaceholderColor = colors.textMuted,
        unfocusedPlaceholderColor = colors.textMuted,
        cursorColor = colors.accent,
        focusedContainerColor = colors.surfaceRecessed,
        unfocusedContainerColor = colors.surfaceRecessed
    )
}

@Composable
fun smishGuardSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = SmishGuardTheme.colors.onAccent,
    checkedTrackColor = SmishGuardTheme.colors.accent,
    uncheckedThumbColor = SmishGuardTheme.colors.textMuted,
    uncheckedTrackColor = SmishGuardTheme.colors.surfaceRecessed,
    uncheckedBorderColor = SmishGuardTheme.colors.borderStrong
)
