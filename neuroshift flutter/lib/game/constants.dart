/// Shared tuning constants and color palette for NeuroShift.
///
/// Colors are stored as ARGB integers (matching the native Android port) so
/// the pure-Dart game engine never depends on Flutter types.
///
// ── Physics / gameplay tuning ──────────────────────────────────────
const double gravityForce = 1800.0;
const double maxSpeed = 900.0;
const double obstacleSpeed = 400.0;
const int particleCount = 60;
const double obstacleIntervalMs = 1800.0;
const int starCount = 80;
const int tapCooldownMs = 250;
const double playerRadius = 28.0;
const int trailLength = 18;
const int topScores = 10;

// ── Color palette (ARGB) ───────────────────────────────────────────
const int colorBg = 0xFF050A1A;
const int colorPlayer = 0xFF00FFCC;
const int colorGlow = 0x6000FFCC;
const int colorObs1 = 0xFFFF3366;
const int colorObs2 = 0xFFFF6633;
const int colorText = 0xFFFFFFFF;
const int colorAccent = 0xFF6C63FF;
const int colorScore = 0xFF00FFCC;

/// Returns [argb] (a 0xAARRGGBB value) with its alpha channel replaced by
/// [a] (0-255, clamped).
int withAlpha(int argb, int a) {
  final aa = a < 0 ? 0 : (a > 255 ? 255 : a);
  return (argb & 0x00FFFFFF) | (aa << 24);
}
