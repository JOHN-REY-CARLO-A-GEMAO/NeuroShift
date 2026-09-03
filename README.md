# NeuroShift

NeuroShift is a gravity-rotation survival game. This repository contains three
independent implementations of the app, each self-contained and developable
on its own:

```
NeuroShift/
├── (root)                 Original Android app (Java) — package com.neuroshift.game
├── kotlin/                Kotlin/Android implementation — package com.neuroshift.kotlin
└── neuroshift flutter/    Flutter implementation — package neuroshift_flutter
```

## 1. Original Android app (Java) — repository root

The original application, unchanged:

- `app/` — Android module, Java, `com.neuroshift.game`
- `build.gradle`, `settings.gradle`, `gradle*` — Gradle 9.6.1 / AGP 9.3.1 toolchain

Build:

```bash
./gradlew assembleDebug
```

## 2. Kotlin/Android implementation — `kotlin/`

A standalone Android Gradle project containing a full Kotlin port of the game
(same gameplay: 4-directional gravity rotation, obstacles, particles, levels,
leaderboard). It intentionally mirrors the root app's toolchain (AGP 9.3.1,
Gradle 9.6.1, compileSdk 34) and adds Kotlin 2.4.0.

- Application id `com.neuroshift.kotlin` — can be installed side by side with
  the original app.

Build:

```bash
cd kotlin
./gradlew assembleDebug
```

See [kotlin/README.md](kotlin/README.md).

## 3. Flutter implementation — `neuroshift flutter/`

A standard Flutter project (generated from the official `flutter create` app
template, stable channel) for the Flutter version of NeuroShift. It is fully
independent from the native Android projects — its own Gradle/AGP/Kotlin
versions live in `neuroshift flutter/android/`, and nothing is shared.

- Package `neuroshift_flutter`, org `com.neuroshift`
- Platforms: android, ios, web, linux, macos, windows
- `lib/main.dart` currently holds the default Flutter template app (counter
  demo) — the NeuroShift UI lives here.

Develop:

```bash
cd "neuroshift flutter"
flutter pub get
flutter test
flutter run
```

> Note: the folder name contains a space (`neuroshift flutter`) on purpose —
> quote it in shell commands.

## Repository notes

- Each implementation ships its own `.gitignore`, Gradle wrapper and build
  configuration; the root Gradle project (`settings.gradle`) only contains
  the original `:app` module, so `kotlin/` and the Flutter project are never
  mixed into the original build.
- The Flutter `android/` and `ios/` folders are standard user-managed host
  projects (as produced by `flutter create`); run `flutter pub get` once on a
  machine with the Flutter SDK before building.
