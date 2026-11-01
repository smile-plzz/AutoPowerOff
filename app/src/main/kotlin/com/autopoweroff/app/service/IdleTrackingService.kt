package com.autopoweroff.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.autopoweroff.app.MainActivity
import com.autopoweroff.app.R
import com.autopoweroff.app.data.AppSettings
import com.autopoweroff.app.data.DefaultSettingsRepository
import com.autopoweroff.app.receiver.ServiceActionReceiver
import com.autopoweroff.app.util.MotionDetector
import com.autopoweroff.app.util.PowerActionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class IdleServiceState(
    val isRunning: Boolean = false,
    val isScreenOff: Boolean = false,
    val isCharging: Boolean = false,
    val elapsedIdleSeconds: Long = 0L,
    val remainingSeconds: Long = 0L,
    val isWarningActive: Boolean = false,
    val warningRemainingSeconds: Int = 0
)

class IdleTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private lateinit var settingsRepository: DefaultSettingsRepository
    private lateinit var notificationManager: NotificationManager
    private var motionDetector: MotionDetector? = null

    private var isScreenOff = false
    private var isCharging = false
    private var elapsedIdleSeconds = 0L
    private var isWarningCountdownActive = false
    private var warningSecondsRemaining = 0

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.i(TAG, "Screen turned off. Beginning idle inactivity countdown.")
                    isScreenOff = true
                    resetTimer()
                    startMonitoringLoop()
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.i(TAG, "Screen turned on. User is active.")
                    isScreenOff = false
                    cancelWarning()
                    resetTimer()
                    updateNotification()
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    Log.i(TAG, "Power connected.")
                    isCharging = true
                    cancelWarning()
                    updateNotification()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    Log.i(TAG, "Power disconnected.")
                    isCharging = false
                    updateNotification()
                }
            }
            publishState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "IdleTrackingService onCreate")
        settingsRepository = DefaultSettingsRepository.getInstance(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannels()
        registerSystemReceivers()
        initializeInitialState()

        motionDetector = MotionDetector(this) {
            if (settingsRepository.getSettings().resetOnMovement) {
                Log.d(TAG, "Device movement detected. Resetting inactivity timer.")
                resetTimer()
                cancelWarning()
                updateNotification()
                publishState()
            }
        }
        if (settingsRepository.getSettings().resetOnMovement) {
            motionDetector?.start()
        }

        _serviceRunningState.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "IdleTrackingService onStartCommand action: $action")

        when (action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_CANCEL_COUNTDOWN -> {
                Log.i(TAG, "User clicked Cancel Auto-Off.")
                cancelWarning()
                resetTimer()
                updateNotification()
                publishState()
            }
            ACTION_RESET_TIMER -> {
                resetTimer()
                cancelWarning()
                updateNotification()
                publishState()
            }
            ACTION_TRIGGER_NOW -> {
                executePowerOff()
            }
            else -> {
                // Default start or refresh
                val initialNotification = buildMonitoringNotification(getString(R.string.service_running_idle))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID_TRACKING,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID_TRACKING, initialNotification)
                }
                startMonitoringLoop()
            }
        }

        return START_STICKY
    }

    private fun initializeInitialState() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        isScreenOff = !powerManager.isInteractive

        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun registerSystemReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(
            this,
            systemReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun startMonitoringLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val settings = settingsRepository.getSettings()

                if (!settings.isEnabled) {
                    stopSelf()
                    break
                }

                if (isCharging && settings.pauseWhileCharging) {
                    // Paused due to charging
                    publishState()
                    continue
                }

                // If screen is off (or user wants idle tracking)
                if (isScreenOff) {
                    elapsedIdleSeconds++
                    val totalTimeout = settings.totalIdleTimeoutSeconds
                    val warningThreshold = settings.warningThresholdSeconds

                    if (elapsedIdleSeconds >= totalTimeout) {
                        // Time reached! Trigger power action
                        executePowerOff()
                        resetTimer()
                    } else if (elapsedIdleSeconds >= warningThreshold) {
                        // In warning countdown zone
                        isWarningCountdownActive = true
                        warningSecondsRemaining = (totalTimeout - elapsedIdleSeconds).toInt()
                        showWarningNotification(warningSecondsRemaining)
                    } else {
                        isWarningCountdownActive = false
                    }
                }

                publishState()
            }
        }
    }

    private fun resetTimer() {
        elapsedIdleSeconds = 0L
    }

    private fun cancelWarning() {
        isWarningCountdownActive = false
        warningSecondsRemaining = 0
        notificationManager.cancel(NOTIFICATION_ID_WARNING)
    }

    private fun executePowerOff() {
        cancelWarning()
        val settings = settingsRepository.getSettings()
        Log.i(TAG, "Idle timeout reached! Dispatching power action: ${settings.powerOffMode}")
        PowerActionManager.executePowerAction(applicationContext, settings.powerOffMode)
    }

    private fun publishState() {
        val settings = settingsRepository.getSettings()
        val total = settings.totalIdleTimeoutSeconds
        val remaining = (total - elapsedIdleSeconds).coerceAtLeast(0L)

        _serviceState.value = IdleServiceState(
            isRunning = true,
            isScreenOff = isScreenOff,
            isCharging = isCharging,
            elapsedIdleSeconds = elapsedIdleSeconds,
            remainingSeconds = remaining,
            isWarningActive = isWarningCountdownActive,
            warningRemainingSeconds = warningSecondsRemaining
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val trackingChannel = NotificationChannel(
                CHANNEL_ID_TRACKING,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val warningChannel = NotificationChannel(
                CHANNEL_ID_WARNING,
                getString(R.string.warning_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.warning_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            notificationManager.createNotificationChannel(trackingChannel)
            notificationManager.createNotificationChannel(warningChannel)
        }
    }

    private fun buildMonitoringNotification(statusText: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val pendingAppIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_TRACKING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.service_running_title))
            .setContentText(statusText)
            .setContentIntent(pendingAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        val text = when {
            isCharging && settingsRepository.getSettings().pauseWhileCharging -> getString(R.string.service_paused_charging)
            isScreenOff -> "Screen off: Idle monitoring active"
            else -> getString(R.string.service_running_idle)
        }
        val notification = buildMonitoringNotification(text)
        notificationManager.notify(NOTIFICATION_ID_TRACKING, notification)
    }

    private fun showWarningNotification(secondsRemaining: Int) {
        val cancelIntent = Intent(this, ServiceActionReceiver::class.java).apply {
            action = ACTION_CANCEL_COUNTDOWN
        }
        val pendingCancelIntent = PendingIntent.getBroadcast(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val appIntent = Intent(this, MainActivity::class.java)
        val pendingAppIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_WARNING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.warning_notification_title))
            .setContentText("Device will turn off in $secondsRemaining seconds due to inactivity.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Inactivity timeout reached! The device will power down or lock in $secondsRemaining seconds. Tap 'Cancel Auto-Off' below to keep the device awake.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingAppIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.cancel_auto_off),
                pendingCancelIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID_WARNING, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "IdleTrackingService onDestroy")
        _serviceRunningState.value = false
        timerJob?.cancel()
        motionDetector?.stop()
        try {
            unregisterReceiver(systemReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering systemReceiver", e)
        }
        notificationManager.cancel(NOTIFICATION_ID_WARNING)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "IdleTrackingService"
        private const val CHANNEL_ID_TRACKING = "idle_tracking_channel"
        private const val CHANNEL_ID_WARNING = "warning_channel"
        private const val NOTIFICATION_ID_TRACKING = 1001
        private const val NOTIFICATION_ID_WARNING = 1002

        const val ACTION_START = "com.autopoweroff.app.action.START"
        const val ACTION_STOP = "com.autopoweroff.app.action.STOP"
        const val ACTION_RESET_TIMER = "com.autopoweroff.app.action.RESET_TIMER"
        const val ACTION_CANCEL_COUNTDOWN = "com.autopoweroff.app.action.CANCEL_COUNTDOWN"
        const val ACTION_TRIGGER_NOW = "com.autopoweroff.app.action.TRIGGER_NOW"

        private val _serviceState = MutableStateFlow(IdleServiceState())
        val serviceState: StateFlow<IdleServiceState> = _serviceState.asStateFlow()

        private val _serviceRunningState = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _serviceRunningState.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, IdleTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, IdleTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun resetTimer(context: Context) {
            val intent = Intent(context, IdleTrackingService::class.java).apply {
                action = ACTION_RESET_TIMER
            }
            context.startService(intent)
        }

        fun triggerNow(context: Context) {
            val intent = Intent(context, IdleTrackingService::class.java).apply {
                action = ACTION_TRIGGER_NOW
            }
            context.startService(intent)
        }
    }
}

