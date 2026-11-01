package com.autopoweroff.app.util

import android.content.Context
import android.util.Log
import com.autopoweroff.app.data.PowerOffMode
import com.autopoweroff.app.service.AutoPowerOffAccessibilityService

object PowerActionManager {
    private const val TAG = "PowerActionManager"

    sealed interface ExecutionResult {
        data object Success : ExecutionResult
        data class Failure(val reason: String) : ExecutionResult
    }

    /**
     * Executes the configured power action.
     */
    fun executePowerAction(context: Context, mode: PowerOffMode): ExecutionResult {
        Log.i(TAG, "Executing power action mode: $mode")
        return when (mode) {
            PowerOffMode.ACCESSIBILITY_LOCK -> {
                if (AutoPowerOffAccessibilityService.lockScreen()) {
                    ExecutionResult.Success
                } else {
                    ExecutionResult.Failure(
                        "Accessibility Service not active or failed to lock screen. Please enable the AutoPowerOff Accessibility Service in Settings."
                    )
                }
            }
            PowerOffMode.ACCESSIBILITY_POWER_MENU -> {
                if (AutoPowerOffAccessibilityService.showPowerDialog()) {
                    ExecutionResult.Success
                } else {
                    ExecutionResult.Failure(
                        "Accessibility Service not active or failed to show power dialog. Please enable the AutoPowerOff Accessibility Service in Settings."
                    )
                }
            }
            PowerOffMode.ROOT_SHUTDOWN -> {
                if (RootExecutor.executeShutdown()) {
                    ExecutionResult.Success
                } else {
                    ExecutionResult.Failure(
                        "Root shutdown command failed. Ensure your device is rooted and root permissions are granted to AutoPowerOff."
                    )
                }
            }
        }
    }
}

