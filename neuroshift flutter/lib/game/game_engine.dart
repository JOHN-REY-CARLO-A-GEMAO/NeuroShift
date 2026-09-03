import 'dart:math';
import 'dart:ui' show Rect;

import 'constants.dart';
import 'models.dart';

enum GameState { ready, playing, paused, gameOver }

enum GravityDir { down, left, up, right }

/// Pure-Dart game simulation for NeuroShift.
///
/// Mirrors the native Android `GameView` logic: a gravity-driven ball falls
/// toward the current gravity direction, tapping rotates gravity by 90°, and
/// the player must dodge incoming obstacles for as long as possible.
class GameEngine {
  GameEngine({Random? random, this.onGameOver}) : _random = random ?? Random();

  final Random _random;

  /// Invoked exactly once when the player collides with an obstacle.
  void Function()? onGameOver;

  // ── Screen ─────────────────────────────────────────────────────
  double screenW = 0;
  double screenH = 0;

  // ── Game state ─────────────────────────────────────────────────
  GameState state = GameState.ready;
  GravityDir gravityDir = GravityDir.down;
  int score = 0;
  int level = 1;
  double gameTimeMs = 0;
  double lastObstacleTime = 0;
  double speedMultiplier = 1;

  // ── Player ─────────────────────────────────────────────────────
  double px = 0;
  double py = 0;
  double pvx = 0;
  double pvy = 0;
  final List<List<double>> trail = [];

  // ── Entities ───────────────────────────────────────────────────
  final List<Obstacle> obstacles = [];
  final List<Particle> particles = [];

  // ── Background stars ───────────────────────────────────────────
  late List<double> starX;
  late List<double> starY;
  late List<double> starR;

  // ── Gravity transition ─────────────────────────────────────────
  double gravityAngle = 90; // rendered angle (degrees), 90 == down
  double targetAngle = 90;
  bool isRotating = false;

  // ── Flash effect ───────────────────────────────────────────────
  double flashAlpha = 0;

  // ── Hit areas (same layout as the native app) ──────────────────
  Rect get pauseButtonRect => Rect.fromLTWH(screenW - 110, 30, 90, 80);
  Rect get gravityIndicatorRect => Rect.fromLTWH(20, 30, 100, 100);

  void init(double w, double h) {
    screenW = w;
    screenH = h;
    px = w / 2;
    py = h / 2;
    pvx = 0;
    pvy = 0;

    state = GameState.ready;
    gravityDir = GravityDir.down;
    gravityAngle = 90;
    targetAngle = 90;
    isRotating = false;
    score = 0;
    level = 1;
    gameTimeMs = 0;
    speedMultiplier = 1;
    lastObstacleTime = 0;
    flashAlpha = 0;

    obstacles.clear();
    particles.clear();
    trail.clear();

    starX = List<double>.filled(starCount, 0);
    starY = List<double>.filled(starCount, 0);
    starR = List<double>.filled(starCount, 0);
    for (var i = 0; i < starCount; i++) {
      starX[i] = _random.nextDouble() * w;
      starY[i] = _random.nextDouble() * h;
      starR[i] = _random.nextDouble() * 2.5 + 0.5;
    }
  }

  /// Keeps the play field in sync after a resize/rotation while preserving
  /// the running game (the player is clamped back inside the new bounds).
  void setScreenSize(double w, double h) {
    if ((w - screenW).abs() < 0.5 && (h - screenH).abs() < 0.5) return;
    screenW = w;
    screenH = h;
    if (px < playerRadius) px = playerRadius;
    if (px > screenW - playerRadius) px = screenW - playerRadius;
    if (py < playerRadius) py = playerRadius;
    if (py > screenH - playerRadius) py = screenH - playerRadius;
  }

  void update(double dt) {
    if (state != GameState.playing) return;

    gameTimeMs += dt * 1000;
    _updateDifficulty();
    _updateGravityRotation(dt);
    _updatePlayer(dt);
    _updateObstacles(dt);
    _updateParticles(dt);
    _updateTrail();
    _spawnObstacles();
    _checkCollisions();
    _updateFlash(dt);

    score = (gameTimeMs / 100).floor();
  }

