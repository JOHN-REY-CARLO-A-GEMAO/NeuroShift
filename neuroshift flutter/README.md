# NeuroShift (Flutter)

The Flutter implementation of **NeuroShift**, a gravity-rotation survival game.

A glowing ball falls toward the current gravity direction. **Tap** to rotate
gravity by 90° (down → left → up → right) and steer through incoming
obstacles. Survive as long as possible — your score is your survival time,
and difficulty ramps up every 10 seconds. High scores are saved locally and
shown on the leaderboard.

This project is a faithful port of the native Android app (see the Kotlin
`GameView` in `../kotlin`), built with the Flutter framework (Dart).

## Layout

```
lib/
├── main.dart                     App entry point + MaterialApp theme
├── game/
│   ├── constants.dart            Tuning values & ARGB color palette
│   ├── models.dart               Obstacle, BouncingObstacle, Particle
│   ├── game_engine.dart          Pure-Dart simulation (no Flutter imports)
│   └── game_painter.dart         CustomPainter that draws the whole scene
├── services/
│   └── score_manager.dart        SharedPreferences-backed score persistence
├── screens/
│   ├── main_menu_screen.dart     Title + PLAY / LEADERBOARD / QUIT
│   ├── game_screen.dart          Game loop (Ticker) + input handling
│   ├── game_over_screen.dart     Score / level / RETRY / MENU
│   └── leaderboard_screen.dart   Top-10 scores list
└── widgets/
    └── menu_button.dart          Shared button + monospace text style
```

The game simulation (`game_engine.dart`) is kept free of Flutter types so its
logic can be unit-tested in isolation (see `test/game_engine_test.dart`).

## Getting started

```bash
cd "neuroshift flutter"
flutter pub get
flutter test
flutter run
```

Platforms: Android, iOS, web, Linux, macOS, Windows.

## Notes

- The Android/iOS host projects under `android/` and `ios/` are standard
  `flutter create` output — they are not shared with the native Gradle
  projects in this repository.
- `lib/main.dart` used to hold the default counter template; it now hosts the
  NeuroShift app itself.
