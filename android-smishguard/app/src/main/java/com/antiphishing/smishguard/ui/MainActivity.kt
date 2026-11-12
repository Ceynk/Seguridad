package com.antiphishing.smishguard.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.antiphishing.smishguard.SmishGuardApp
import com.antiphishing.smishguard.ui.navigation.AppNavigation
import com.antiphishing.smishguard.ui.navigation.Screen
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme

private data class LaunchRouting(
    val destination: String = Screen.Dashboard.route,
    val sharedSender: String = "",
    val sharedBody: String = "",
    val token: Long = System.nanoTime()
)

class MainActivity : ComponentActivity() {

    private var pendingRouting by mutableStateOf<LaunchRouting?>(null)
    private var pendingPermissionList by mutableStateOf<Array<String>?>(null)
    private var showDeniedPermissionModal by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val phoneStateGranted = permissions[Manifest.permission.READ_PHONE_STATE] == true
        val receiveSmsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        val deniedPermissions = permissions.filterValues { !it }.keys
        if (deniedPermissions.isNotEmpty() && deniedPermissions.all {
                !shouldShowRequestPermissionRationale(it)
            }) {
            showDeniedPermissionModal = true
        }
        if (phoneStateGranted || receiveSmsGranted) {
            requestCallScreeningRole()
        }
    }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as SmishGuardApp
        val initialRouting = parseRoutingFromIntent(intent)

        setContent {
            SmishGuardTheme {
                val colors = SmishGuardTheme.colors
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colors.background
                ) {
                    val navController = rememberNavController()
                    val latestRouting = pendingRouting

                    LaunchedEffect(latestRouting?.token) {
                        if (latestRouting != null) {
                            navController.navigate(latestRouting.destination) {
                                val startId = navController.graph.findStartDestination().id
                                popUpTo(startId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }

                    AppNavigation(
                        navController = navController,
                        app = app,
                        startDestination = initialRouting.destination,
                        initialSharedSender = latestRouting?.sharedSender ?: initialRouting.sharedSender,
                        initialSharedBody = latestRouting?.sharedBody ?: initialRouting.sharedBody
                    )

                    val permissionsToLaunch = pendingPermissionList
                    if (permissionsToLaunch != null) {
                        AlertDialog(
                            onDismissRequest = { pendingPermissionList = null },
                            title = {
                                Text(
                                    text = "Permisos de protección",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.textPrimary
                                )
                            },
                            text = {
                                Text(
                                    text = "SmishGuard requiere permiso de recepción de SMS y estado telefónico para interceptar amenazas en tiempo real. El análisis se ejecuta localmente sin leer tu historial previo.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val list = permissionsToLaunch
                                        pendingPermissionList = null
                                        permissionLauncher.launch(list)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accent,
                                        contentColor = colors.onAccent
                                    ),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text("Continuar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingPermissionList = null }) {
                                    Text("Ahora no", color = colors.textSecondary)
                                }
                            },
                            containerColor = colors.surface,
                            shape = MaterialTheme.shapes.large
                        )
                    }

                    if (showDeniedPermissionModal) {
                        AlertDialog(
                            onDismissRequest = { showDeniedPermissionModal = false },
                            title = {
                                Text(
                                    text = "Permiso desactivado",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.textPrimary
                                )
                            },
                            text = {
                                Text(
                                    text = "El canal asociado al permiso rechazado permanece inactivo. Puedes habilitarlo desde los ajustes del sistema.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDeniedPermissionModal = false
                                        val settingsIntent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:$packageName")
                                        )
                                        startActivity(settingsIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accent,
                                        contentColor = colors.onAccent
                                    ),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text("Abrir ajustes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeniedPermissionModal = false }) {
                                    Text("Cerrar", color = colors.textSecondary)
                                }
                            },
                            containerColor = colors.surface,
                            shape = MaterialTheme.shapes.large
                        )
                    }
                }
            }
        }

        requestRequiredPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRouting = parseRoutingFromIntent(intent)
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        if (ev != null && (
                ev.actionMasked == MotionEvent.ACTION_HOVER_ENTER ||
                    ev.actionMasked == MotionEvent.ACTION_HOVER_MOVE ||
                    ev.actionMasked == MotionEvent.ACTION_HOVER_EXIT
                )
        ) {
            return true
        }
        return try {
            super.dispatchGenericMotionEvent(ev)
        } catch (_: IllegalStateException) {
            true
        }
    }

    private fun parseRoutingFromIntent(incomingIntent: Intent?): LaunchRouting {
        if (incomingIntent == null) {
            return LaunchRouting()
        }

        var sharedSender = ""
        var sharedBody = ""
        var targetDestination = Screen.Dashboard.route

        val navigateTarget = incomingIntent.getStringExtra("navigate_to")
        if (navigateTarget == "quarantine") {
            targetDestination = Screen.Quarantine.route
        } else if (navigateTarget == "dashboard") {
            targetDestination = Screen.Dashboard.route
        } else if (incomingIntent.action == Intent.ACTION_SEND) {
            val intentType = incomingIntent.type
            if (intentType != null && intentType.startsWith("text/")) {
                val sharedText = incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    sharedBody = sharedText
                    targetDestination = Screen.ManualAnalyzer.route
                }
            }
        } else if (incomingIntent.action == Intent.ACTION_VIEW) {
            val dataUri = incomingIntent.data
            if (dataUri != null) {
                val scheme = dataUri.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") {
                    sharedBody = dataUri.toString()
                    targetDestination = Screen.ManualAnalyzer.route
                } else if (scheme == "sms" || scheme == "smsto") {
                    sharedSender = dataUri.schemeSpecificPart.orEmpty()
                    sharedBody = incomingIntent.getStringExtra("sms_body")
                        ?: incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
                        ?: ""
                    targetDestination = Screen.ManualAnalyzer.route
                }
            }
        } else if (incomingIntent.action == Intent.ACTION_SENDTO) {
            val dataUri = incomingIntent.data
            if (dataUri != null) {
                sharedSender = dataUri.schemeSpecificPart.orEmpty()
            }
            sharedBody = incomingIntent.getStringExtra("sms_body")
                ?: incomingIntent.getStringExtra(Intent.EXTRA_TEXT)
                ?: ""
            if (sharedSender.isNotBlank() || sharedBody.isNotBlank()) {
                targetDestination = Screen.ManualAnalyzer.route
            }
        }

        return LaunchRouting(
            destination = targetDestination,
            sharedSender = sharedSender,
            sharedBody = sharedBody
        )
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            pendingPermissionList = permissionsToRequest.toTypedArray()
        } else {
            requestCallScreeningRole()
        }
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                        val roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        roleLauncher.launch(roleRequestIntent)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
