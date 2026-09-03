import 'package:flutter/material.dart';

import '../game/constants.dart';
import '../services/score_manager.dart';
import '../widgets/menu_button.dart';
import 'game_screen.dart';

class GameOverScreen extends StatelessWidget {
  const GameOverScreen({
    super.key,
    required this.scoreManager,
    required this.score,
    required this.level,
  });

  final ScoreManager scoreManager;
  final int score;
  final int level;

  @override
  Widget build(BuildContext context) {
    final best = scoreManager.getHighScore();
    return Scaffold(
      backgroundColor: const Color(colorBg),
      body: SafeArea(
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                'GAME OVER',
                style: monoTextStyle(size: 44, color: colorObs1, bold: true),
              ),
              const SizedBox(height: 28),
              Text(
                'LEVEL $level',
                style: monoTextStyle(size: 22, color: colorAccent),
              ),
              const SizedBox(height: 24),
              Text(
                '$score',
                style: monoTextStyle(size: 90, color: colorScore, bold: true),
              ),
              const SizedBox(height: 20),
              Text(
                'BEST: $best',
                style: monoTextStyle(size: 20, color: 0xAAFFFFFF),
              ),
              const SizedBox(height: 48),
              MenuButton(
                label: '↺  RETRY',
                color: const Color(colorAccent),
                onTap: () {
                  Navigator.of(context).pushReplacement(
                    MaterialPageRoute<void>(
                      builder: (_) => GameScreen(scoreManager: scoreManager),
                    ),
                  );
                },
              ),
              const SizedBox(height: 14),
              MenuButton(
                label: '⌂  MENU',
                color: const Color(0xFF1E2A4A),
                onTap: () => Navigator.of(context).popUntil((r) => r.isFirst),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
