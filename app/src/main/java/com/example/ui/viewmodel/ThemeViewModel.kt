package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "DARK") ?: "DARK")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.getLong("last_backup_time", 0L))
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _lastExportedTime = MutableStateFlow(prefs.getLong("last_exported_time", 0L))
    val lastExportedTime: StateFlow<Long> = _lastExportedTime.asStateFlow()

    private val _skipDriveNotice = MutableStateFlow(prefs.getBoolean("skip_drive_notice", false))
    val skipDriveNotice: StateFlow<Boolean> = _skipDriveNotice.asStateFlow()

    private val _isPrivacyMode = MutableStateFlow(prefs.getBoolean("privacy_mode", false))
    val isPrivacyMode: StateFlow<Boolean> = _isPrivacyMode.asStateFlow()

    private val storedPinHash = prefs.getString("app_pin_hash", null)
    private val _isPinConfigured = MutableStateFlow(!storedPinHash.isNullOrBlank())
    val isPinConfigured: StateFlow<Boolean> = _isPinConfigured.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(storedPinHash.isNullOrBlank())
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun updateLastBackupTime(timestamp: Long) {
        _lastBackupTime.value = timestamp
        prefs.edit().putLong("last_backup_time", timestamp).apply()
    }

    fun updateLastExportedTime(timestamp: Long) {
        _lastExportedTime.value = timestamp
        prefs.edit().putLong("last_exported_time", timestamp).apply()
    }

    fun setSkipDriveNotice(skip: Boolean) {
        _skipDriveNotice.value = skip
        prefs.edit().putBoolean("skip_drive_notice", skip).apply()
    }

    fun togglePrivacyMode() {
        val next = !_isPrivacyMode.value
        _isPrivacyMode.value = next
        prefs.edit().putBoolean("privacy_mode", next).apply()
    }

    fun setAppPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString("app_pin_hash", hash).apply()
        _isPinConfigured.value = true
        _isAppUnlocked.value = true
    }

    fun removeAppPin() {
        prefs.edit().remove("app_pin_hash").apply()
        _isPinConfigured.value = false
        _isAppUnlocked.value = true
    }

    fun verifyAndUnlock(pin: String): Boolean {
        val currentHash = prefs.getString("app_pin_hash", null) ?: return true
        val inputHash = hashPin(pin)
        val matches = currentHash == inputHash
        if (matches) {
            _isAppUnlocked.value = true
        }
        return matches
    }

    fun lockApp() {
        if (_isPinConfigured.value) {
            _isAppUnlocked.value = false
        }
    }

    private fun hashPin(pin: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("CF_SALT_$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
