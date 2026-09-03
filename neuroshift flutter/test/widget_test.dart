import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:neuroshift_flutter/main.dart';
import 'package:neuroshift_flutter/services/score_manager.dart';

void main() {
  testWidgets('main menu renders the NeuroShift title and actions',
      (tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scoreManager = ScoreManager(prefs);

    await tester.pumpWidget(NeuroShiftApp(scoreManager: scoreManager));
    await tester.pump();

    expect(find.text('NEUROSHIFT'), findsOneWidget);
    expect(find.textContaining('PLAY'), findsOneWidget);
    expect(find.textContaining('LEADERBOARD'), findsOneWidget);
    expect(find.textContaining('BEST: 0'), findsOneWidget);
  });

  test('scores persist and update the high score', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final scoreManager = ScoreManager(prefs);

    scoreManager.saveScore(120);
    scoreManager.saveScore(340);
    scoreManager.saveScore(90);

    expect(scoreManager.getHighScore(), 340);
    expect(scoreManager.getScoreList(), [340, 120, 90]);
  });
}
