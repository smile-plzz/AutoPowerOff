package com.autopoweroff.app.ui.main

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autopoweroff.app.data.AppSettings
import com.autopoweroff.app.data.DefaultSettingsRepository
import com.autopoweroff.app.data.PowerOffMode
import com.autopoweroff.app.data.SettingsRepository
import com.autopoweroff.app.service.AutoPowerOffAccessibilityService
import com.autopoweroff.app.service.IdleServiceState
import com.autopoweroff.app.service.IdleTrackingService
import com.autopoweroff.app.util.PowerActionManager
import com.autopoweroff.app.util.RootExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiMessage {
    data class Info(val message: String) : UiMessage
    data class Error(val message: String) : UiMessage
}

interface PermissionHelper {
    fun isAccessibilityGranted(): Boolean
    fun isIgnoringBatteryOptimizations(): Boolean
    suspend fun isRootAvailable(): Boolean
}

class DefaultPermissionHelper(private val context: Context) : PermissionHelper {
    override fun isAccessibilityGranted(): Boolean {
        return AutoPowerOffAccessibilityService.isAccessibilitySettingsEnabled(context)
    }

    override fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    override suspend fun isRootAvailable(): Boolean {
        return RootExecutor.isRootAvailable()
    }
}

class MainScreenViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository = DefaultSettingsRepository.getInstance(application),
    private val permissionHelper: PermissionHelper = DefaultPermissionHelper(application)
) : AndroidViewModel(application) {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
    val serviceState: StateFlow<IdleServiceState> = IdleTrackingService.serviceState

    private val _isAccessibilityGranted = MutableStateFlow(false)
    val isAccessibilityGranted: StateFlow<Boolean> = _isAccessibilityGranted.asStateFlow()

    private val _isRootAvailable = MutableStateFlow(false)
    val isRootAvailable: StateFlow<Boolean> = _isRootAvailable.asStateFlow()

    private val _isIgnoringBatteryOptimizations = MutableStateFlow(false)
    val isIgnoringBatteryOptimizations: StateFlow<Boolean> = _isIgnoringBatteryOptimizations.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiMessage>()
    val uiEvents: SharedFlow<UiMessage> = _uiEvents.asSharedFlow()

    init {
        refreshPermissionStatuses()
    }

    fun refreshPermissionStatuses() {
        _isAccessibilityGranted.value = permissionHelper.isAccessibilityGranted()
        _isIgnoringBatteryOptimizations.value = permissionHelper.isIgnoringBatteryOptimizations()

        viewModelScope.launch(Dispatchers.IO) {
            _isRootAvailable.value = permissionHelper.isRootAvailable()
        }
    }

    fun toggleService(enabled: Boolean) {
        val context = getApplication<Application>()
        settingsRepository.setEnabled(enabled)
        try {
            if (enabled) {
                IdleTrackingService.start(context)
                emitMessage(UiMessage.Info("AutoPowerOff service started."))
            } else {
                IdleTrackingService.stop(context)
                emitMessage(UiMessage.Info("AutoPowerOff service stopped."))
            }
        } catch (e: Exception) {
            emitMessage(UiMessage.Info("Service setting updated (enabled: $enabled)."))
        }
    }

    fun setIdleTimeoutMinutes(minutes: Int) {
        settingsRepository.setIdleTimeoutMinutes(minutes)
    }

    fun setPowerOffMode(mode: PowerOffMode) {
        settingsRepository.setPowerOffMode(mode)
    }

    fun setWarningCountdownSeconds(seconds: Int) {
        settingsRepository.setWarningCountdownSeconds(seconds)
    }

    fun setPauseWhileCharging(pause: Boolean) {
        settingsRepository.setPauseWhileCharging(pause)
    }

    fun setResetOnMovement(reset: Boolean) {
        settingsRepository.setResetOnMovement(reset)
    }

    fun setStartOnBoot(start: Boolean) {
        settingsRepository.setStartOnBoot(start)
    }

    fun testPowerActionNow() {
        val context = getApplication<Application>()
        val mode = settings.value.powerOffMode
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = PowerActionManager.executePowerAction(context, mode)) {
                is PowerActionManager.ExecutionResult.Success -> {
                    _uiEvents.emit(UiMessage.Info("Test execution succeeded for: ${mode.title}"))
                }
                is PowerActionManager.ExecutionResult.Failure -> {
                    _uiEvents.emit(UiMessage.Error(result.reason))
                }
            }
        }
    }

    private fun emitMessage(message: UiMessage) {
        viewModelScope.launch {
            _uiEvents.emit(message)
        }
    }
}
