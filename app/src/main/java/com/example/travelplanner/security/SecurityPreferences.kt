package com.example.travelplanner.security

import android.content.Context

interface SecurityPreferences {
    fun isBiometricEnabled(): Boolean
    fun setBiometricEnabled(enabled: Boolean)
    fun lockAfterSeconds(): Int
    fun setLockAfterSeconds(seconds: Int)
}

class SharedPreferencesSecurityPreferences(context: Context) : SecurityPreferences {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isBiometricEnabled(): Boolean =
        prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    override fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).commit()
    }

    override fun lockAfterSeconds(): Int =
        prefs.getInt(KEY_LOCK_AFTER_SECONDS, DEFAULT_LOCK_SECONDS)

    override fun setLockAfterSeconds(seconds: Int) {
        prefs.edit()
            .putInt(KEY_LOCK_AFTER_SECONDS, seconds.coerceIn(10, 600))
            .commit()
    }

    private companion object {
        const val PREFS_NAME = "security_preferences"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_LOCK_AFTER_SECONDS = "lock_after_seconds"
        const val DEFAULT_LOCK_SECONDS = 60
    }
}

class InMemorySecurityPreferences(
    biometricEnabled: Boolean = false,
    lockSeconds: Int = 60
) : SecurityPreferences {
    private var enabled = biometricEnabled
    private var lockAfter = lockSeconds

    override fun isBiometricEnabled(): Boolean = enabled

    override fun setBiometricEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun lockAfterSeconds(): Int = lockAfter

    override fun setLockAfterSeconds(seconds: Int) {
        lockAfter = seconds.coerceIn(10, 600)
    }
}
