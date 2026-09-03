import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import 'constants.dart';
import 'game_engine.dart';

/// Renders the entire game scene directly from [GameEngine] state.
class GamePainter extends CustomPainter {
  GamePainter(this.engine, this.elapsedSeconds, {Listenable? repaint})
      : super(repaint: repaint);

  final GameEngine engine;

  /// Wall-clock seconds since the ticker started, used for star flicker.
  final double elapsedSeconds;

  @override
  void paint(Canvas canvas, Size size) {
    _drawBackground(canvas, size);
    _drawStars(canvas);
    _drawObstacles(canvas);
    _drawTrail(canvas);
    _drawParticles(canvas);

    if (engine.state != GameState.gameOver) {
      _drawPlayer(canvas);
    }

    _drawHud(canvas);
    _drawGravityIndicator(canvas);
    _drawFlash(canvas, size);

    switch (engine.state) {
      case GameState.ready:
        _drawReadyScreen(canvas, size);
        break;
      case GameState.paused:
        _drawPausedScreen(canvas, size);
        break;
      case GameState.gameOver:
        _drawGameOverScreen(canvas, size);
        break;
      case GameState.playing:
        break;
    }
  }

  @override
  bool shouldRepaint(covariant GamePainter oldDelegate) => true;

  // ── Scene ──────────────────────────────────────────────────────

