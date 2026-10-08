package com.antiphishing.smishguard.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antiphishing.smishguard.domain.model.SmsMessageData
import com.antiphishing.smishguard.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToQuarantine: () -> Unit,
    onNavigateToAnalyzer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalScanned by viewModel.totalScanned.collectAsState()
    val quarantinedCount by viewModel.quarantinedCount.collectAsState()
    val phishingCount by viewModel.phishingCount.collectAsState()
    val recentQuarantined by viewModel.recentQuarantined.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SmishGuard",
                        style = MaterialTheme.typography.headlineMedium,
                        color = CyberCyan
                    )
                    Text(
                        text = "Protección contra smishing y fraude",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceCard,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = ThreatSafeGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "100% Local",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThreatSafeGreen
                        )
                    }
                }
            }
        }

        item {
            ShieldStatusCard(
                isActive = uiState.isProtectionActive,
                onToggle = { viewModel.toggleProtection(it) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Analizados",
                    value = totalScanned.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Amenazas",
                    value = phishingCount.toString(),
                    icon = Icons.Default.Warning,
                    color = ThreatDangerRed,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Cuarentena",
                    value = quarantinedCount.toString(),
                    icon = Icons.Default.Lock,
                    color = ThreatWarningYellow,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToAnalyzer,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analizar Enlace", maxLines = 1)
                }

                Button(
                    onClick = onNavigateToQuarantine,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver Cuarentena", maxLines = 1)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Amenazas Detectadas",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (recentQuarantined.isNotEmpty()) {
                    Text(
                        text = "Ver todo (${recentQuarantined.size})",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onNavigateToQuarantine() }
                    )
                }
            }
        }

        if (recentQuarantined.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = ThreatSafeGreen,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Sin amenazas recientes",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "No se han detectado mensajes maliciosos.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(recentQuarantined.take(3), key = { it.id }) { threat ->
                RecentThreatItem(threat = threat, onClick = onNavigateToQuarantine)
            }
        }
    }
}

@Composable
fun ShieldStatusCard(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val gradientBrush = if (isActive) {
        Brush.horizontalGradient(listOf(Color(0xFF0F3A40), Color(0xFF13283B)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF3A1A1A), Color(0xFF261313)))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isActive) ThreatSafeGreenBg else ThreatDangerRedBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Security else Icons.Default.GppBad,
                            contentDescription = null,
                            tint = if (isActive) ThreatSafeGreen else ThreatDangerRed,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isActive) "Protección Activa" else "Protección Pausada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isActive) "Filtrando SMS y Phishing en tiempo real" else "El dispositivo está desprotegido",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isActive) CyberCyan else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberCyan,
                        checkedTrackColor = CyberCyanDark.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = title, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
fun RecentThreatItem(
    threat: SmsMessageData,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ThreatDangerRedBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ThreatDangerRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = threat.sender,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = dateFormat.format(Date(threat.timestamp)),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = threat.body,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ThreatDangerRedBg
            ) {
                Text(
                    text = "${threat.threatScore}%",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = ThreatDangerRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
