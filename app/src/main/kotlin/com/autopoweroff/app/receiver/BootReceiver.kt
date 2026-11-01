package com.autopoweroff.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.autopoweroff.app.data.DefaultSettingsRepository
import com.autopoweroff.app.service.IdleTrackingService

/**
 * Automatically starts the IdleTrackingService upon device boot if configured in settings.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i(TAG, "Device boot completed, checking AutoPowerOff settings...")
        val settings = DefaultSettingsRepository.getInstance(context).getSettings()

        if (settings.isEnabled && settings.startOnBoot) {
            Log.i(TAG, "Starting IdleTrackingService on boot.")
            IdleTrackingService.start(context)
        } else {
            Log.i(TAG, "Start on boot is disabled (isEnabled=${settings.isEnabled}, startOnBoot=${settings.startOnBoot}).")
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

