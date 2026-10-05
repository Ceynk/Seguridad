package com.antiphishing.smishguard.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.antiphishing.smishguard.SmishGuardApp
import com.antiphishing.smishguard.ui.navigation.AppNavigation
import com.antiphishing.smishguard.ui.navigation.Screen
import com.antiphishing.smishguard.ui.theme.DarkBackground
import com.antiphishing.smishguard.ui.theme.SmishGuardTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Si se concedieron los permisos básicos de telefonía, solicitar el rol de filtrado de llamadas
        if (permissions[Manifest.permission.READ_PHONE_STATE] == true ||
            permissions[Manifest.permission.RECEIVE_SMS] == true
        ) {
            requestCallScreeningRole()
        }
    }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()

        val app = application as SmishGuardApp
        var initialSharedSender = ""
        var initialSharedBody = ""
        var targetDestination = Screen.Dashboard.route

        if (intent.getStringExtra("navigate_to") == "quarantine") {
            targetDestination = Screen.Quarantine.route
        } else if (intent.action == android.content.Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT) ?: ""
            if (sharedText.isNotBlank()) {
                initialSharedBody = sharedText
                targetDestination = Screen.ManualAnalyzer.route
            }
        } else if (intent.action == android.content.Intent.ACTION_SENDTO || intent.action == android.content.Intent.ACTION_VIEW) {
            val schemeSpecific = intent.data?.schemeSpecificPart ?: ""
            val sharedText = intent.getStringExtra("sms_body") ?: intent.getStringExtra(android.content.Intent.EXTRA_TEXT) ?: ""
            if (schemeSpecific.isNotBlank() || sharedText.isNotBlank()) {
                initialSharedSender = schemeSpecific
                initialSharedBody = sharedText
                targetDestination = Screen.ManualAnalyzer.route
            }
        }

        setContent {
            SmishGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        app = app,
                        startDestination = targetDestination,
                        initialSharedSender = initialSharedSender,
                        initialSharedBody = initialSharedBody
                    )
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CALL_LOG)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            requestCallScreeningRole()
        }
    }

    /**
     * En Android 10+ (API 29+), solicita al sistema el rol oficial de filtrado e interceptación
     * de llamadas fraudulentas (ROLE_CALL_SCREENING) para que CallScreeningService reciba las llamadas.
     */
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
            } catch (e: Exception) {
                // Silencioso ante personalizaciones de fabricante
            }
        }
    }
}
