package com.autopoweroff.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    private class FakeSettingsRepository(initialSettings: AppSettings = AppSettings()) : SettingsRepository {
        private val _flow = MutableStateFlow(initialSettings)
        override val settingsFlow: StateFlow<AppSettings> = _flow.asStateFlow()

        override fun getSettings(): AppSettings = _flow.value
        override fun updateSettings(settings: AppSettings) { _flow.value = settings }
        override fun setEnabled(enabled: Boolean) { updateSettings(getSettings().copy(isEnabled = enabled)) }
        override fun setIdleTimeoutMinutes(minutes: Int) { updateSettings(getSettings().copy(idleTimeoutMinutes = minutes)) }
        override fun setPowerOffMode(mode: PowerOffMode) { updateSettings(getSettings().copy(powerOffMode = mode)) }
        override fun setWarningCountdownSeconds(seconds: Int) { updateSettings(getSettings().copy(warningCountdownSeconds = seconds)) }
        override fun setPauseWhileCharging(pause: Boolean) { updateSettings(getSettings().copy(pauseWhileCharging = pause)) }
        override fun setResetOnMovement(reset: Boolean) { updateSettings(getSettings().copy(resetOnMovement = reset)) }
        override fun setStartOnBoot(start: Boolean) { updateSettings(getSettings().copy(startOnBoot = start)) }
    }

    @Test
    fun defaultSettings_areSafeAndValid() {
        val settings = AppSettings()
        assertFalse(settings.isEnabled)
        assertEquals(30, settings.idleTimeoutMinutes)
        assertEquals(PowerOffMode.ACCESSIBILITY_LOCK, settings.powerOffMode)
        assertEquals(30, settings.warningCountdownSeconds)
        assertTrue(settings.pauseWhileCharging)
        assertTrue(settings.resetOnMovement)
        assertFalse(settings.startOnBoot)

        assertEquals(1800L, settings.totalIdleTimeoutSeconds)
        assertEquals(1770L, settings.warningThresholdSeconds)
    }

    @Test
    fun calculations_handleCustomIntervalsCorrectly() {
        val settings = AppSettings(
            idleTimeoutMinutes = 15,
            warningCountdownSeconds = 60
        )
        assertEquals(900L, settings.totalIdleTimeoutSeconds)
        assertEquals(840L, settings.warningThresholdSeconds)
    }

    @Test
    fun calculations_clampWarningThresholdAtZeroIfWarningExceedsTimeout() {
        val settings = AppSettings(
            idleTimeoutMinutes = 1, // 60 seconds
            warningCountdownSeconds = 120 // exceeds total
        )
        assertEquals(60L, settings.totalIdleTimeoutSeconds)
        assertEquals(0L, settings.warningThresholdSeconds)
    }

    @Test
    fun fakeRepository_updatesSettingsCorrectly() {
        val repository = FakeSettingsRepository()
        repository.setEnabled(true)
        repository.setIdleTimeoutMinutes(45)
        repository.setPowerOffMode(PowerOffMode.ROOT_SHUTDOWN)
        repository.setWarningCountdownSeconds(15)
        repository.setPauseWhileCharging(false)
        repository.setResetOnMovement(false)
        repository.setStartOnBoot(true)

        val updated = repository.getSettings()
        assertTrue(updated.isEnabled)
        assertEquals(45, updated.idleTimeoutMinutes)
        assertEquals(PowerOffMode.ROOT_SHUTDOWN, updated.powerOffMode)
        assertEquals(15, updated.warningCountdownSeconds)
        assertFalse(updated.pauseWhileCharging)
        assertFalse(updated.resetOnMovement)
        assertTrue(updated.startOnBoot)
    }

    @Test
    fun powerOffMode_fromName_fallbackToAccessibilityLock() {
        assertEquals(PowerOffMode.ACCESSIBILITY_LOCK, PowerOffMode.fromName("UNKNOWN_MODE"))
        assertEquals(PowerOffMode.ROOT_SHUTDOWN, PowerOffMode.fromName("ROOT_SHUTDOWN"))
        assertEquals(PowerOffMode.ACCESSIBILITY_POWER_MENU, PowerOffMode.fromName("ACCESSIBILITY_POWER_MENU"))
    }
}

