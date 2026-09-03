import 'package:shared_preferences/shared_preferences.dart';

import '../game/constants.dart';

/// Persists the high score and the top score list, mirroring the native
/// Android `ScoreManager` (SharedPreferences-backed).
class ScoreManager {
  ScoreManager(this._prefs);

  static const String _keyHigh = 'high_score';
  static const String _keyList = 'score_list';

  final SharedPreferences _prefs;

  void saveScore(int score) {
    if (score > getHighScore()) {
      _prefs.setInt(_keyHigh, score);
    }

    final scores = getScoreList()..add(score);
    scores.sort((a, b) => b.compareTo(a));
    final trimmed =
        scores.length > topScores ? scores.sublist(0, topScores) : scores;
    _prefs.setString(_keyList, trimmed.join(','));
  }

  int getHighScore() => _prefs.getInt(_keyHigh) ?? 0;

  List<int> getScoreList() {
    final raw = _prefs.getString(_keyList) ?? '';
    if (raw.isEmpty) return [];
    return raw
        .split(',')
        .map((s) => int.tryParse(s.trim()))
        .whereType<int>()
        .toList();
  }
}
