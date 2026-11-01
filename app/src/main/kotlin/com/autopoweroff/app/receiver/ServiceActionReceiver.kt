package com.autopoweroff.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.autopoweroff.app.service.IdleTrackingService

/**
 * Handles actions dispatched from notification buttons (e.g. Cancel countdown, Stop service).
 */
class ServiceActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Received action: $action")

        when (action) {
            IdleTrackingService.ACTION_CANCEL_COUNTDOWN -> {
                val serviceIntent = Intent(context, IdleTrackingService::class.java).apply {
                    this.action = IdleTrackingService.ACTION_CANCEL_COUNTDOWN
                }
                context.startService(serviceIntent)
            }
            IdleTrackingService.ACTION_STOP -> {
                val serviceIntent = Intent(context, IdleTrackingService::class.java).apply {
                    this.action = IdleTrackingService.ACTION_STOP
                }
                context.startService(serviceIntent)
            }
        }
    }

    companion object {
        private const val TAG = "ServiceActionReceiver"
    }
}

