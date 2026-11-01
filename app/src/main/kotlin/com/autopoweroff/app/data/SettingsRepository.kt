package com.autopoweroff.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    val settingsFlow: StateFlow<AppSettings>
    fun getSettings(): AppSettings
    fun updateSettings(settings: AppSettings)
    fun setEnabled(enabled: Boolean)
    fun setIdleTimeoutMinutes(minutes: Int)
    fun setPowerOffMode(mode: PowerOffMode)
    fun setWarningCountdownSeconds(seconds: Int)
    fun setPauseWhileCharging(pause: Boolean)
    fun setResetOnMovement(reset: Boolean)
    fun setStartOnBoot(start: Boolean)
}

class DefaultSettingsRepository(context: Context) : SettingsRepository {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _settingsFlow = MutableStateFlow(loadFromPrefs())
    override val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadFromPrefs(): AppSettings {
        return AppSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            idleTimeoutMinutes = prefs.getInt(KEY_IDLE_TIMEOUT_MINUTES, 30),
            powerOffMode = PowerOffMode.fromName(prefs.getString(KEY_POWER_OFF_MODE, PowerOffMode.ACCESSIBILITY_LOCK.name)),
            warningCountdownSeconds = prefs.getInt(KEY_WARNING_COUNTDOWN_SECONDS, 30),
            pauseWhileCharging = prefs.getBoolean(KEY_PAUSE_WHILE_CHARGING, true),
            resetOnMovement = prefs.getBoolean(KEY_RESET_ON_MOVEMENT, true),
            startOnBoot = prefs.getBoolean(KEY_START_ON_BOOT, false)
        )
    }

    override fun getSettings(): AppSettings = _settingsFlow.value

    override fun updateSettings(settings: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.isEnabled)
            .putInt(KEY_IDLE_TIMEOUT_MINUTES, settings.idleTimeoutMinutes)
            .putString(KEY_POWER_OFF_MODE, settings.powerOffMode.name)
            .putInt(KEY_WARNING_COUNTDOWN_SECONDS, settings.warningCountdownSeconds)
            .putBoolean(KEY_PAUSE_WHILE_CHARGING, settings.pauseWhileCharging)
            .putBoolean(KEY_RESET_ON_MOVEMENT, settings.resetOnMovement)
            .putBoolean(KEY_START_ON_BOOT, settings.startOnBoot)
            .apply()
        _settingsFlow.value = settings
    }

    override fun setEnabled(enabled: Boolean) {
        updateSettings(getSettings().copy(isEnabled = enabled))
    }

    override fun setIdleTimeoutMinutes(minutes: Int) {
        updateSettings(getSettings().copy(idleTimeoutMinutes = minutes))
    }

    override fun setPowerOffMode(mode: PowerOffMode) {
        updateSettings(getSettings().copy(powerOffMode = mode))
    }

    override fun setWarningCountdownSeconds(seconds: Int) {
        updateSettings(getSettings().copy(warningCountdownSeconds = seconds))
    }

    override fun setPauseWhileCharging(pause: Boolean) {
        updateSettings(getSettings().copy(pauseWhileCharging = pause))
    }

    override fun setResetOnMovement(reset: Boolean) {
        updateSettings(getSettings().copy(resetOnMovement = reset))
    }

    override fun setStartOnBoot(start: Boolean) {
        updateSettings(getSettings().copy(startOnBoot = start))
    }

    companion object {
        private const val PREFS_NAME = "auto_power_off_preferences"
        private const val KEY_ENABLED = "key_enabled"
        private const val KEY_IDLE_TIMEOUT_MINUTES = "key_idle_timeout_minutes"
        private const val KEY_POWER_OFF_MODE = "key_power_off_mode"
        private const val KEY_WARNING_COUNTDOWN_SECONDS = "key_warning_countdown_seconds"
        private const val KEY_PAUSE_WHILE_CHARGING = "key_pause_while_charging"
        private const val KEY_RESET_ON_MOVEMENT = "key_reset_on_movement"
        private const val KEY_START_ON_BOOT = "key_start_on_boot"

        @Volatile
        private var INSTANCE: DefaultSettingsRepository? = null

        fun getInstance(context: Context): DefaultSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DefaultSettingsRepository(context).also { INSTANCE = it }
            }
        }
    }
}

