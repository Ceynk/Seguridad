package com.antiphishing.smishguard.data.repository

import android.content.Context
import android.content.SharedPreferences

class SecuritySettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smishguard_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROTECTION_ENABLED = "protection_enabled"
        private const val KEY_QUARANTINE_THRESHOLD = "quarantine_threshold"
        private const val KEY_AGGRESSIVE_MODE = "aggressive_mode"
        private const val KEY_SILENT_DISMISS_NOTIF = "silent_dismiss_notif"
        private const val KEY_COMMUNITY_CONFIRMATIONS = "community_confirmations"
    }

    var isProtectionEnabled: Boolean
        get() {
            return prefs.getBoolean(KEY_PROTECTION_ENABLED, true)
        }
        set(value) {
            val editor = prefs.edit()
            editor.putBoolean(KEY_PROTECTION_ENABLED, value)
            editor.apply()
        }

    var quarantineThreshold: Int
        get() {
            return prefs.getInt(KEY_QUARANTINE_THRESHOLD, 50)
        }
        set(value) {
            val editor = prefs.edit()
            editor.putInt(KEY_QUARANTINE_THRESHOLD, value)
            editor.apply()
        }

    var isAggressiveModeEnabled: Boolean
        get() {
            return prefs.getBoolean(KEY_AGGRESSIVE_MODE, false)
        }
        set(value) {
            val editor = prefs.edit()
            editor.putBoolean(KEY_AGGRESSIVE_MODE, value)
            editor.apply()
        }

    var isSilentDismissNotifEnabled: Boolean
        get() {
            return prefs.getBoolean(KEY_SILENT_DISMISS_NOTIF, true)
        }
        set(value) {
            val editor = prefs.edit()
            editor.putBoolean(KEY_SILENT_DISMISS_NOTIF, value)
            editor.apply()
        }

    var communityConfirmations: Int
        get() = prefs.getInt(KEY_COMMUNITY_CONFIRMATIONS, 0)
        set(value) {
            prefs.edit().putInt(KEY_COMMUNITY_CONFIRMATIONS, value).apply()
        }
}
