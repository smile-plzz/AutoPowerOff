# AutoPowerOff for Android

**AutoPowerOff** is a modern Android application designed to monitor device idle/unused time and automatically trigger a power-off or screen-lock action to conserve battery and protect privacy.

## Features
- **Inactivity Detection**: Tracks when the screen is off or the phone is left untouched/stationary.
- **Configurable Idle Timer**: Set auto-off delays (e.g., 5m, 10m, 15m, 30m, 1h, 2h).
- **Multiple Turn-Off Modes**:
  - **Accessibility Lock & Sleep**: Instant lock & screen shutoff using Android Accessibility Service (Recommended, non-root).
  - **Native Power Menu Trigger**: Brings up the system power dialog automatically.
  - **Root Power-Off**: Clean hardware shutdown via `su` (`svc power shutdown` / `reboot -p`).
- **Abort & Warning Countdown**: Visual heads-up notification with audio/vibration and a configurable cancellation window (15s, 30s, 60s) before powering down.
- **Motion Detection**: Reset inactivity timer when the phone is physically moved or picked up.
- **Charger Protection**: Option to pause the auto-off timer while connected to power.
- **Boot Persistence**: Option to resume monitoring automatically when the device boots.
- **Edge-to-Edge Material 3 UI**: Full compliance with Android 15 edge-to-edge guidelines.

## Project Structure
```
AutoPowerOff/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/autopoweroff/app/
│   │   │   │   ├── data/          # AppSettings, PowerOffMode, SettingsRepository
│   │   │   │   ├── service/       # IdleTrackingService, AutoPowerOffAccessibilityService
│   │   │   │   ├── receiver/      # BootReceiver, ServiceActionReceiver
│   │   │   │   ├── ui/            # Compose UI (MainScreen, MainScreenViewModel, Theme)
│   │   │   │   └── util/          # RootExecutor, PowerActionManager, MotionDetector
│   │   │   ├── res/               # XML configs, layouts, strings, themes
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                  # Unit tests (SettingsRepositoryTest, PowerActionManagerTest, MainScreenViewModelTest)
│   │   └── androidTest/           # Instrumented UI tests (MainScreenTest)
├── README.md
├── dev-log.md
├── context.md
└── CHANGELOG.md
```

## Setup & Building
1. Ensure JDK 21+ and Android SDK (Platform 35/36) are installed.
2. Build via Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run unit tests:
   ```bash
   ./gradlew test
   ```
4. Output APK location:
   `app/build/outputs/apk/debug/app-debug.apk`

## Usage
1. Open AutoPowerOff and configure desired permissions:
   - **Accessibility Service**: Required for non-root Screen Lock & Power Menu.
   - **Notifications**: Required on Android 13+ to display the warning countdown notification.
   - **Battery Optimization**: Whitelist recommended for continuous background execution.
   - **Root Access**: Optional; only needed for true hardware shutdown.
2. Select your inactivity timeout and turn-off mode.
3. Toggle "AutoPowerOff Enabled" to activate monitoring.
