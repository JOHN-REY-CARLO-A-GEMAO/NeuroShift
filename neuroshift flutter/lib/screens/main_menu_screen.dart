import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../game/constants.dart';
import '../services/score_manager.dart';
import '../widgets/menu_button.dart';
import 'game_screen.dart';
import 'leaderboard_screen.dart';

class MainMenuScreen extends StatefulWidget {
  const MainMenuScreen({super.key, required this.scoreManager});

  final ScoreManager scoreManager;

  @override
  State<MainMenuScreen> createState() => _MainMenuScreenState();
}

class _MainMenuScreenState extends State<MainMenuScreen> {
  Future<void> _open(Widget page) async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => page),
    );
    if (mounted) setState(() {}); // Refresh the BEST: label on return.
  }

  @override
  Widget build(BuildContext context) {
    final best = widget.scoreManager.getHighScore();
    return Scaffold(
      backgroundColor: const Color(colorBg),
      body: SafeArea(
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                'NEUROSHIFT',
                style: monoTextStyle(
                    size: 48, color: colorPlayer, bold: true, letterSpacing: 3),
              ),
              const SizedBox(height: 12),
              Text(
                'BEST: $best',
                style: monoTextStyle(size: 20, color: 0xAA00FFCC),
              ),
              const SizedBox(height: 44),
              MenuButton(
                label: '▶  PLAY',
                color: const Color(colorAccent),
                height: 60,
                fontSize: 20,
                onTap: () => _open(GameScreen(scoreManager: widget.scoreManager)),
              ),
              const SizedBox(height: 16),
              MenuButton(
                label: '🏆  LEADERBOARD',
                color: const Color(0xFF1E2A4A),
                onTap: () =>
                    _open(LeaderboardScreen(scoreManager: widget.scoreManager)),
              ),
              if (!kIsWeb) ...[
                const SizedBox(height: 12),
                MenuButton(
                  label: '✕  QUIT',
                  color: const Color(0xFF1E2A4A),
                  onTap: () => SystemNavigator.pop(),
                ),
              ],
              const SizedBox(height: 56),
              Text(
                'v1.0  •  TAP TO ROTATE GRAVITY',
                style: monoTextStyle(size: 12, color: 0x446C63FF, letterSpacing: 0.5),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
