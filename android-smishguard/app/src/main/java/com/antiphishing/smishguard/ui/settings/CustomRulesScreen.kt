package com.antiphishing.smishguard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antiphishing.smishguard.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRulesScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val customRules by viewModel.customRules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var patternInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reglas de Detección Propias", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    validationError = null
                    showAddDialog = true
                },
                containerColor = CyberCyan,
                contentColor = DarkBackground
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Regla")
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Define palabras clave o expresiones regulares (Regex) para aislar mensajes específicos.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (customRules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Rule,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No hay reglas personalizadas creadas",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(customRules, key = { it.id }) { rule ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rule.pattern,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (rule.isEnabled) CyberCyan else TextSecondary
                                )
                                if (rule.description.isNotBlank()) {
                                    Text(text = rule.description, fontSize = 12.sp, color = TextSecondary)
                                }
                                val ruleTypeText = if (rule.isRegex) {
                                    "Tipo: Expresión Regular (Regex)"
                                } else {
                                    "Tipo: Palabra clave exacta"
                                }
                                Text(
                                    text = ruleTypeText,
                                    fontSize = 11.sp,
                                    color = TextSecondary.copy(alpha = 0.7f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = rule.isEnabled,
                                    onCheckedChange = { viewModel.toggleCustomRule(rule) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CyberCyan,
                                        checkedTrackColor = CyberCyanDark.copy(alpha = 0.5f)
                                    )
                                )
                                IconButton(onClick = { viewModel.deleteCustomRule(rule) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = ThreatDangerRed)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Nueva Regla de Bloqueo", color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = patternInput,
                            onValueChange = {
                                patternInput = it
                                validationError = null
                            },
                            label = { Text("Patrón o Palabra clave") },
                            placeholder = { Text("Ej: promo-descuento o bono de regalo") },
                            singleLine = true,
                            isError = validationError != null
                        )
                        val currentError = validationError
                        if (currentError != null) {
                            Text(
                                text = currentError,
                                color = ThreatDangerRed,
                                fontSize = 12.sp
                            )
                        }
                        OutlinedTextField(
                            value = descInput,
                            onValueChange = { descInput = it },
                            label = { Text("Descripción o Motivo (Opcional)") },
                            singleLine = true
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("¿Es expresión regular (Regex)?", fontSize = 13.sp, color = TextPrimary)
                            Checkbox(
                                checked = isRegex,
                                onCheckedChange = {
                                    isRegex = it
                                    validationError = null
                                },
                                colors = CheckboxDefaults.colors(checkedColor = CyberCyan)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addCustomRule(patternInput, isRegex, descInput) { err ->
                                if (err != null) {
                                    validationError = err
                                } else {
                                    patternInput = ""
                                    descInput = ""
                                    isRegex = false
                                    validationError = null
                                    showAddDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkBackground)
                    ) {
                        Text("Crear Regla")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}
