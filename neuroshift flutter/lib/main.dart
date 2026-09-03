import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'game/constants.dart';
import 'screens/main_menu_screen.dart';
import 'services/score_manager.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final prefs = await SharedPreferences.getInstance();
  runApp(NeuroShiftApp(scoreManager: ScoreManager(prefs)));
}

class NeuroShiftApp extends StatelessWidget {
  const NeuroShiftApp({super.key, required this.scoreManager});

  final ScoreManager scoreManager;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NeuroShift',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(colorBg),
        colorScheme: const ColorScheme.dark(
          primary: Color(colorAccent),
          secondary: Color(colorPlayer),
        ),
        fontFamily: 'monospace',
      ),
      home: MainMenuScreen(scoreManager: scoreManager),
    );
  }
}
