package com.autopoweroff.app.data

/**
 * Supported execution mechanisms for turning off or sleeping the device.
 */
enum class PowerOffMode(val title: String, val description: String) {
    ACCESSIBILITY_LOCK(
        title = "Lock Screen & Sleep",
        description = "Turns off display and locks the device securely via Accessibility Service (Recommended, No Root Required)."
    ),
    ACCESSIBILITY_POWER_MENU(
        title = "Native Power Menu",
        description = "Brings up the native Android system power dialog via Accessibility Service."
    ),
    ROOT_SHUTDOWN(
        title = "Root Hardware Shutdown",
        description = "Executes true hardware shutdown command via 'su' binary (Requires Root Access)."
    );

    companion object {
        fun fromName(name: String?): PowerOffMode {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ACCESSIBILITY_LOCK
        }
    }
}

