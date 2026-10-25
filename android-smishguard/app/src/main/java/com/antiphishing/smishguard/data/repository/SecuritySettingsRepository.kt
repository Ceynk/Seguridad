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
        get() = prefs.getBoolean(KEY_PROTECTION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, value).apply()

    var quarantineThreshold: Int
        get() = prefs.getInt(KEY_QUARANTINE_THRESHOLD, 50)
        set(value) = prefs.edit().putInt(KEY_QUARANTINE_THRESHOLD, value).apply()

    var isAggressiveModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_AGGRESSIVE_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_AGGRESSIVE_MODE, value).apply()

    var isSilentDismissNotifEnabled: Boolean
        get() = prefs.getBoolean(KEY_SILENT_DISMISS_NOTIF, true)
        set(value) = prefs.edit().putBoolean(KEY_SILENT_DISMISS_NOTIF, value).apply()

    var communityConfirmations: Int
        get() = prefs.getInt(KEY_COMMUNITY_CONFIRMATIONS, 0)
        set(value) {
            prefs.edit().putInt(KEY_COMMUNITY_CONFIRMATIONS, value).apply()
        }
}
