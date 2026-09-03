package com.neuroshift.game;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    // Game States
    public enum GameState {
        READY, PLAYING, PAUSED, GAME_OVER
    }

    // Gravity Directions
    public enum GravityDir {
        DOWN, LEFT, UP, RIGHT
    }

    // ─── Constants ────────────────────────────────────────────────
    private static final float GRAVITY_FORCE    = 1800f;
    private static final float MAX_SPEED        = 900f;
    private static final int   OBSTACLE_SPEED   = 400;
    private static final int   PARTICLE_COUNT   = 60;
    private static final long  OBSTACLE_INTERVAL= 1800;

    // ─── Core ─────────────────────────────────────────────────────
    private GameLoop gameLoop;
    private SurfaceHolder holder;
    private Context context;
    private Random random = new Random();

    // ─── Game State ───────────────────────────────────────────────
    private GameState state = GameState.READY;
    private GravityDir gravityDir = GravityDir.DOWN;
    private int score = 0;
    private int level = 1;
    private long gameTime = 0;
    private long lastObstacleTime = 0;
    private float speedMultiplier = 1.0f;

    // ─── Screen ───────────────────────────────────────────────────
    private int screenW, screenH;

    // ─── Player ───────────────────────────────────────────────────
    private float px, py;          // position
    private float pvx, pvy;        // velocity
    private float playerRadius = 28f;
    private float trailAlpha = 0f;
    private List<float[]> trail = new ArrayList<>();

    // ─── Obstacles ────────────────────────────────────────────────
    private List<Obstacle> obstacles = new ArrayList<>();

    // ─── Particles ────────────────────────────────────────────────
    private final List<Particle> particles = new ArrayList<>();

    // ─── Background Stars ─────────────────────────────────────────
    private float[] starX, starY, starR;
    private static final int STAR_COUNT = 80;

    // ─── Paints ───────────────────────────────────────────────────
    private Paint bgPaint        = new Paint();
    private Paint playerPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint glowPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint obstaclePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint trailPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint textPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint scorePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint uiPaint        = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint starPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint arrowPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint particlePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ─── Colors ───────────────────────────────────────────────────
    private static final int COLOR_BG      = 0xFF050A1A;
    private static final int COLOR_PLAYER  = 0xFF00FFCC;
    private static final int COLOR_GLOW    = 0x6000FFCC;
    private static final int COLOR_OBS1    = 0xFFFF3366;
    private static final int COLOR_OBS2    = 0xFFFF6633;
    private static final int COLOR_TEXT    = 0xFFFFFFFF;
    private static final int COLOR_ACCENT  = 0xFF6C63FF;
    private static final int COLOR_SCORE   = 0xFF00FFCC;

    // ─── Touch ────────────────────────────────────────────────────
    private long lastTapTime = 0;
    private static final long TAP_COOLDOWN = 250;

    // ─── Score Manager ────────────────────────────────────────────
    private ScoreManager scoreManager;

    // ─── Gravity Transition ───────────────────────────────────────
    private float gravityAngle    = 90f;  // current rendered angle
    private float targetAngle     = 90f;
    private boolean isRotating    = false;

    // ─── Flash Effect ─────────────────────────────────────────────
    private float flashAlpha = 0f;

    // ─── UI Button Rects ─────────────────────────────────────────
    private RectF pauseButtonRect;
    private RectF gravityIndicatorRect;

    public GameView(Context context) {
        super(context);
        this.context = context;
        holder = getHolder();
        holder.addCallback(this);
        scoreManager = new ScoreManager(context);
        setFocusable(true);
    }

    // ════════════════════════════════════════════════════════════════
    //  SurfaceHolder Callbacks
    // ════════════════════════════════════════════════════════════════

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenW = getWidth();
        screenH = getHeight();
        initGame();
        gameLoop = new GameLoop(this, holder);
        gameLoop.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        screenW = w;
        screenH = h;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    // ════════════════════════════════════════════════════════════════
    //  Initialization
    // ════════════════════════════════════════════════════════════════

    private void initGame() {
        // Player start position
        px  = screenW / 2f;
        py  = screenH / 2f;
        pvx = 0f;
        pvy = 0f;

        // Reset state
        state           = GameState.READY;
        gravityDir      = GravityDir.DOWN;
        gravityAngle    = 90f;
        targetAngle     = 90f;
        isRotating      = false;
        score           = 0;
        level           = 1;
        gameTime        = 0;
        speedMultiplier = 1.0f;
        lastObstacleTime= 0;
        flashAlpha      = 0f;

        obstacles.clear();
        particles.clear();
        trail.clear();

        // Stars
        starX = new float[STAR_COUNT];
        starY = new float[STAR_COUNT];
        starR = new float[STAR_COUNT];
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = random.nextFloat() * screenW;
            starY[i] = random.nextFloat() * screenH;
            starR[i] = random.nextFloat() * 2.5f + 0.5f;
        }

        // UI Rects
        pauseButtonRect      = new RectF(screenW - 110, 30, screenW - 20, 110);
        gravityIndicatorRect = new RectF(20, 30, 120, 130);

        setupPaints();
    }

    private void setupPaints() {
        bgPaint.setColor(COLOR_BG);
        bgPaint.setStyle(Paint.Style.FILL);

        playerPaint.setColor(COLOR_PLAYER);
        playerPaint.setStyle(Paint.Style.FILL);

        glowPaint.setColor(COLOR_GLOW);
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setMaskFilter(new BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL));

        obstaclePaint.setStyle(Paint.Style.FILL);

        trailPaint.setStyle(Paint.Style.FILL);
        trailPaint.setMaskFilter(new BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL));

        textPaint.setColor(COLOR_TEXT);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setTextSize(52f);

        scorePaint.setColor(COLOR_SCORE);
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        scorePaint.setTextSize(72f);

        starPaint.setColor(0xFFFFFFFF);
        starPaint.setStyle(Paint.Style.FILL);

        arrowPaint.setColor(COLOR_PLAYER);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setStrokeWidth(4f);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);

        particlePaint.setStyle(Paint.Style.FILL);

        uiPaint.setStyle(Paint.Style.FILL);
    }

    // ════════════════════════════════════════════════════════════════
    //  Game Loop
    // ════════════════════════════════════════════════════════════════

    public void update(float dt) {
        if (state != GameState.PLAYING) return;

        gameTime += (long)(dt * 1000);
        updateDifficulty();
        updateGravityRotation(dt);
        updatePlayer(dt);
        updateObstacles(dt);
        updateParticles(dt);
        updateTrail();
        spawnObstacles();
        checkCollisions();
        updateFlash(dt);

        score = (int)(gameTime / 100);
    }

    private void updateDifficulty() {
        level = (int)(gameTime / 10000) + 1;
        speedMultiplier = 1.0f + (level - 1) * 0.25f;
    }

    private void updateGravityRotation(float dt) {
        if (isRotating) {
            float diff = targetAngle - gravityAngle;
            // Shortest path
            if (diff > 180)  diff -= 360;
            if (diff < -180) diff += 360;

            float step = diff * dt * 8f;
            if (Math.abs(diff) < 2f) {
                gravityAngle = targetAngle;
                isRotating   = false;
            } else {
                gravityAngle += step;
            }
        }
    }

    private void updatePlayer(float dt) {
        float gx = 0, gy = 0;
        switch (gravityDir) {
            case DOWN:  gy =  GRAVITY_FORCE; break;
            case UP:    gy = -GRAVITY_FORCE; break;
            case LEFT:  gx = -GRAVITY_FORCE; break;
            case RIGHT: gx =  GRAVITY_FORCE; break;
        }

        pvx += gx * dt;
        pvy += gy * dt;

        // Cap speed
        pvx = Math.max(-MAX_SPEED, Math.min(MAX_SPEED, pvx));
        pvy = Math.max(-MAX_SPEED, Math.min(MAX_SPEED, pvy));

        px += pvx * dt;
        py += pvy * dt;

        // Wall bounce with damping
        if (px - playerRadius < 0) {
            px  = playerRadius;
            pvx = Math.abs(pvx) * 0.4f;
            spawnImpactParticles(px, py, 8);
        }
        if (px + playerRadius > screenW) {
            px  = screenW - playerRadius;
            pvx = -Math.abs(pvx) * 0.4f;
            spawnImpactParticles(px, py, 8);
        }
        if (py - playerRadius < 0) {
            py  = playerRadius;
            pvy = Math.abs(pvy) * 0.4f;
            spawnImpactParticles(px, py, 8);
        }
        if (py + playerRadius > screenH) {
            py  = screenH - playerRadius;
            pvy = -Math.abs(pvy) * 0.4f;
            spawnImpactParticles(px, py, 8);
        }
    }

    private void updateObstacles(float dt) {
        for (Obstacle obs : obstacles) {
            obs.update(dt, speedMultiplier);
        }
        obstacles.removeIf(obs -> obs.isOffScreen(screenW, screenH));
    }

    private void updateParticles(float dt) {
        synchronized (particles) {
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.update(dt);
                if (p.isDead()) it.remove();
            }
        }
    }

    private void updateTrail() {
        trail.add(0, new float[]{px, py});
        if (trail.size() > 18) trail.remove(trail.size() - 1);
    }

    private void spawnObstacles() {
        long now = System.currentTimeMillis();
        long interval = (long)(OBSTACLE_INTERVAL / speedMultiplier);
        if (now - lastObstacleTime < interval) return;
        lastObstacleTime = now;

        int type = random.nextInt(4);
        switch (type) {
            case 0: spawnHorizontalObstacle(); break;
            case 1: spawnVerticalObstacle();   break;
            case 2: spawnCornerObstacle();     break;
            case 3: spawnMovingObstacle();     break;
        }
    }

    private void spawnHorizontalObstacle() {
        int gapW = screenW / 3;
        int gapX = random.nextInt(screenW - gapW);
        int h    = 25 + random.nextInt(30);
        boolean fromTop = random.nextBoolean();

        if (fromTop) {
            // Top bar
            if (gapX > 20)
                obstacles.add(new Obstacle(0, -h, gapX, h, OBSTACLE_SPEED, 0, getObsColor()));
            // Right bar
            if (gapX + gapW < screenW - 20)
                obstacles.add(new Obstacle(gapX + gapW, -h, screenW - gapX - gapW, h, OBSTACLE_SPEED, 0, getObsColor()));
        } else {
            // Bottom spawn
            if (gapX > 20)
                obstacles.add(new Obstacle(0, screenH, gapX, h, -OBSTACLE_SPEED, 0, getObsColor()));
            if (gapX + gapW < screenW - 20)
                obstacles.add(new Obstacle(gapX + gapW, screenH, screenW - gapX - gapW, h, -OBSTACLE_SPEED, 0, getObsColor()));
        }
    }

    private void spawnVerticalObstacle() {
        int gapH = screenH / 4;
        int gapY = random.nextInt(screenH - gapH);
        int w    = 25 + random.nextInt(30);
        boolean fromLeft = random.nextBoolean();

        if (fromLeft) {
            if (gapY > 20)
                obstacles.add(new Obstacle(-w, 0, w, gapY, 0, OBSTACLE_SPEED, getObsColor()));
            if (gapY + gapH < screenH - 20)
                obstacles.add(new Obstacle(-w, gapY + gapH, w, screenH - gapY - gapH, 0, OBSTACLE_SPEED, getObsColor()));
        } else {
            if (gapY > 20)
                obstacles.add(new Obstacle(screenW, 0, w, gapY, 0, -OBSTACLE_SPEED, getObsColor()));
            if (gapY + gapH < screenH - 20)
                obstacles.add(new Obstacle(screenW, gapY + gapH, w, screenH - gapY - gapH, 0, -OBSTACLE_SPEED, getObsColor()));
        }
    }

    private void spawnCornerObstacle() {
        int corner = random.nextInt(4);
        int size   = 80 + random.nextInt(60);
        switch (corner) {
            case 0: obstacles.add(new Obstacle(-size, -size, size, size, OBSTACLE_SPEED/2, OBSTACLE_SPEED/2, getObsColor())); break;
            case 1: obstacles.add(new Obstacle(screenW, -size, size, size, -OBSTACLE_SPEED/2, OBSTACLE_SPEED/2, getObsColor())); break;
            case 2: obstacles.add(new Obstacle(-size, screenH, size, size, OBSTACLE_SPEED/2, -OBSTACLE_SPEED/2, getObsColor())); break;
            case 3: obstacles.add(new Obstacle(screenW, screenH, size, size, -OBSTACLE_SPEED/2, -OBSTACLE_SPEED/2, getObsColor())); break;
        }
    }

    private void spawnMovingObstacle() {
        // Bouncing obstacle
        int x = random.nextInt(screenW - 80);
        int y = -60;
        int w = 60 + random.nextInt(60);
        int h = 20 + random.nextInt(20);
        obstacles.add(new BouncingObstacle(x, y, w, h,
            (random.nextBoolean() ? 1 : -1) * (200 + random.nextInt(200)),
            OBSTACLE_SPEED,
            getObsColor(), screenW));
    }

    private int getObsColor() {
        return random.nextBoolean() ? COLOR_OBS1 : COLOR_OBS2;
    }

    private void checkCollisions() {
        for (Obstacle obs : obstacles) {
            if (obs.collidesWith(px, py, playerRadius)) {
                triggerGameOver();
                return;
            }
        }
    }

    private void triggerGameOver() {
        state = GameState.GAME_OVER;
        flashAlpha = 1.0f;
        spawnDeathParticles();
        scoreManager.saveScore(score);

        // Navigate to GameOver screen after short delay
        postDelayed(() -> {
            Intent intent = new Intent(context, GameOverActivity.class);
            intent.putExtra("score", score);
            intent.putExtra("level", level);
            context.startActivity(intent);
        }, 1200);
    }

    private void updateFlash(float dt) {
        if (flashAlpha > 0) {
            flashAlpha -= dt * 2.5f;
            if (flashAlpha < 0) flashAlpha = 0;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Particles
    // ════════════════════════════════════════════════════════════════

    private void spawnImpactParticles(float x, float y, int count) {
        synchronized (particles) {
            for (int i = 0; i < count; i++) {
                float angle = random.nextFloat() * 360f;
                float speed = 100 + random.nextFloat() * 300;
                float vx = (float) (Math.cos(Math.toRadians(angle)) * speed);
                float vy = (float) (Math.sin(Math.toRadians(angle)) * speed);
                particles.add(new Particle(x, y, vx, vy, COLOR_PLAYER, 3f + random.nextFloat() * 4f, 0.4f));
            }
        }
    }

    private void spawnDeathParticles() {
        synchronized (particles) {
            for (int i = 0; i < 60; i++) {
                float angle = random.nextFloat() * 360f;
                float speed = 200 + random.nextFloat() * 600;
                float vx = (float) (Math.cos(Math.toRadians(angle)) * speed);
                float vy = (float) (Math.sin(Math.toRadians(angle)) * speed);
                int color = random.nextBoolean() ? COLOR_PLAYER : COLOR_OBS1;
                particles.add(new Particle(px, py, vx, vy, color, 4f + random.nextFloat() * 8f, 0.8f));
            }
        }
    }

    private void spawnGravityParticles() {
        synchronized (particles) {
            for (int i = 0; i < 12; i++) {
                float angle = random.nextFloat() * 360f;
                float speed = 80 + random.nextFloat() * 200;
                float vx = (float) (Math.cos(Math.toRadians(angle)) * speed);
                float vy = (float) (Math.sin(Math.toRadians(angle)) * speed);
                particles.add(new Particle(px, py, vx, vy, COLOR_ACCENT, 3f + random.nextFloat() * 5f, 0.5f));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Rendering
    // ════════════════════════════════════════════════════════════════

    public void render(Canvas canvas) {
        if (canvas == null) return;

        drawBackground(canvas);
        drawStars(canvas);
        drawObstacles(canvas);
        drawTrail(canvas);
        drawParticles(canvas);

        if (state != GameState.GAME_OVER) {
            drawPlayer(canvas);
        }

        drawHUD(canvas);
        drawGravityIndicator(canvas);
        drawFlash(canvas);

        switch (state) {
            case READY:     drawReadyScreen(canvas);    break;
            case PAUSED:    drawPausedScreen(canvas);   break;
            case GAME_OVER: drawGameOverScreen(canvas); break;
        }
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawRect(0, 0, screenW, screenH, bgPaint);

        // Gradient lines (scanlines effect)
        Paint linePaint = new Paint();
        linePaint.setColor(0x08FFFFFF);
        for (int y = 0; y < screenH; y += 4) {
            canvas.drawLine(0, y, screenW, y, linePaint);
        }
    }

    private void drawStars(Canvas canvas) {
        for (int i = 0; i < STAR_COUNT; i++) {
            float flicker = 0.4f + 0.6f * (float)Math.sin(System.currentTimeMillis() * 0.001 + i);
            starPaint.setAlpha((int)(flicker * 180));
            canvas.drawCircle(starX[i], starY[i], starR[i], starPaint);
        }
    }

    private void drawObstacles(Canvas canvas) {
        for (Obstacle obs : obstacles) {
            // Glow
            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setColor(obs.color & 0x00FFFFFF | 0x40000000);
            glow.setMaskFilter(new BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL));
            canvas.drawRect(obs.getRect(), glow);

            // Body
            obstaclePaint.setColor(obs.color);
            canvas.drawRoundRect(obs.getRect(), 8f, 8f, obstaclePaint);

            // Shine
            Paint shine = new Paint();
            shine.setColor(0x30FFFFFF);
            RectF shineRect = new RectF(
                obs.getRect().left + 4,
                obs.getRect().top + 4,
                obs.getRect().right - 4,
                obs.getRect().top + 10
            );
            canvas.drawRoundRect(shineRect, 4f, 4f, shine);
        }
    }

    private void drawTrail(Canvas canvas) {
        for (int i = 0; i < trail.size(); i++) {
            float[] pos  = trail.get(i);
            float   frac = 1f - (float)i / trail.size();
            float   r    = playerRadius * frac * 0.7f;
            int     alpha= (int)(frac * frac * 180);
            trailPaint.setColor(COLOR_PLAYER & 0x00FFFFFF | (alpha << 24));
            canvas.drawCircle(pos[0], pos[1], r, trailPaint);
        }
    }

    private void drawParticles(Canvas canvas) {
        synchronized (particles) {
            for (Particle p : particles) {
                particlePaint.setColor(p.color & 0x00FFFFFF | ((int) (p.alpha * 255) << 24));
                canvas.drawCircle(p.x, p.y, p.radius * p.alpha, particlePaint);
            }
        }
    }

    private void drawPlayer(Canvas canvas) {
        // Outer glow
        glowPaint.setMaskFilter(new BlurMaskFilter(50f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawCircle(px, py, playerRadius * 1.6f, glowPaint);

        // Inner glow
        glowPaint.setMaskFilter(new BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawCircle(px, py, playerRadius * 1.2f, glowPaint);

        // Core
        canvas.drawCircle(px, py, playerRadius, playerPaint);

        // Highlight
        Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlight.setColor(0x80FFFFFF);
        canvas.drawCircle(px - playerRadius * 0.3f, py - playerRadius * 0.3f, playerRadius * 0.35f, highlight);

        // Gravity arrow
        drawGravityArrow(canvas);
    }

    private void drawGravityArrow(Canvas canvas) {
        canvas.save();
        canvas.rotate(gravityAngle - 90f, px, py);

        float arrowLen = playerRadius * 0.9f;
        float arrowX   = px;
        float arrowY   = py + arrowLen;

        arrowPaint.setColor(0xCCFFFFFF);
        arrowPaint.setStrokeWidth(3f);
        arrowPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(arrowX, py - arrowLen * 0.5f, arrowX, arrowY, arrowPaint);

        // Arrowhead
        Path arrowHead = new Path();
        arrowHead.moveTo(arrowX, arrowY + 10);
        arrowHead.lineTo(arrowX - 8, arrowY - 4);
        arrowHead.lineTo(arrowX + 8, arrowY - 4);
        arrowHead.close();
        arrowPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrowHead, arrowPaint);

        canvas.restore();
    }

    private void drawHUD(Canvas canvas) {
        if (state == GameState.PLAYING || state == GameState.PAUSED) {
            // Score
            scorePaint.setTextSize(68f);
            scorePaint.setColor(COLOR_SCORE);
            canvas.drawText(String.valueOf(score), screenW / 2f, 100, scorePaint);

            // Level badge
            uiPaint.setColor(0x80000000);
            canvas.drawRoundRect(new RectF(screenW / 2f - 60, 110, screenW / 2f + 60, 148), 20f, 20f, uiPaint);
            textPaint.setTextSize(28f);
            textPaint.setColor(COLOR_ACCENT);
            canvas.drawText("LVL " + level, screenW / 2f, 138, textPaint);

            // Pause button
            uiPaint.setColor(0x80000000);
            canvas.drawRoundRect(pauseButtonRect, 16f, 16f, uiPaint);
            drawPauseIcon(canvas);
        }
    }

    private void drawPauseIcon(Canvas canvas) {
        Paint p = new Paint();
        p.setColor(COLOR_TEXT);
        p.setStyle(Paint.Style.FILL);
        float cx = pauseButtonRect.centerX();
        float cy = pauseButtonRect.centerY();
        canvas.drawRoundRect(new RectF(cx - 16, cy - 18, cx - 4, cy + 18), 4f, 4f, p);
        canvas.drawRoundRect(new RectF(cx + 4,  cy - 18, cx + 16, cy + 18), 4f, 4f, p);
    }

    private void drawGravityIndicator(Canvas canvas) {
        if (state != GameState.PLAYING && state != GameState.PAUSED) return;

        uiPaint.setColor(0x80000000);
        canvas.drawRoundRect(gravityIndicatorRect, 16f, 16f, uiPaint);

        float cx = gravityIndicatorRect.centerX();
        float cy = gravityIndicatorRect.centerY();

        // Draw rotating arrow
        canvas.save();
        canvas.rotate(gravityAngle - 90, cx, cy);
        Paint gArrow = new Paint(Paint.ANTI_ALIAS_FLAG);
        gArrow.setColor(COLOR_PLAYER);
        gArrow.setStyle(Paint.Style.STROKE);
        gArrow.setStrokeWidth(3f);
        gArrow.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(cx, cy - 25, cx, cy + 25, gArrow);
        Path head = new Path();
        head.moveTo(cx, cy + 32);
        head.lineTo(cx - 10, cy + 18);
        head.lineTo(cx + 10, cy + 18);
        head.close();
        gArrow.setStyle(Paint.Style.FILL);
        canvas.drawPath(head, gArrow);
        canvas.restore();

        textPaint.setTextSize(18f);
        textPaint.setColor(0xAAFFFFFF);
        canvas.drawText("GRAVITY", cx, gravityIndicatorRect.bottom + 22, textPaint);
    }

    private void drawFlash(Canvas canvas) {
        if (flashAlpha > 0) {
            Paint flash = new Paint();
            flash.setColor(0xFFFF3366 & 0x00FFFFFF | ((int)(flashAlpha * 180) << 24));
            canvas.drawRect(0, 0, screenW, screenH, flash);
        }
    }

    private void drawReadyScreen(Canvas canvas) {
        // Dim overlay
        Paint overlay = new Paint();
        overlay.setColor(0xAA000000);
        canvas.drawRect(0, 0, screenW, screenH, overlay);

        textPaint.setTextSize(80f);
        textPaint.setColor(COLOR_PLAYER);
        canvas.drawText("NEURO", screenW / 2f, screenH / 2f - 60, textPaint);
        canvas.drawText("SHIFT", screenW / 2f, screenH / 2f + 20, textPaint);

        textPaint.setTextSize(32f);
        textPaint.setColor(0xAAFFFFFF);
        canvas.drawText("TAP TO START", screenW / 2f, screenH / 2f + 100, textPaint);

        textPaint.setTextSize(26f);
        textPaint.setColor(0x88FFFFFF);
        canvas.drawText("TAP = ROTATE GRAVITY 90°", screenW / 2f, screenH / 2f + 160, textPaint);
    }

    private void drawPausedScreen(Canvas canvas) {
        Paint overlay = new Paint();
        overlay.setColor(0xBB000000);
        canvas.drawRect(0, 0, screenW, screenH, overlay);

        textPaint.setTextSize(72f);
        textPaint.setColor(COLOR_PLAYER);
        canvas.drawText("PAUSED", screenW / 2f, screenH / 2f - 40, textPaint);

        textPaint.setTextSize(36f);
        textPaint.setColor(0xAAFFFFFF);
        canvas.drawText("TAP TO RESUME", screenW / 2f, screenH / 2f + 50, textPaint);
    }

    private void drawGameOverScreen(Canvas canvas) {
        // Handled by GameOverActivity after delay
        Paint overlay = new Paint();
        overlay.setColor(0x99000000);
        canvas.drawRect(0, 0, screenW, screenH, overlay);

        textPaint.setTextSize(72f);
        textPaint.setColor(COLOR_OBS1);
        canvas.drawText("GAME OVER", screenW / 2f, screenH / 2f - 40, textPaint);

        scorePaint.setTextSize(52f);
        canvas.drawText("SCORE: " + score, screenW / 2f, screenH / 2f + 50, scorePaint);
    }

    // ════════════════════════════════════════════════════════════════
    //  Touch
    // ════════════════════════════════════════════════════════════════

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;

        long now = System.currentTimeMillis();
        if (now - lastTapTime < TAP_COOLDOWN) return true;
        lastTapTime = now;

        float tx = event.getX();
        float ty = event.getY();

        switch (state) {
            case READY:
                state = GameState.PLAYING;
                break;

            case PLAYING:
                // Pause button
                if (pauseButtonRect.contains(tx, ty)) {
                    state = GameState.PAUSED;
                    return true;
                }
                rotateGravity();
                break;

            case PAUSED:
                state = GameState.PLAYING;
                break;

            case GAME_OVER:
                break;
        }
        return true;
    }

    private void rotateGravity() {
        switch (gravityDir) {
            case DOWN:  gravityDir = GravityDir.LEFT;  targetAngle = 180f; break;
            case LEFT:  gravityDir = GravityDir.UP;    targetAngle = 270f; break;
            case UP:    gravityDir = GravityDir.RIGHT; targetAngle = 0f;   break;
            case RIGHT: gravityDir = GravityDir.DOWN;  targetAngle = 90f;  break;
        }

        // Kill velocity in the new gravity axis
        switch (gravityDir) {
            case DOWN:
            case UP:    pvx *= 0.3f; break;
            case LEFT:
            case RIGHT: pvy *= 0.3f; break;
        }

        isRotating = true;
        spawnGravityParticles();
    }

    // ════════════════════════════════════════════════════════════════
    //  Pause / Resume
    // ════════════════════════════════════════════════════════════════

    public void pause() {
        if (state == GameState.PLAYING) state = GameState.PAUSED;
        if (gameLoop != null) {
            gameLoop.setRunning(false);
            try { gameLoop.join(1000); } catch (InterruptedException ignored) {}
        }
    }

    public void resume() {
        if (gameLoop == null || !gameLoop.isAlive()) {
            gameLoop = new GameLoop(this, holder);
            gameLoop.start();
        }
    }
}