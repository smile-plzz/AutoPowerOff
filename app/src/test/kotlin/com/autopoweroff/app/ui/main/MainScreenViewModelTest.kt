package com.autopoweroff.app.ui.main

import android.app.Application
import com.autopoweroff.app.data.AppSettings
import com.autopoweroff.app.data.PowerOffMode
import com.autopoweroff.app.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSettingsRepo : SettingsRepository {
        private val _flow = MutableStateFlow(AppSettings())
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

    private class FakePermissionHelper(
        var accessibility: Boolean = true,
        var batteryOpt: Boolean = true,
        var root: Boolean = false
    ) : PermissionHelper {
        override fun isAccessibilityGranted(): Boolean = accessibility
        override fun isIgnoringBatteryOptimizations(): Boolean = batteryOpt
        override suspend fun isRootAvailable(): Boolean = root
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun viewModel_initializesWithSettingsAndPermissions() = runTest(testDispatcher) {
        val repo = FakeSettingsRepo()
        val permissionHelper = FakePermissionHelper(
            accessibility = true,
            batteryOpt = true,
            root = false
        )
        val viewModel = MainScreenViewModel(
            application = Application(),
            settingsRepository = repo,
            permissionHelper = permissionHelper
        )

        advanceUntilIdle()

        assertTrue(viewModel.isAccessibilityGranted.value)
        assertTrue(viewModel.isIgnoringBatteryOptimizations.value)
        assertFalse(viewModel.isRootAvailable.value)
        assertEquals(30, viewModel.settings.value.idleTimeoutMinutes)
    }

    @Test
    fun viewModel_updatesSettingsViaActions() = runTest(testDispatcher) {
        val repo = FakeSettingsRepo()
        val permissionHelper = FakePermissionHelper()
        val viewModel = MainScreenViewModel(
            application = Application(),
            settingsRepository = repo,
            permissionHelper = permissionHelper
        )

        viewModel.setIdleTimeoutMinutes(60)
        assertEquals(60, viewModel.settings.value.idleTimeoutMinutes)

        viewModel.setPowerOffMode(PowerOffMode.ROOT_SHUTDOWN)
        assertEquals(PowerOffMode.ROOT_SHUTDOWN, viewModel.settings.value.powerOffMode)

        viewModel.setWarningCountdownSeconds(15)
        assertEquals(15, viewModel.settings.value.warningCountdownSeconds)

        viewModel.setPauseWhileCharging(false)
        assertFalse(viewModel.settings.value.pauseWhileCharging)

        viewModel.setResetOnMovement(false)
        assertFalse(viewModel.settings.value.resetOnMovement)

        viewModel.setStartOnBoot(true)
        assertTrue(viewModel.settings.value.startOnBoot)
    }
}
