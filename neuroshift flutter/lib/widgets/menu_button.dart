import 'package:flutter/material.dart';

import '../game/constants.dart';

/// Cross-platform fallbacks for the generic `monospace` family.
const List<String> kMonoFallback = [
  'Menlo',
  'Consolas',
  'Courier New',
  'monospace',
];

/// Shared monospace text style used across all NeuroShift screens.
TextStyle monoTextStyle({
  double? size,
  int color = colorText,
  bool bold = false,
  double letterSpacing = 1.2,
}) {
  return TextStyle(
    color: Color(color),
    fontSize: size,
    fontWeight: bold ? FontWeight.bold : FontWeight.normal,
    fontFamily: 'monospace',
    fontFamilyFallback: kMonoFallback,
    letterSpacing: letterSpacing,
  );
}

/// A reusable menu button matching the native app's look.
class MenuButton extends StatelessWidget {
  const MenuButton({
    super.key,
    required this.label,
    required this.color,
    required this.onTap,
    this.width = 220,
    this.height = 56,
    this.fontSize = 16,
  });

  final String label;
  final Color color;
  final VoidCallback onTap;
  final double width;
  final double height;
  final double fontSize;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: width,
      height: height,
      child: Material(
        color: color,
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: onTap,
          child: Center(
            child: Text(
              label,
              style: monoTextStyle(size: fontSize, color: colorText, bold: true),
            ),
          ),
        ),
      ),
    );
  }
}
