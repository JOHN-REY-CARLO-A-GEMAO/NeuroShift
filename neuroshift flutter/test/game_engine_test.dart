import 'dart:math';

import 'package:flutter_test/flutter_test.dart';

import 'package:neuroshift_flutter/game/constants.dart';
import 'package:neuroshift_flutter/game/game_engine.dart';
import 'package:neuroshift_flutter/game/models.dart';

void main() {
  GameEngine makeEngine() => GameEngine(random: Random(42))..init(400, 800);

  test('gravity rotates through the full 4-direction cycle', () {
    final e = makeEngine();
    expect(e.gravityDir, GravityDir.down);
    expect(e.targetAngle, 90);

    e.rotateGravity();
    expect(e.gravityDir, GravityDir.left);
    expect(e.targetAngle, 180);

    e.rotateGravity();
    expect(e.gravityDir, GravityDir.up);
    expect(e.targetAngle, 270);

    e.rotateGravity();
    expect(e.gravityDir, GravityDir.right);
    expect(e.targetAngle, 0);

    e.rotateGravity();
    expect(e.gravityDir, GravityDir.down);
    expect(e.targetAngle, 90);
  });

  test('score is derived from elapsed game time', () {
    final e = makeEngine();
    e.state = GameState.playing;
    e.update(1.0); // 1000 ms -> score 10
    expect(e.score, 10);
    e.update(0.5); // +500 ms -> 1500 ms -> score 15
    expect(e.score, 15);
  });

  test('difficulty level rises every 10 seconds and boosts speed', () {
    final e = makeEngine();
    e.state = GameState.playing;
    e.update(10.0);
    expect(e.level, 2);
    expect(e.speedMultiplier, 1.25);
  });

  test('player bounces off the left wall and reverses velocity', () {
    final e = makeEngine();
    e.state = GameState.playing;
    e.gravityDir = GravityDir.left;
    e.px = 10;
    e.py = 400;
    e.pvx = -600;
    e.pvy = 0;
    e.update(1 / 60);
    expect(e.px, greaterThanOrEqualTo(playerRadius));
    expect(e.pvx, greaterThan(0));
  });

  test('obstacle collision detection (circle vs rect)', () {
    final o = Obstacle(100, 100, 50, 50, 0, 0, 0xFF000000);
    expect(o.collidesWith(125, 125, 10), isTrue);
    expect(o.collidesWith(300, 300, 10), isFalse);
  });

  test('colliding with an obstacle triggers game over', () {
    var over = false;
    final e = GameEngine(random: Random(42), onGameOver: () => over = true)
      ..init(400, 800);
    e.state = GameState.playing;
    e.px = 200;
    e.py = 400;
    e.pvx = 0;
    e.pvy = 0;
    e.obstacles.add(Obstacle(e.px - 10, e.py - 10, 20, 20, 0, 0, 0xFF000000));
    e.update(1 / 60);
    expect(e.state, GameState.gameOver);
    expect(over, isTrue);
  });

  test('bouncing obstacle stays inside horizontal bounds', () {
    final b = BouncingObstacle(390, 100, 40, 20, 300, 0, 0xFF000000, 400);
    b.update(1 / 60, 1.0);
    expect(b.x + b.w, lessThanOrEqualTo(400.001));
    expect(b.vx, lessThan(0));
  });
}