  void rotateGravity() {
    switch (gravityDir) {
      case GravityDir.down:
        gravityDir = GravityDir.left;
        targetAngle = 180;
        break;
      case GravityDir.left:
        gravityDir = GravityDir.up;
        targetAngle = 270;
        break;
      case GravityDir.up:
        gravityDir = GravityDir.right;
        targetAngle = 0;
        break;
      case GravityDir.right:
        gravityDir = GravityDir.down;
        targetAngle = 90;
        break;
    }

    // Kill velocity along the new gravity axis.
    switch (gravityDir) {
      case GravityDir.down:
      case GravityDir.up:
        pvx *= 0.3;
        break;
      case GravityDir.left:
      case GravityDir.right:
        pvy *= 0.3;
        break;
    }

    isRotating = true;
    _spawnGravityParticles();
  }

  // ── Update helpers ─────────────────────────────────────────────

  void _updateDifficulty() {
    level = (gameTimeMs / 10000).floor() + 1;
    speedMultiplier = 1 + (level - 1) * 0.25;
  }

  void _updateGravityRotation(double dt) {
    if (!isRotating) return;
    var diff = targetAngle - gravityAngle;
    // Shortest path around the circle.
    if (diff > 180) diff -= 360;
    if (diff < -180) diff += 360;

    final step = diff * dt * 8;
    if (diff.abs() < 2) {
      gravityAngle = targetAngle;
      isRotating = false;
    } else {
      gravityAngle += step;
    }
  }

  void _updatePlayer(double dt) {
    double gx = 0;
    double gy = 0;
    switch (gravityDir) {
      case GravityDir.down:
        gy = gravityForce;
        break;
      case GravityDir.up:
        gy = -gravityForce;
        break;
      case GravityDir.left:
        gx = -gravityForce;
        break;
      case GravityDir.right:
        gx = gravityForce;
        break;
    }

    pvx += gx * dt;
    pvy += gy * dt;

    pvx = pvx.clamp(-maxSpeed, maxSpeed).toDouble();
    pvy = pvy.clamp(-maxSpeed, maxSpeed).toDouble();

    px += pvx * dt;
    py += pvy * dt;

    // Wall bounce with damping.
    if (px - playerRadius < 0) {
      px = playerRadius;
      pvx = pvx.abs() * 0.4;
      _spawnImpact(px, py, 8);
    }
    if (px + playerRadius > screenW) {
      px = screenW - playerRadius;
      pvx = -pvx.abs() * 0.4;
      _spawnImpact(px, py, 8);
    }
    if (py - playerRadius < 0) {
      py = playerRadius;
      pvy = pvy.abs() * 0.4;
      _spawnImpact(px, py, 8);
    }
    if (py + playerRadius > screenH) {
      py = screenH - playerRadius;
      pvy = -pvy.abs() * 0.4;
      _spawnImpact(px, py, 8);
    }
  }

  void _updateObstacles(double dt) {
    for (final o in obstacles) {
      o.update(dt, speedMultiplier);
    }
    obstacles.removeWhere((o) => o.isOffScreen(screenW, screenH));
  }

  void _updateParticles(double dt) {
    for (final p in particles) {
      p.update(dt);
    }
    particles.removeWhere((p) => p.isDead);
  }

  void _updateTrail() {
    trail.insert(0, [px, py]);
    if (trail.length > trailLength) {
      trail.removeAt(trail.length - 1);
    }
  }

  void _updateFlash(double dt) {
    if (flashAlpha > 0) {
      flashAlpha -= dt * 2.5;
      if (flashAlpha < 0) flashAlpha = 0;
    }
  }

  // ── Spawning ───────────────────────────────────────────────────

  void _spawnObstacles() {
    final interval = obstacleIntervalMs / speedMultiplier;
    if (gameTimeMs - lastObstacleTime < interval) return;
    lastObstacleTime = gameTimeMs;

    switch (_random.nextInt(4)) {
      case 0:
        _spawnHorizontal();
        break;
      case 1:
        _spawnVertical();
        break;
      case 2:
        _spawnCorner();
        break;
      case 3:
        _spawnMoving();
        break;
    }
  }

  void _spawnHorizontal() {
    final gapW = screenW / 3;
    final gapX = _random.nextInt((screenW - gapW).round());
    final h = 25 + _random.nextInt(30);
    final fromTop = _random.nextBool();
    final speed = obstacleSpeed;

    if (fromTop) {
      if (gapX > 20) {
        obstacles.add(Obstacle(0, -h.toDouble(), gapX.toDouble(), h.toDouble(),
            speed, 0, _obsColor()));
      }
      if (gapX + gapW < screenW - 20) {
        obstacles.add(Obstacle(gapX + gapW, -h.toDouble(),
            screenW - gapX - gapW, h.toDouble(), speed, 0, _obsColor()));
      }
    } else {
      if (gapX > 20) {
        obstacles.add(Obstacle(0, screenH, gapX.toDouble(), h.toDouble(),
            -speed, 0, _obsColor()));
      }
      if (gapX + gapW < screenW - 20) {
        obstacles.add(Obstacle(gapX + gapW, screenH, screenW - gapX - gapW,
            h.toDouble(), -speed, 0, _obsColor()));
      }
    }
  }

