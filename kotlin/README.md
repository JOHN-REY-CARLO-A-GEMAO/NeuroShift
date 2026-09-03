# NeuroShift (Kotlin / Android)

The Kotlin implementation of NeuroShift — a standalone Android Gradle project,
independent from the original Java app at the repository root and from the
Flutter project in `../neuroshift flutter`.

## Contents

- `app/` — Android application module
  - Package `com.neuroshift.kotlin` (application id `com.neuroshift.kotlin`, so it
    can be installed side by side with the original `com.neuroshift.game` app)
  - Full Kotlin port of the original game:
    - `MainActivity` — title screen (play / leaderboard / quit, high score)
    - `GameActivity` + `GameView` — the gravity-rotation survival game
      (custom `SurfaceView` rendering, 60 FPS `GameLoop` thread, obstacles,
      particles, trails, levels, pause)
    - `GameOverActivity` — score / level / retry / menu
    - `LeaderboardActivity` — top 10 scores (local `SharedPreferences`)
    - `Obstacle`, `BouncingObstacle`, `Particle`, `ScoreManager`
- `gradlew` / `gradlew.bat` — Gradle wrapper (same distribution as the root app)

## Requirements

- JDK 17 or newer (the project pins a JVM toolchain in `gradle/gradle-daemon-jvm.properties`)
- Android SDK: compileSdk 34, minSdk 21, targetSdk 34 (same as the original app)
- Android Studio (Ladybug or newer) or any IDE that speaks Gradle 9

## Build

```bash
./gradlew assembleDebug          # or: ./gradlew :app:assembleDebug
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (device connected)
```

Or open the `kotlin/` folder as a standalone project in Android Studio.

## Toolchain

This project intentionally mirrors the root app's toolchain (AGP 9.3.1,
Gradle 9.6.1) and adds the Kotlin Gradle plugin 2.4.0, so both native
implementations can be built with the same setup.