  void _drawBackground(Canvas canvas, Size size) {
    canvas.drawRect(Offset.zero & size, Paint()..color = const Color(colorBg));

    // Subtle scanlines.
    final line = Paint()..color = const Color(0x08FFFFFF);
    for (double y = 0; y < size.height; y += 4) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), line);
    }
  }

  void _drawStars(Canvas canvas) {
    final paint = Paint();
    for (var i = 0; i < starCount; i++) {
      final flicker = 0.4 + 0.6 * math.sin(elapsedSeconds + i);
      final a = (flicker * 180).round();
      paint.color = Color(withAlpha(colorText, a));
      canvas.drawCircle(
          Offset(engine.starX[i], engine.starY[i]), engine.starR[i], paint);
    }
  }

  void _drawObstacles(Canvas canvas) {
    for (final o in engine.obstacles) {
      final rect = Rect.fromLTWH(o.x, o.y, o.w, o.h);

      // Glow.
      final glow = Paint()
        ..color = Color(withAlpha(o.color, 0x40))
        ..maskFilter = ui.MaskFilter.blur(ui.BlurStyle.normal, 20);
      canvas.drawRect(rect, glow);

      // Body.
      canvas.drawRRect(
        RRect.fromRectAndRadius(rect, const Radius.circular(8)),
        Paint()..color = Color(o.color),
      );

      // Shine strip.
      final shineRect = Rect.fromLTRB(
          rect.left + 4, rect.top + 4, rect.right - 4, rect.top + 10);
      canvas.drawRRect(
        RRect.fromRectAndRadius(shineRect, const Radius.circular(4)),
        Paint()..color = const Color(0x30FFFFFF),
      );
    }
  }

  void _drawTrail(Canvas canvas) {
    final paint = Paint()..maskFilter = ui.MaskFilter.blur(ui.BlurStyle.normal, 10);
    for (var i = 0; i < engine.trail.length; i++) {
      final pos = engine.trail[i];
      final frac = 1 - i / engine.trail.length;
      final r = playerRadius * frac * 0.7;
      final a = (frac * frac * 180).round();
      paint.color = Color(withAlpha(colorPlayer, a));
      canvas.drawCircle(Offset(pos[0], pos[1]), r, paint);
    }
  }

  void _drawParticles(Canvas canvas) {
    final paint = Paint();
    for (final p in engine.particles) {
      final a = (p.alpha * 255).round();
      paint.color = Color(withAlpha(p.color, a));
      canvas.drawCircle(Offset(p.x, p.y), p.radius * p.alpha, paint);
    }
  }

  void _drawPlayer(Canvas canvas) {
    final x = engine.px;
    final y = engine.py;
    final r = playerRadius;

    // Outer glow.
    final outer = Paint()
      ..color = const Color(colorGlow)
      ..maskFilter = ui.MaskFilter.blur(ui.BlurStyle.normal, 50);
    canvas.drawCircle(Offset(x, y), r * 1.6, outer);

    // Inner glow.
    final inner = Paint()
      ..color = const Color(colorGlow)
      ..maskFilter = ui.MaskFilter.blur(ui.BlurStyle.normal, 20);
    canvas.drawCircle(Offset(x, y), r * 1.2, inner);

    // Core.
    canvas.drawCircle(Offset(x, y), r, Paint()..color = const Color(colorPlayer));

    // Highlight.
    canvas.drawCircle(
      Offset(x - r * 0.3, y - r * 0.3),
      r * 0.35,
      Paint()..color = const Color(0x80FFFFFF),
    );

    _drawGravityArrow(canvas, x, y, r);
  }

  void _drawGravityArrow(Canvas canvas, double cx, double cy, double r) {
    canvas.save();
    canvas.translate(cx, cy);
    canvas.rotate((engine.gravityAngle - 90) * math.pi / 180);

    final arrowLen = r * 0.9;
    final arrow = Paint()
      ..color = const Color(0xCCFFFFFF)
      ..strokeWidth = 3
      ..style = ui.PaintingStyle.stroke
      ..strokeCap = ui.StrokeCap.round;
    canvas.drawLine(Offset(0, -arrowLen * 0.5), Offset(0, arrowLen), arrow);

    final head = Path()
      ..moveTo(0, arrowLen + 10)
      ..lineTo(-8, arrowLen - 4)
      ..lineTo(8, arrowLen - 4)
      ..close();
    canvas.drawPath(
      head,
      Paint()
        ..color = const Color(0xCCFFFFFF)
        ..style = ui.PaintingStyle.fill,
    );
    canvas.restore();
  }

  // ── HUD ────────────────────────────────────────────────────────

  void _drawHud(Canvas canvas) {
    if (engine.state != GameState.playing && engine.state != GameState.paused) {
      return;
    }

    final w = engine.screenW;
    _text(canvas, '${engine.score}', Offset(w / 2, 100), 68, colorScore, bold: true);

    final badge = Rect.fromLTRB(w / 2 - 60, 110, w / 2 + 60, 148);
    canvas.drawRRect(
      RRect.fromRectAndRadius(badge, const Radius.circular(20)),
      Paint()..color = const Color(0x80000000),
    );
    _text(canvas, 'LVL ${engine.level}', Offset(w / 2, 138), 28, colorAccent,
        bold: true);

    final pauseRect = engine.pauseButtonRect;
    canvas.drawRRect(
      RRect.fromRectAndRadius(pauseRect, const Radius.circular(16)),
      Paint()..color = const Color(0x80000000),
    );
    final cx = pauseRect.center.dx;
    final cy = pauseRect.center.dy;
    final iconPaint = Paint()..color = const Color(colorText);
    canvas.drawRRect(
      RRect.fromRectAndRadius(
          Rect.fromLTRB(cx - 16, cy - 18, cx - 4, cy + 18),
          const Radius.circular(4)),
      iconPaint,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(
          Rect.fromLTRB(cx + 4, cy - 18, cx + 16, cy + 18),
          const Radius.circular(4)),
      iconPaint,
    );
  }

  void _drawGravityIndicator(Canvas canvas) {
    if (engine.state != GameState.playing && engine.state != GameState.paused) {
      return;
    }

    final rect = engine.gravityIndicatorRect;
    canvas.drawRRect(
      RRect.fromRectAndRadius(rect, const Radius.circular(16)),
      Paint()..color = const Color(0x80000000),
    );

    final cx = rect.center.dx;
    final cy = rect.center.dy;
    canvas.save();
    canvas.translate(cx, cy);
    canvas.rotate((engine.gravityAngle - 90) * math.pi / 180);

    final arrow = Paint()
      ..color = const Color(colorPlayer)
      ..style = ui.PaintingStyle.stroke
      ..strokeWidth = 3
      ..strokeCap = ui.StrokeCap.round;
    canvas.drawLine(Offset(0, -25), Offset(0, 25), arrow);

    final head = Path()
      ..moveTo(0, 32)
      ..lineTo(-10, 18)
      ..lineTo(10, 18)
      ..close();
    canvas.drawPath(
      head,
      Paint()
        ..color = const Color(colorPlayer)
        ..style = ui.PaintingStyle.fill,
    );
    canvas.restore();

    _text(canvas, 'GRAVITY', Offset(cx, rect.bottom + 22), 18, 0xAAFFFFFF);
  }

  void _drawFlash(Canvas canvas, Size size) {
    if (engine.flashAlpha <= 0) return;
    final a = (engine.flashAlpha * 180).round();
    canvas.drawRect(
      Offset.zero & size,
      Paint()..color = Color(withAlpha(colorObs1, a)),
    );
  }

  // ── Overlays ───────────────────────────────────────────────────

  void _drawReadyScreen(Canvas canvas, Size size) {
    canvas.drawRect(Offset.zero & size, Paint()..color = const Color(0xAA000000));
    final w = engine.screenW;
    final h = engine.screenH;
    _text(canvas, 'NEURO', Offset(w / 2, h / 2 - 60), 80, colorPlayer, bold: true);
    _text(canvas, 'SHIFT', Offset(w / 2, h / 2 + 20), 80, colorPlayer, bold: true);
    _text(canvas, 'TAP TO START', Offset(w / 2, h / 2 + 100), 32, 0xAAFFFFFF);
    _text(canvas, 'TAP = ROTATE GRAVITY 90°', Offset(w / 2, h / 2 + 160), 26,
        0x88FFFFFF);
  }

  void _drawPausedScreen(Canvas canvas, Size size) {
    canvas.drawRect(Offset.zero & size, Paint()..color = const Color(0xBB000000));
    final w = engine.screenW;
    final h = engine.screenH;
    _text(canvas, 'PAUSED', Offset(w / 2, h / 2 - 40), 72, colorPlayer, bold: true);
    _text(canvas, 'TAP TO RESUME', Offset(w / 2, h / 2 + 50), 36, 0xAAFFFFFF);
  }

  void _drawGameOverScreen(Canvas canvas, Size size) {
    canvas.drawRect(Offset.zero & size, Paint()..color = const Color(0x99000000));
    final w = engine.screenW;
    final h = engine.screenH;
    _text(canvas, 'GAME OVER', Offset(w / 2, h / 2 - 40), 72, colorObs1,
        bold: true);
    _text(canvas, 'SCORE: ${engine.score}', Offset(w / 2, h / 2 + 50), 52,
        colorScore, bold: true);
  }

  // ── Text helper ────────────────────────────────────────────────

  void _text(
    Canvas canvas,
    String text,
    Offset center,
    double fontSize,
    int color, {
    bool bold = false,
  }) {
    final tp = TextPainter(
      text: TextSpan(
        text: text,
        style: TextStyle(
          color: Color(color),
          fontSize: fontSize,
          fontWeight: bold ? FontWeight.bold : FontWeight.normal,
          fontFamily: 'monospace',
          fontFamilyFallback: const ['Menlo', 'Consolas', 'Courier New', 'monospace'],
          letterSpacing: 1.2,
        ),
      ),
      textDirection: TextDirection.ltr,
      textAlign: TextAlign.center,
    )..layout();
    tp.paint(canvas, center - Offset(tp.width / 2, tp.height / 2));
  }
}
