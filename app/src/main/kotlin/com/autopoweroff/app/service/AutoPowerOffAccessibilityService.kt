package com.autopoweroff.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

class AutoPowerOffAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        Log.i(TAG, "AutoPowerOffAccessibilityService connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No specific window content inspection required.
    }

    override fun onInterrupt() {
        Log.w(TAG, "AutoPowerOffAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance?.get() == this) {
            instance = null
        }
        Log.i(TAG, "AutoPowerOffAccessibilityService destroyed.")
    }

    companion object {
        private const val TAG = "AccessibilityService"
        private var instance: WeakReference<AutoPowerOffAccessibilityService>? = null

        /**
         * Returns true if the service is currently running and bound.
         */
        val isConnected: Boolean
            get() = instance?.get() != null

        /**
         * Locks the screen and sleeps the device using GLOBAL_ACTION_LOCK_SCREEN.
         */
        fun lockScreen(): Boolean {
            val service = instance?.get()
            if (service == null) {
                Log.w(TAG, "AccessibilityService instance is null, cannot lock screen.")
                return false
            }

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val result = service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                Log.i(TAG, "GLOBAL_ACTION_LOCK_SCREEN dispatched, result: $result")
                result
            } else {
                Log.w(TAG, "GLOBAL_ACTION_LOCK_SCREEN requires Android 9 (API 28)+.")
                false
            }
        }

        /**
         * Shows the system power dialog using GLOBAL_ACTION_POWER_DIALOG.
         */
        fun showPowerDialog(): Boolean {
            val service = instance?.get()
            if (service == null) {
                Log.w(TAG, "AccessibilityService instance is null, cannot show power dialog.")
                return false
            }

            val result = service.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            Log.i(TAG, "GLOBAL_ACTION_POWER_DIALOG dispatched, result: $result")
            return result
        }

        /**
         * Checks whether the user has enabled this Accessibility Service in Android Settings.
         */
        fun isAccessibilitySettingsEnabled(context: Context): Boolean {
            val expectedComponentName = "${context.packageName}/${AutoPowerOffAccessibilityService::class.java.canonicalName}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        /**
         * Intent to launch the Accessibility settings screen so the user can enable the service.
         */
        fun createSettingsIntent(): Intent {
            return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

