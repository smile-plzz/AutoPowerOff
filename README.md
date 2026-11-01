# AutoPowerOff for Android

**AutoPowerOff** is an Android application designed to monitor device idle/unused time and automatically trigger a power-off or screen-lock action to conserve battery and protect privacy.

## Features
- **Inactivity Detection**: Tracks when the screen is off or the phone is left untouched/stationary.
- **Configurable Idle Timer**: Set auto-off delays (e.g., 10m, 30m, 1h, 2h, custom).
- **Multiple Turn-Off Modes**:
  - **Root Power-Off**: Clean hardware shutdown via `su` (`svc power shutdown`).
  - **Accessibility Lock & Sleep**: Instant lock & screen shutoff using Android Accessibility Service.
  - **Power Menu Trigger**: Brings up the native power dialog automatically.
- **Abort & Warning Countdown**: Visual heads-up notification with audio/vibration and a 30-second cancellation window before powering down.
- **Charger Protection**: Option to pause the auto-off timer while charging.

## Project Structure
```
AutoPowerOff/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/autopoweroff/app/
│   │   │   │   ├── data/          # Preferences & settings
│   │   │   │   ├── service/       # IdleTrackingService & Accessibility
│   │   │   │   ├── receiver/      # Screen, Battery & Alarm receivers
│   │   │   │   ├── ui/            # Compose UI screens & ViewModels
│   │   │   │   └── util/          # Root executor & Power actions
│   │   │   └── AndroidManifest.xml
├── README.md
├── dev-log.md
├── context.md
└── CHANGELOG.md
```

## Setup & Building
1. Ensure JDK 21+ and Android SDK (Platform 34/35) are installed.
2. Build via Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install to connected device or emulator:
   ```bash
   ./gradlew installDebug
   ```

## Usage
1. Open AutoPowerOff and grant necessary permissions:
   - Notification Permission (Android 13+)
   - Accessibility Service (for Screen Off / Power Dialog automation)
   - Root access (Optional, only needed for hardware shutdown)
2. Set your desired inactivity timeout.
3. Toggle "Enable Auto-Off Service".