  void _spawnVertical() {
    final gapH = screenH / 4;
    final gapY = _random.nextInt((screenH - gapH).round());
    final w = 25 + _random.nextInt(30);
    final fromLeft = _random.nextBool();
    final speed = obstacleSpeed;

    if (fromLeft) {
      if (gapY > 20) {
        obstacles.add(Obstacle(-w.toDouble(), 0, w.toDouble(), gapY.toDouble(),
            0, speed, _obsColor()));
      }
      if (gapY + gapH < screenH - 20) {
        obstacles.add(Obstacle(-w.toDouble(), gapY + gapH, w.toDouble(),
            screenH - gapY - gapH, 0, speed, _obsColor()));
      }
    } else {
      if (gapY > 20) {
        obstacles.add(Obstacle(screenW, 0, w.toDouble(), gapY.toDouble(), 0,
            -speed, _obsColor()));
      }
      if (gapY + gapH < screenH - 20) {
        obstacles.add(Obstacle(screenW, gapY + gapH, w.toDouble(),
            screenH - gapY - gapH, 0, -speed, _obsColor()));
      }
    }
  }

  void _spawnCorner() {
    final corner = _random.nextInt(4);
    final size = (80 + _random.nextInt(60)).toDouble();
    final half = obstacleSpeed / 2;

    switch (corner) {
      case 0:
        obstacles.add(Obstacle(-size, -size, size, size, half, half, _obsColor()));
        break;
      case 1:
        obstacles.add(Obstacle(screenW, -size, size, size, -half, half, _obsColor()));
        break;
      case 2:
        obstacles.add(Obstacle(-size, screenH, size, size, half, -half, _obsColor()));
        break;
      case 3:
        obstacles.add(Obstacle(screenW, screenH, size, size, -half, -half, _obsColor()));
        break;
    }
  }

  void _spawnMoving() {
    final x = _random.nextInt((screenW - 80).round()).toDouble();
    const y = -60.0;
    final w = (60 + _random.nextInt(60)).toDouble();
    final h = (20 + _random.nextInt(20)).toDouble();
    final vx = (_random.nextBool() ? 1.0 : -1.0) * (200 + _random.nextInt(200));
    obstacles.add(
        BouncingObstacle(x, y, w, h, vx, obstacleSpeed, _obsColor(), screenW));
  }

  int _obsColor() => _random.nextBool() ? colorObs1 : colorObs2;

  // ── Collisions ─────────────────────────────────────────────────

  void _checkCollisions() {
    for (final o in obstacles) {
      if (o.collidesWith(px, py, playerRadius)) {
        _triggerGameOver();
        return;
      }
    }
  }

  void _triggerGameOver() {
    state = GameState.gameOver;
    flashAlpha = 1;
    _spawnDeathParticles();
    onGameOver?.call();
  }

  // ── Particles ──────────────────────────────────────────────────

  void _spawnImpact(double x, double y, int count) {
    for (var i = 0; i < count; i++) {
      final angle = _random.nextDouble() * 360;
      final speed = 100 + _random.nextDouble() * 300;
      final vx = cos(angle * pi / 180) * speed;
      final vy = sin(angle * pi / 180) * speed;
      particles.add(
          Particle(x, y, vx, vy, colorPlayer, 3 + _random.nextDouble() * 4, 0.4));
    }
  }

  void _spawnDeathParticles() {
    for (var i = 0; i < particleCount; i++) {
      final angle = _random.nextDouble() * 360;
      final speed = 200 + _random.nextDouble() * 600;
      final vx = cos(angle * pi / 180) * speed;
      final vy = sin(angle * pi / 180) * speed;
      final color = _random.nextBool() ? colorPlayer : colorObs1;
      particles.add(Particle(px, py, vx, vy, color, 4 + _random.nextDouble() * 8, 0.8));
    }
  }

  void _spawnGravityParticles() {
    for (var i = 0; i < 12; i++) {
      final angle = _random.nextDouble() * 360;
      final speed = 80 + _random.nextDouble() * 200;
      final vx = cos(angle * pi / 180) * speed;
      final vy = sin(angle * pi / 180) * speed;
      particles.add(
          Particle(px, py, vx, vy, colorAccent, 3 + _random.nextDouble() * 5, 0.5));
    }
  }
}
