/// Plain-Dart game entities shared by the engine and the renderer.

/// A moving rectangular obstacle that drifts across the play field.
class Obstacle {
  Obstacle(this.x, this.y, this.w, this.h, this.vx, this.vy, this.color);

  double x;
  double y;
  double w;
  double h;
  double vx;
  double vy;
  final int color;

  void update(double dt, double speedMult) {
    x += vx * speedMult * dt;
    y += vy * speedMult * dt;
  }

  bool isOffScreen(double screenW, double screenH) {
    return x + w < -100 ||
        x > screenW + 100 ||
        y + h < -100 ||
        y > screenH + 100;
  }

  /// Circle vs. rounded-rectangle collision (clamp circle centre to the box).
  bool collidesWith(double cx, double cy, double radius) {
    final nearX = cx.clamp(x, x + w).toDouble();
    final nearY = cy.clamp(y, y + h).toDouble();
    final dx = cx - nearX;
    final dy = cy - nearY;
    return (dx * dx + dy * dy) < (radius * radius);
  }
}

/// An obstacle that bounces horizontally off the left/right walls.
class BouncingObstacle extends Obstacle {
  BouncingObstacle(
    super.x,
    super.y,
    super.w,
    super.h,
    super.vx,
    super.vy,
    super.color,
    this.screenW,
  );

  final double screenW;

  @override
  void update(double dt, double speedMult) {
    x += vx * speedMult * dt;
    y += vy * speedMult * dt;

    if (x < 0) {
      x = 0;
      vx = vx.abs();
    }
    if (x + w > screenW) {
      x = screenW - w;
      vx = -vx.abs();
    }
  }
}

/// A short-lived visual particle.
class Particle {
  Particle(this.x, this.y, this.vx, this.vy, this.color, this.radius, this.lifetime)
      : maxLifetime = lifetime;

  double x;
  double y;
  double vx;
  double vy;
  final int color;
  double radius;
  double lifetime;
  final double maxLifetime;

  double get alpha => lifetime <= 0 ? 0.0 : lifetime / maxLifetime;

  void update(double dt) {
    x += vx * dt;
    y += vy * dt;
    vx *= (1 - dt * 3); // drag
    vy *= (1 - dt * 3);
    lifetime -= dt;
  }

  bool get isDead => lifetime <= 0;
}
