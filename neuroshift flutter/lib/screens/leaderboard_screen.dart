import 'package:flutter/material.dart';

import '../game/constants.dart';
import '../services/score_manager.dart';
import '../widgets/menu_button.dart';

class LeaderboardScreen extends StatelessWidget {
  const LeaderboardScreen({super.key, required this.scoreManager});

  final ScoreManager scoreManager;

  static const List<String> medals = ['🥇', '🥈', '🥉'];

  @override
  Widget build(BuildContext context) {
    final scores = scoreManager.getScoreList();
    return Scaffold(
      backgroundColor: const Color(colorBg),
      body: SafeArea(
        child: Column(
          children: [
            const SizedBox(height: 40),
            Text(
              'LEADERBOARD',
              style: monoTextStyle(size: 34, color: colorPlayer, bold: true),
            ),
            const SizedBox(height: 32),
            Expanded(
              child: scores.isEmpty
                  ? Center(
                      child: Text(
                        'No scores yet!\nPlay to set a record.',
                        textAlign: TextAlign.center,
                        style: monoTextStyle(size: 20, color: 0xAAFFFFFF),
                      ),
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.symmetric(horizontal: 32),
                      itemCount: scores.length,
                      itemBuilder: (context, i) {
                        final medal = i < medals.length ? medals[i] : '  ';
                        return Padding(
                          padding: const EdgeInsets.symmetric(vertical: 9),
                          child: Text(
                            '$medal  #${i + 1}   ${scores[i]}',
                            style: monoTextStyle(size: 24, color: colorText),
                          ),
                        );
                      },
                    ),
            ),
            Padding(
              padding: const EdgeInsets.only(top: 24, bottom: 24),
              child: MenuButton(
                label: '← BACK',
                color: const Color(colorAccent),
                onTap: () => Navigator.of(context).pop(),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
