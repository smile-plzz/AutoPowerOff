# Development Log

## [2026-11-02 02:33:00 +06:00] - Project Initialization & Architecture Setup
- **Task**: Initialized `AutoPowerOff` Android application repository and documentation.
- **Context Established**:
  - Outlined Android security architecture regarding hardware shutdown vs screen locking.
  - Defined multi-mode execution: Root Shutdown, Accessibility Lock Screen, and Power Dialog trigger.
  - Created initial documentation files: `README.md`, `context.md`, `dev-log.md`, `CHANGELOG.md`.
- **Status**: Scaffolding project structure and clarifying requirements.

## [2026-11-02 03:05:00 +06:00] - Complete Implementation & Verified Build
- **Task**: Integrated official Android CLI skills (`https://github.com/android/skills`) including `edge-to-edge`, `android-permissions-security`, `android-intent-security`, and `testing-setup`. Implemented complete AutoPowerOff app with Jetpack Compose Material 3 UI, Foreground Service, Accessibility Service, motion detection, and unit tests.
- **Changes Implemented**:
  1. **Official Skills Integration**:
     - Installed Android CLI (`android.exe`) and initialized agent skills repository.
     - Installed `edge-to-edge`, `android-permissions-security`, `android-intent-security`, `testing-setup`.
  2. **Data & Preferences Layer**:
     - `PowerOffMode`: Enum for `ACCESSIBILITY_LOCK`, `ACCESSIBILITY_POWER_MENU`, and `ROOT_SHUTDOWN`.
     - `AppSettings`: Data model for timeouts, countdown warnings, charger bypass, motion reset, and boot start.
     - `DefaultSettingsRepository`: SharedPreferences persistence with reactive StateFlow updates.
  3. **Privileged Actions & Accessibility Integration**:
     - `AutoPowerOffAccessibilityService`: Implements `GLOBAL_ACTION_LOCK_SCREEN` (API 28+) and `GLOBAL_ACTION_POWER_DIALOG` (API 21+).
     - `RootExecutor`: Detection and execution of hardware shutdown commands (`svc power shutdown`, `reboot -p`, etc.).
     - `PowerActionManager`: Clean execution routing and result reporting.
  4. **Background Tracking & Safety Service**:
     - `IdleTrackingService`: Foreground service with `specialUse` type adhering to Android 14/15 requirements.
     - Inactivity countdown timer tracking screen-off events.
     - Heads-up warning notification with countdown alert and instant "Cancel Auto-Off" action button.
     - Charger protection receiver (`ACTION_POWER_CONNECTED` / `ACTION_POWER_DISCONNECTED`).
     - `MotionDetector`: Accelerometer sensor listener to reset timers when phone is moved.
     - `BootReceiver` and `ServiceActionReceiver`.
  5. **UI Layer**:
     - Modern Jetpack Compose Material 3 UI with Edge-to-Edge support.
     - Live master toggle card with active status indicators.
     - System diagnostics & permissions card (Accessibility, Notifications, Battery Optimization, Root).
     - Configurable idle timeout chips, power-off mode selection cards, warning duration, and safety toggles.
     - Test & verification execution dialog.
  6. **Testing & Verification**:
     - `SettingsRepositoryTest`: Unit tests for model serialization, duration calculations, and edge cases.
     - `PowerActionManagerTest`: Unit tests verifying modes and result contracts.
     - `MainScreenViewModelTest`: Coroutine unit tests for ViewModel state and setting updates.
     - Build verification: `./gradlew test assembleDebug` completed successfully with exit code 0.
     - Output APK: `app/build/outputs/apk/debug/app-debug.apk` (18.9 MB).
- **Status**: Completed and fully verified.


## [2026-11-02 15:15:00 +06:00] - Launch Crash Triage and Fix
- **Issue**: App crashed immediately upon launch when tested on physical device.
- **Root Cause Analysis**:
  1. `MainScreenViewModel` had default constructor parameters without `@JvmOverloads`. In Java bytecode, no single-argument `MainScreenViewModel(Application)` constructor existed. When `MainScreen` invoked `viewModel()`, `AndroidViewModelFactory` failed with `NoSuchMethodException` trying to instantiate `MainScreenViewModel(Application)`.
  2. `MainActivity` launched through experimental Navigation3 `rememberNavBackStack(Main)`, creating unnecessary runtime serialization overhead for a single-screen app.
  3. `AutoPowerOffAccessibilityService` in `AndroidManifest.xml` had `android:exported="false"`. Android requires accessibility services to have `android:exported="true"` (protected by `BIND_ACCESSIBILITY_SERVICE`) so the system framework can bind to it.
  4. Security checks and permission queries lacked defensive error handling against OEM-specific security restrictions.
- **Fixes Applied**:
  1. Added `@JvmOverloads` to `MainScreenViewModel` constructor and exposed explicit `MainScreenViewModel.Factory` (`ViewModelProvider.Factory`).
  2. Direct `MainScreen()` rendering in `MainActivity.kt`, removing unused Navigation3 wrappers.
  3. Set `android:exported="true"` and `android:label` on `AutoPowerOffAccessibilityService`.
  4. Wrapped `isAccessibilitySettingsEnabled`, `isIgnoringBatteryOptimizations`, `RootExecutor.isRootAvailable`, and `initializeInitialState` in safe `try/catch` blocks.
- **Verification**:
  - Bytecode verified via `javap`: `MainScreenViewModel(Application)` constructor present.
  - All unit tests passed (`./gradlew test`).
  - Debug APK rebuilt successfully: `app/build/outputs/apk/debug/app-debug.apk`.
