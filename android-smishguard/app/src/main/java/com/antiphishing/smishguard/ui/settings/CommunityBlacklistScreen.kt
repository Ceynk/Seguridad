package com.antiphishing.smishguard.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antiphishing.smishguard.ui.theme.CyberCyan
import com.antiphishing.smishguard.ui.theme.DarkBackground
import com.antiphishing.smishguard.ui.theme.DarkSurface
import com.antiphishing.smishguard.ui.theme.TextPrimary
import com.antiphishing.smishguard.ui.theme.TextSecondary

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CommunityBlacklistScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val entradas by viewModel.communityEntries.collectAsState()
    var valor by remember { mutableStateOf("") }
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun enviar() {
        viewModel.reportarEnComunidad(valor) { mensaje ->
            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
        }
        valor = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista Negra Comunitaria", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Solo se comparte el indicador", fontWeight = FontWeight.Bold, color = CyberCyan)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "No se envía el SMS, el remitente, tus contactos ni datos del dispositivo. Los reportes con menos de 5 confirmaciones se muestran como reportados por la comunidad. Desde 5 se marcan como maliciosos.",
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = valor,
                            onValueChange = { valor = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Enlace, dominio o número") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (viewModel.debeConfirmarReporte()) {
                                    mostrarConfirmacion = true
                                } else {
                                    enviar()
                                }
                            },
                            enabled = valor.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudUpload, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir y compartir")
                        }
                    }
                }
            }
            items(entradas, key = { it.id }) { entrada ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(entrada.indicador, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            "${entrada.tipo} · ${entrada.reportes} reporte(s) · " +
                                if (entrada.esMalicioso) "Malicioso" else "Reportado por la comunidad",
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("Confirmar envío") },
            text = {
                Text("Se enviará únicamente el indicador normalizado de esta entrada a la comunidad. No se enviará el contenido del SMS ni información del dispositivo.")
            },
            confirmButton = {
                Button(onClick = {
                    mostrarConfirmacion = false
                    viewModel.confirmarReporteMostrado()
                    enviar()
                }) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                Button(onClick = { mostrarConfirmacion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
