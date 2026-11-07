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

    private val _userName = MutableStateFlow(prefs.getString("user_name", "Administrador") ?: "Administrador")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userNickname = MutableStateFlow(prefs.getString("user_nickname", "Gestor Financeiro") ?: "Gestor Financeiro")
    val userNickname: StateFlow<String> = _userNickname.asStateFlow()

    private val _walletName = MutableStateFlow(prefs.getString("wallet_name", "Carteira Principal") ?: "Carteira Principal")
    val walletName: StateFlow<String> = _walletName.asStateFlow()

    private val _defaultCurrency = MutableStateFlow(prefs.getString("default_currency", "BRL (R$)") ?: "BRL (R$)")
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    private val _dateFormat = MutableStateFlow(prefs.getString("date_format", "DD/MM/AAAA") ?: "DD/MM/AAAA")
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    private val initialStart = prefs.getLong("app_start_date", 0L).let {
        if (it == 0L) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong("app_start_date", now).apply()
            now
        } else it
    }
    private val _appStartDate = MutableStateFlow(initialStart)
    val appStartDate: StateFlow<Long> = _appStartDate.asStateFlow()

    private val _notifyPayables = MutableStateFlow(prefs.getBoolean("notify_payables", true))
    val notifyPayables: StateFlow<Boolean> = _notifyPayables.asStateFlow()

    private val _notifyReceivables = MutableStateFlow(prefs.getBoolean("notify_receivables", true))
    val notifyReceivables: StateFlow<Boolean> = _notifyReceivables.asStateFlow()

    private val _notificationTime = MutableStateFlow(prefs.getString("notification_time", "08:00") ?: "08:00")
    val notificationTime: StateFlow<String> = _notificationTime.asStateFlow()

    fun updateProfile(
        name: String,
        nickname: String,
        wallet: String,
        currency: String = "BRL (R$)",
        formatDate: String = "DD/MM/AAAA"
    ) {
        _userName.value = name.trim().ifBlank { "Administrador" }
        _userNickname.value = nickname.trim().ifBlank { "Gestor Financeiro" }
        _walletName.value = wallet.trim().ifBlank { "Carteira Principal" }
        _defaultCurrency.value = currency
        _dateFormat.value = formatDate

        prefs.edit()
            .putString("user_name", _userName.value)
            .putString("user_nickname", _userNickname.value)
            .putString("wallet_name", _walletName.value)
            .putString("default_currency", _defaultCurrency.value)
            .putString("date_format", _dateFormat.value)
            .apply()
    }

    fun setNotifyPayables(enabled: Boolean) {
        _notifyPayables.value = enabled
        prefs.edit().putBoolean("notify_payables", enabled).apply()
    }

    fun setNotifyReceivables(enabled: Boolean) {
        _notifyReceivables.value = enabled
        prefs.edit().putBoolean("notify_receivables", enabled).apply()
    }

    fun setNotificationTime(time: String) {
        _notificationTime.value = time
        prefs.edit().putString("notification_time", time).apply()
    }

    private fun hashPin(pin: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("CF_SALT_$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
