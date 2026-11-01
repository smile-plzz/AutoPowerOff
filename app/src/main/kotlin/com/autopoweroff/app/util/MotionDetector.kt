package com.autopoweroff.app.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Monitors physical movement via accelerometer to detect when the device is picked up or moved.
 */
class MotionDetector(
    context: Context,
    private val onMotionDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isRegistered = false
    private var lastMagnitude = 9.81f
    private var lastTriggerTimestamp = 0L

    fun start() {
        if (isRegistered || accelerometer == null || sensorManager == null) return
        val success = sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        isRegistered = success
        Log.i(TAG, "MotionDetector registered: $success")
    }

    fun stop() {
        if (!isRegistered || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isRegistered = false
        Log.i(TAG, "MotionDetector stopped.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val currentMagnitude = sqrt(x * x + y * y + z * z)
        val delta = abs(currentMagnitude - lastMagnitude)
        lastMagnitude = currentMagnitude

        // Threshold of significant motion
        if (delta > MOTION_THRESHOLD) {
            val now = System.currentTimeMillis()
            // Throttle to avoid repeated triggers within cooldown window
            if (now - lastTriggerTimestamp > DEBOUNCE_MS) {
                lastTriggerTimestamp = now
                Log.d(TAG, "Significant motion detected (delta=$delta). Resetting idle timer.")
                onMotionDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val TAG = "MotionDetector"
        private const val MOTION_THRESHOLD = 2.0f // m/s^2 delta
        private const val DEBOUNCE_MS = 3000L // 3 seconds debounce
    }
}

