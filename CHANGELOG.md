# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2026-11-02
### Fixed
- Fixed fatal crash on launch caused by missing `MainScreenViewModel(Application)` constructor in compiled bytecode: added `@JvmOverloads` and explicit `ViewModelProvider.Factory`.
- Simplified `MainActivity` root composition to directly render `MainScreen()`, eliminating Navigation3 initialization failure risks.
- Fixed `AutoPowerOffAccessibilityService` manifest binding configuration by setting `android:exported="true"`.
- Added defensive exception handling across system service queries, battery status receivers, and root checks.

## [1.0.0] - 2026-11-02
### Added
- Complete implementation of `AutoPowerOff` Android application.
- `IdleTrackingService`: Foreground service (Android 14/15 compatible `specialUse` FGS) for background idle monitoring.
- `AutoPowerOffAccessibilityService`: Non-root screen locking via `GLOBAL_ACTION_LOCK_SCREEN` and native power dialog trigger via `GLOBAL_ACTION_POWER_DIALOG`.
- `RootExecutor`: Privileged hardware shutdown execution via `su` command (`svc power shutdown`).
- `MotionDetector`: Accelerometer-based motion sensing to reset timer on physical movement.
- `BootReceiver`: Automatic service startup on device boot.
- `ServiceActionReceiver`: Immediate cancellation from high-priority warning countdown notifications.
- Compose Material 3 UI with Edge-to-Edge compliance (`MainScreen.kt`, `MainScreenViewModel.kt`).
- Comprehensive unit test suite (`SettingsRepositoryTest`, `PowerActionManagerTest`, `MainScreenViewModelTest`).
- Verified debug APK generation via `./gradlew assembleDebug`.
