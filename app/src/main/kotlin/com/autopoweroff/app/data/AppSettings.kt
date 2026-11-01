package com.autopoweroff.app.data

/**
 * Configuration preferences for AutoPowerOff.
 */
data class AppSettings(
    val isEnabled: Boolean = false,
    val idleTimeoutMinutes: Int = 30,
    val powerOffMode: PowerOffMode = PowerOffMode.ACCESSIBILITY_LOCK,
    val warningCountdownSeconds: Int = 30,
    val pauseWhileCharging: Boolean = true,
    val resetOnMovement: Boolean = true,
    val startOnBoot: Boolean = false
) {
    val totalIdleTimeoutSeconds: Long
        get() = idleTimeoutMinutes.toLong() * 60L

    val warningThresholdSeconds: Long
        get() = (totalIdleTimeoutSeconds - warningCountdownSeconds).coerceAtLeast(0L)
}

