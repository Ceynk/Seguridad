package com.antiphishing.smishguard.data.repository

import android.content.Context
import android.content.SharedPreferences

class SecuritySettingsRepository(context: Context) {

    enum class InternationalCallPolicy {
        ALLOW,
        BLOCK_ALL,
        BLOCK_SUSPICIOUS
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smishguard_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROTECTION_ENABLED = "protection_enabled"
        private const val KEY_QUARANTINE_THRESHOLD = "quarantine_threshold"
        private const val KEY_AGGRESSIVE_MODE = "aggressive_mode"
        private const val KEY_SILENT_DISMISS_NOTIF = "silent_dismiss_notif"
        private const val KEY_COMMUNITY_CONFIRMATIONS = "community_confirmations"
        private const val KEY_BLOCK_PRIVATE = "block_private_calls"
        private const val KEY_BLOCK_INVALID = "block_invalid_calls"
        private const val KEY_INTERNATIONAL_POLICY = "international_call_policy"
        private const val KEY_COMMUNITY_CURSOR = "community_sync_cursor"
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

    var blockPrivateCalls: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_PRIVATE, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_PRIVATE, value).apply()

    var blockInvalidCalls: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_INVALID, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_INVALID, value).apply()

    var internationalCallPolicy: InternationalCallPolicy
        get() = runCatching {
            InternationalCallPolicy.valueOf(
                prefs.getString(KEY_INTERNATIONAL_POLICY, InternationalCallPolicy.BLOCK_SUSPICIOUS.name)
                    ?: InternationalCallPolicy.BLOCK_SUSPICIOUS.name
            )
        }.getOrDefault(InternationalCallPolicy.BLOCK_SUSPICIOUS)
        set(value) = prefs.edit().putString(KEY_INTERNATIONAL_POLICY, value.name).apply()

    var communitySyncCursor: Long
        get() = prefs.getLong(KEY_COMMUNITY_CURSOR, 0L)
        set(value) = prefs.edit().putLong(KEY_COMMUNITY_CURSOR, value).apply()
}
