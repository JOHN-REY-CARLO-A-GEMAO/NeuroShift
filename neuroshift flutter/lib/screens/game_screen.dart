import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';

import '../game/constants.dart';
import '../game/game_engine.dart';
import '../game/game_painter.dart';
import '../services/score_manager.dart';
import 'game_over_screen.dart';

/// The in-game screen: runs the simulation on a [Ticker] and renders it with
/// a [CustomPainter], mirroring the native `GameView`.
class GameScreen extends StatefulWidget {
  const GameScreen({super.key, required this.scoreManager});

  final ScoreManager scoreManager;

  @override
  State<GameScreen> createState() => _GameScreenState();
}

class _GameScreenState extends State<GameScreen>
    with SingleTickerProviderStateMixin, WidgetsBindingObserver {
  GameEngine? _engine;
  late final Ticker _ticker;
  final ValueNotifier<int> _frame = ValueNotifier<int>(0);
  Duration _lastElapsed = Duration.zero;
  int _lastTapAt = 0;
  bool _gameOverHandled = false;
  double _elapsedSeconds = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _ticker = createTicker(_onTick);
    _ticker.start();
  }

  void _onTick(Duration elapsed) {
    _elapsedSeconds = elapsed.inMicroseconds / 1e6;
    final engine = _engine;
    if (engine == null) return;

    var dt = 0.0;
    if (_lastElapsed != Duration.zero) {
      dt = (elapsed - _lastElapsed).inMicroseconds / 1e6;
    }
    _lastElapsed = elapsed;
    if (dt > 0.05) dt = 0.05; // Cap to avoid a spiral of death.
    if (dt > 0) {
      engine.update(dt);
      if (engine.state == GameState.gameOver && !_gameOverHandled) {
        _gameOverHandled = true;
        _handleGameOver();
      }
    }
    _frame.value++;
  }

  void _handleGameOver() {
    final engine = _engine!;
    widget.scoreManager.saveScore(engine.score);

    // Match the native app: show the in-canvas "GAME OVER" for a moment,
    // then navigate to the dedicated game-over screen.
    Future<void>.delayed(const Duration(milliseconds: 1200), () {
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(
          builder: (_) => GameOverScreen(
            scoreManager: widget.scoreManager,
            score: engine.score,
            level: engine.level,
          ),
        ),
      );
    });
  }

  void _onTapUp(TapUpDetails details) {
    final engine = _engine;
    if (engine == null) return;

    final now = DateTime.now().millisecondsSinceEpoch;
    if (now - _lastTapAt < tapCooldownMs) return;
    _lastTapAt = now;

    switch (engine.state) {
      case GameState.ready:
        engine.state = GameState.playing;
        break;
      case GameState.playing:
        if (engine.pauseButtonRect.contains(details.localPosition)) {
          engine.state = GameState.paused;
        } else {
          engine.rotateGravity();
        }
        break;
      case GameState.paused:
        engine.state = GameState.playing;
        break;
      case GameState.gameOver:
        break;
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    final engine = _engine;
    if (engine == null) return;
    if (state != AppLifecycleState.resumed && engine.state == GameState.playing) {
      engine.state = GameState.paused;
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _ticker.dispose();
    _frame.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(colorBg),
      body: LayoutBuilder(
        builder: (context, constraints) {
          final w = constraints.maxWidth;
          final h = constraints.maxHeight;
          final engine = _engine ??= (GameEngine()..init(w, h));
          engine.setScreenSize(w, h);

          return GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTapUp: _onTapUp,
            child: CustomPaint(
              size: Size(w, h),
              painter: GamePainter(engine, _elapsedSeconds, repaint: _frame),
            ),
          );
        },
      ),
    );
  }
}
