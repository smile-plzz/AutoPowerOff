# Project Context: AutoPowerOff (Android)

## Goals
- Create an Android application that automatically turns off the phone (or locks/turns off the screen) after a specified duration of inactivity/unused state.
- Support configurable idle timeout (e.g. 15m, 30m, 1h, 2h).
- Support multiple power-off mechanisms:
  1. **Root Shutdown** (`su -c "svc power shutdown"` or `reboot -p`): For rooted devices desiring a true hardware power-off.
  2. **Accessibility Power Dialog Automation**: Dispatches `GLOBAL_ACTION_POWER_DIALOG` and clicks power off if supported.
  3. **Screen Lock / Sleep Mode** (`GLOBAL_ACTION_LOCK_SCREEN` via Accessibility or DevicePolicyManager): Safe, non-root mechanism to lock the device and turn off screen.
- Provide user safety features:
  - Countdown warning notification before shutdown with "Cancel" action.
  - Motion detection (optional sensor check) to reset timer on physical movement.
  - Power connection bypass (optional: don't turn off if plugged into charger).

## Tech Stack
- **Platform**: Android (Min SDK 26 - Android 8.0 Oreo, Target SDK 35 - Android 15)
- **Language**: Kotlin
- **Build System**: Gradle (Kotlin DSL or Groovy DSL)
- **Architecture**: Modern Android architecture (MVVM / Clean architecture)
- **UI**: Jetpack Compose / Material 3
- **Background Processing**: Android Foreground Service + BroadcastReceivers + AlarmManager/WorkManager
- **Sensors**: Accelerometer / Significant Motion API (optional idle detection)

## Constraints & Considerations
- **Android Security Restrictions**: Standard third-party Android apps cannot programmatically invoke hardware power-down (`android.permission.SHUTDOWN`) without root or platform signature keys.
- **Background Execution Limits**: Must use a Foreground Service with notification to prevent Android OS from killing the idle monitoring service.
- **Battery Optimization**: The app must prompt user to disable battery optimizations (Doze mode whitelist) for continuous idle tracking.
