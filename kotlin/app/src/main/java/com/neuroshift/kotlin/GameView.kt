package com.neuroshift.kotlin

import android.content.Context
import android.content.Intent
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    // Game States
    enum class GameState {
        READY, PLAYING, PAUSED, GAME_OVER
    }

    // Gravity Directions
    enum class GravityDir {
        DOWN, LEFT, UP, RIGHT
    }

    // ─── Constants ────────────────────────────────────────────────
    companion object {
        private const val GRAVITY_FORCE = 1800f
        private const val MAX_SPEED = 900f
        private const val OBSTACLE_SPEED = 400
        private const val PARTICLE_COUNT = 60
        private const val OBSTACLE_INTERVAL = 1800L
        private const val STAR_COUNT = 80
        private const val TAP_COOLDOWN = 250L

        // ─── Colors ───────────────────────────────────────────────
        private const val COLOR_BG = 0xFF050A1A.toInt()
        private const val COLOR_PLAYER = 0xFF00FFCC.toInt()
        private const val COLOR_GLOW = 0x6000FFCC
        private const val COLOR_OBS1 = 0xFFFF3366.toInt()
        private const val COLOR_OBS2 = 0xFFFF6633.toInt()
        private const val COLOR_TEXT = 0xFFFFFFFF.toInt()
        private const val COLOR_ACCENT = 0xFF6C63FF.toInt()
        private const val COLOR_SCORE = 0xFF00FFCC.toInt()
    }

    // ─── Core ─────────────────────────────────────────────────────
    private var gameLoop: GameLoop? = null
    private val holder: SurfaceHolder
    private val random = java.util.Random()

    // ─── Game State ───────────────────────────────────────────────
    private var state: GameState = GameState.READY
    private var gravityDir: GravityDir = GravityDir.DOWN
    private var score = 0
    private var level = 1
    private var gameTime = 0L
    private var lastObstacleTime = 0L
    private var speedMultiplier = 1.0f

    // ─── Screen ───────────────────────────────────────────────────
    private var screenW = 0
    private var screenH = 0

    // ─── Player ───────────────────────────────────────────────────
    private var px = 0f
    private var py = 0f
    private var pvx = 0f
    private var pvy = 0f
    private val playerRadius = 28f
    private var trailAlpha = 0f
    private val trail = ArrayList<FloatArray>()

    // ─── Obstacles ────────────────────────────────────────────────
    private val obstacles = ArrayList<Obstacle>()

    // ─── Particles ────────────────────────────────────────────────
    private val particles = ArrayList<Particle>()

    // ─── Background Stars ─────────────────────────────────────────
    private var starX: FloatArray? = null
    private var starY: FloatArray? = null
    private var starR: FloatArray? = null

    // ─── Paints ───────────────────────────────────────────────────
    private val bgPaint = Paint()
    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val obstaclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val uiPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ─── Touch ────────────────────────────────────────────────────
    private var lastTapTime = 0L

    // ─── Score Manager ────────────────────────────────────────────
    private val scoreManager: ScoreManager

    // ─── Gravity Transition ───────────────────────────────────────
    private var gravityAngle = 90f   // current rendered angle
    private var targetAngle = 90f
    private var isRotating = false

    // ─── Flash Effect ─────────────────────────────────────────────
    private var flashAlpha = 0f

    // ─── UI Button Rects ─────────────────────────────────────────
    private lateinit var pauseButtonRect: RectF
    private lateinit var gravityIndicatorRect: RectF

    init {
        holder = getHolder()
        holder.addCallback(this)
        scoreManager = ScoreManager(context)
        isFocusable = true
    }

    // ════════════════════════════════════════════════════════════════
    //  SurfaceHolder Callbacks
    // ════════════════════════════════════════════════════════════════

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenW = width
        screenH = height
        initGame()
        gameLoop = GameLoop(this, holder)
        gameLoop!!.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        screenW = w
        screenH = h
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    // ════════════════════════════════════════════════════════════════
    //  Initialization
    // ════════════════════════════════════════════════════════════════

    private fun initGame() {
        // Player start position
        px = screenW / 2f
        py = screenH / 2f
        pvx = 0f
        pvy = 0f

        // Reset state
        state = GameState.READY
        gravityDir = GravityDir.DOWN
        gravityAngle = 90f
        targetAngle = 90f
        isRotating = false
        score = 0
        level = 1
        gameTime = 0L
        speedMultiplier = 1.0f
        lastObstacleTime = 0L
        flashAlpha = 0f

        obstacles.clear()
        particles.clear()
        trail.clear()

        // Stars
        starX = FloatArray(STAR_COUNT)
        starY = FloatArray(STAR_COUNT)
        starR = FloatArray(STAR_COUNT)
        for (i in 0 until STAR_COUNT) {
            starX!![i] = random.nextFloat() * screenW
            starY!![i] = random.nextFloat() * screenH
            starR!![i] = random.nextFloat() * 2.5f + 0.5f
        }

        // UI Rects
        pauseButtonRect = RectF(screenW - 110f, 30f, screenW - 20f, 110f)
        gravityIndicatorRect = RectF(20f, 30f, 120f, 130f)

        setupPaints()
    }

    private fun setupPaints() {
        bgPaint.color = COLOR_BG
        bgPaint.style = Paint.Style.FILL

        playerPaint.color = COLOR_PLAYER
        playerPaint.style = Paint.Style.FILL

        glowPaint.color = COLOR_GLOW
        glowPaint.style = Paint.Style.FILL
        glowPaint.maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)

        obstaclePaint.style = Paint.Style.FILL

        trailPaint.style = Paint.Style.FILL
        trailPaint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)

        textPaint.color = COLOR_TEXT
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textPaint.textSize = 52f

        scorePaint.color = COLOR_SCORE
        scorePaint.textAlign = Paint.Align.CENTER
        scorePaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        scorePaint.textSize = 72f

        starPaint.color = 0xFFFFFFFF.toInt()
        starPaint.style = Paint.Style.FILL

        arrowPaint.color = COLOR_PLAYER
        arrowPaint.style = Paint.Style.FILL
        arrowPaint.strokeWidth = 4f
        arrowPaint.strokeCap = Paint.Cap.ROUND

        particlePaint.style = Paint.Style.FILL

        uiPaint.style = Paint.Style.FILL
    }

    // ════════════════════════════════════════════════════════════════
    //  Game Loop
    // ════════════════════════════════════════════════════════════════

    fun update(dt: Float) {
        if (state != GameState.PLAYING) return

        gameTime += (dt * 1000).toLong()
        updateDifficulty()
        updateGravityRotation(dt)
        updatePlayer(dt)
        updateObstacles(dt)
        updateParticles(dt)
        updateTrail()
        spawnObstacles()
        checkCollisions()
        updateFlash(dt)

        score = (gameTime / 100).toInt()
    }

    private fun updateDifficulty() {
        level = (gameTime / 10000).toInt() + 1
        speedMultiplier = 1.0f + (level - 1) * 0.25f
    }

    private fun updateGravityRotation(dt: Float) {
        if (isRotating) {
            var diff = targetAngle - gravityAngle
            // Shortest path
            if (diff > 180f) diff -= 360f
            if (diff < -180f) diff += 360f

            val step = diff * dt * 8f
            if (abs(diff) < 2f) {
                gravityAngle = targetAngle
                isRotating = false
            } else {
                gravityAngle += step
            }
        }
    }

    private fun updatePlayer(dt: Float) {
        var gx = 0f
        var gy = 0f
        when (gravityDir) {
            GravityDir.DOWN -> gy = GRAVITY_FORCE
            GravityDir.UP -> gy = -GRAVITY_FORCE
            GravityDir.LEFT -> gx = -GRAVITY_FORCE
            GravityDir.RIGHT -> gx = GRAVITY_FORCE
        }

        pvx += gx * dt
        pvy += gy * dt

        // Cap speed
        pvx = maxOf(-MAX_SPEED, minOf(MAX_SPEED, pvx))
        pvy = maxOf(-MAX_SPEED, minOf(MAX_SPEED, pvy))

        px += pvx * dt
        py += pvy * dt

        // Wall bounce with damping
        if (px - playerRadius < 0f) {
            px = playerRadius
            pvx = abs(pvx) * 0.4f
            spawnImpactParticles(px, py, 8)
        }
        if (px + playerRadius > screenW) {
            px = screenW - playerRadius
            pvx = -abs(pvx) * 0.4f
            spawnImpactParticles(px, py, 8)
        }
        if (py - playerRadius < 0f) {
            py = playerRadius
            pvy = abs(pvy) * 0.4f
            spawnImpactParticles(px, py, 8)
        }
        if (py + playerRadius > screenH) {
            py = screenH - playerRadius
            pvy = -abs(pvy) * 0.4f
            spawnImpactParticles(px, py, 8)
        }
    }

    private fun updateObstacles(dt: Float) {
        for (obs in obstacles) {
            obs.update(dt, speedMultiplier)
        }
        obstacles.removeAll { it.isOffScreen(screenW, screenH) }
    }

    private fun updateParticles(dt: Float) {
        synchronized(particles) {
            val it = particles.iterator()
            while (it.hasNext()) {
                val p = it.next()
                p.update(dt)
                if (p.isDead()) it.remove()
            }
        }
    }

    private fun updateTrail() {
        trail.add(0, floatArrayOf(px, py))
        if (trail.size > 18) trail.removeAt(trail.size - 1)
    }

    private fun spawnObstacles() {
        val now = System.currentTimeMillis()
        val interval = (OBSTACLE_INTERVAL / speedMultiplier).toLong()
        if (now - lastObstacleTime < interval) return
        lastObstacleTime = now

        when (random.nextInt(4)) {
            0 -> spawnHorizontalObstacle()
            1 -> spawnVerticalObstacle()
            2 -> spawnCornerObstacle()
            3 -> spawnMovingObstacle()
        }
    }

    private fun spawnHorizontalObstacle() {
        val gapW = screenW / 3
        val gapX = random.nextInt(screenW - gapW)
        val h = 25 + random.nextInt(30)
        val fromTop = random.nextBoolean()
        val speed = OBSTACLE_SPEED.toFloat()

        if (fromTop) {
            // Top bar
            if (gapX > 20)
                obstacles.add(Obstacle(0f, -h.toFloat(), gapX.toFloat(), h.toFloat(), speed, 0f, getObsColor()))
            // Right bar
            if (gapX + gapW < screenW - 20)
                obstacles.add(
                    Obstacle(
                        (gapX + gapW).toFloat(), -h.toFloat(),
                        (screenW - gapX - gapW).toFloat(), h.toFloat(), speed, 0f, getObsColor()
                    )
                )
        } else {
            // Bottom spawn
            if (gapX > 20)
                obstacles.add(Obstacle(0f, screenH.toFloat(), gapX.toFloat(), h.toFloat(), -speed, 0f, getObsColor()))
            if (gapX + gapW < screenW - 20)
                obstacles.add(
                    Obstacle(
                        (gapX + gapW).toFloat(), screenH.toFloat(),
                        (screenW - gapX - gapW).toFloat(), h.toFloat(), -speed, 0f, getObsColor()
                    )
                )
        }
    }

    private fun spawnVerticalObstacle() {
        val gapH = screenH / 4
        val gapY = random.nextInt(screenH - gapH)
        val w = 25 + random.nextInt(30)
        val fromLeft = random.nextBoolean()
        val speed = OBSTACLE_SPEED.toFloat()

        if (fromLeft) {
            if (gapY > 20)
                obstacles.add(Obstacle(-w.toFloat(), 0f, w.toFloat(), gapY.toFloat(), 0f, speed, getObsColor()))
            if (gapY + gapH < screenH - 20)
                obstacles.add(
                    Obstacle(
                        -w.toFloat(), (gapY + gapH).toFloat(),
                        w.toFloat(), (screenH - gapY - gapH).toFloat(), 0f, speed, getObsColor()
                    )
                )
        } else {
            if (gapY > 20)
                obstacles.add(Obstacle(screenW.toFloat(), 0f, w.toFloat(), gapY.toFloat(), 0f, -speed, getObsColor()))
            if (gapY + gapH < screenH - 20)
                obstacles.add(
                    Obstacle(
                        screenW.toFloat(), (gapY + gapH).toFloat(),
                        w.toFloat(), (screenH - gapY - gapH).toFloat(), 0f, -speed, getObsColor()
                    )
                )
        }
    }

    private fun spawnCornerObstacle() {
        val corner = random.nextInt(4)
        val size = (80 + random.nextInt(60)).toFloat()
        val half = OBSTACLE_SPEED / 2.0f
        when (corner) {
            0 -> obstacles.add(Obstacle(-size, -size, size, size, half, half, getObsColor()))
            1 -> obstacles.add(Obstacle(screenW.toFloat(), -size, size, size, -half, half, getObsColor()))
            2 -> obstacles.add(Obstacle(-size, screenH.toFloat(), size, size, half, -half, getObsColor()))
            3 -> obstacles.add(Obstacle(screenW.toFloat(), screenH.toFloat(), size, size, -half, -half, getObsColor()))
        }
    }

    private fun spawnMovingObstacle() {
        // Bouncing obstacle
        val x = random.nextInt(screenW - 80).toFloat()
        val y = -60f
        val w = (60 + random.nextInt(60)).toFloat()
        val h = (20 + random.nextInt(20)).toFloat()
        obstacles.add(
            BouncingObstacle(
                x, y, w, h,
                (if (random.nextBoolean()) 1f else -1f) * (200 + random.nextInt(200)).toFloat(),
                OBSTACLE_SPEED.toFloat(),
                getObsColor(), screenW
            )
        )
    }

    private fun getObsColor(): Int {
        return if (random.nextBoolean()) COLOR_OBS1 else COLOR_OBS2
    }

    private fun checkCollisions() {
        for (obs in obstacles) {
            if (obs.collidesWith(px, py, playerRadius)) {
                triggerGameOver()
                return
            }
        }
    }

    private fun triggerGameOver() {
        state = GameState.GAME_OVER
        flashAlpha = 1.0f
        spawnDeathParticles()
        scoreManager.saveScore(score)

        // Navigate to GameOver screen after short delay
        postDelayed({
            val intent = Intent(context, GameOverActivity::class.java)
            intent.putExtra("score", score)
            intent.putExtra("level", level)
            context.startActivity(intent)
        }, 1200)
    }

    private fun updateFlash(dt: Float) {
        if (flashAlpha > 0f) {
            flashAlpha -= dt * 2.5f
            if (flashAlpha < 0f) flashAlpha = 0f
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Particles
    // ════════════════════════════════════════════════════════════════

    private fun spawnImpactParticles(x: Float, y: Float, count: Int) {
        synchronized(particles) {
            for (i in 0 until count) {
                val angle = random.nextFloat() * 360f
                val speed = 100f + random.nextFloat() * 300f
                val vx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed
                val vy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed
                particles.add(Particle(x, y, vx, vy, COLOR_PLAYER, 3f + random.nextFloat() * 4f, 0.4f))
            }
        }
    }

    private fun spawnDeathParticles() {
        synchronized(particles) {
            for (i in 0 until PARTICLE_COUNT) {
                val angle = random.nextFloat() * 360f
                val speed = 200f + random.nextFloat() * 600f
                val vx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed
                val vy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed
                val color = if (random.nextBoolean()) COLOR_PLAYER else COLOR_OBS1
                particles.add(Particle(px, py, vx, vy, color, 4f + random.nextFloat() * 8f, 0.8f))
            }
        }
    }

    private fun spawnGravityParticles() {
        synchronized(particles) {
            for (i in 0 until 12) {
                val angle = random.nextFloat() * 360f
                val speed = 80f + random.nextFloat() * 200f
                val vx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed
                val vy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed
                particles.add(Particle(px, py, vx, vy, COLOR_ACCENT, 3f + random.nextFloat() * 5f, 0.5f))
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Rendering
    // ════════════════════════════════════════════════════════════════

    fun render(canvas: Canvas) {
        drawBackground(canvas)
        drawStars(canvas)
        drawObstacles(canvas)
        drawTrail(canvas)
        drawParticles(canvas)

        if (state != GameState.GAME_OVER) {
            drawPlayer(canvas)
        }

        drawHUD(canvas)
        drawGravityIndicator(canvas)
        drawFlash(canvas)

        when (state) {
            GameState.READY -> drawReadyScreen(canvas)
            GameState.PAUSED -> drawPausedScreen(canvas)
            GameState.GAME_OVER -> drawGameOverScreen(canvas)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), bgPaint)

        // Gradient lines (scanlines effect)
        val linePaint = Paint()
        linePaint.color = 0x08FFFFFF
        var y = 0
        while (y < screenH) {
            canvas.drawLine(0f, y.toFloat(), screenW.toFloat(), y.toFloat(), linePaint)
            y += 4
        }
    }

    private fun drawStars(canvas: Canvas) {
        val sx = starX ?: return
        val sy = starY ?: return
        val sr = starR ?: return
        val t = System.currentTimeMillis().toFloat() * 0.001f
        for (i in 0 until STAR_COUNT) {
            val flicker = 0.4f + 0.6f * sin((t + i).toDouble()).toFloat()
            starPaint.alpha = (flicker * 180).toInt()
            canvas.drawCircle(sx[i], sy[i], sr[i], starPaint)
        }
    }

    private fun drawObstacles(canvas: Canvas) {
        for (obs in obstacles) {
            // Glow
            val glow = Paint(Paint.ANTI_ALIAS_FLAG)
            glow.color = obs.color and 0x00FFFFFF or 0x40000000
            glow.maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawRect(obs.rect, glow)

            // Body
            obstaclePaint.color = obs.color
            canvas.drawRoundRect(obs.rect, 8f, 8f, obstaclePaint)

            // Shine
            val shine = Paint()
            shine.color = 0x30FFFFFF
            val r = obs.rect
            val shineRect = RectF(r.left + 4f, r.top + 4f, r.right - 4f, r.top + 10f)
            canvas.drawRoundRect(shineRect, 4f, 4f, shine)
        }
    }

    private fun drawTrail(canvas: Canvas) {
        for (i in trail.indices) {
            val pos = trail[i]
            val frac = 1f - i / trail.size.toFloat()
            val r = playerRadius * frac * 0.7f
            val alpha = (frac * frac * 180).toInt()
            trailPaint.color = COLOR_PLAYER and 0x00FFFFFF or (alpha shl 24)
            canvas.drawCircle(pos[0], pos[1], r, trailPaint)
        }
    }

    private fun drawParticles(canvas: Canvas) {
        synchronized(particles) {
            for (p in particles) {
                particlePaint.color = p.color and 0x00FFFFFF or ((p.alpha * 255).toInt() shl 24)
                canvas.drawCircle(p.x, p.y, p.radius * p.alpha, particlePaint)
            }
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        // Outer glow
        glowPaint.maskFilter = BlurMaskFilter(50f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(px, py, playerRadius * 1.6f, glowPaint)

        // Inner glow
        glowPaint.maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawCircle(px, py, playerRadius * 1.2f, glowPaint)

        // Core
        canvas.drawCircle(px, py, playerRadius, playerPaint)

        // Highlight
        val highlight = Paint(Paint.ANTI_ALIAS_FLAG)
        highlight.color = 0x80FFFFFF.toInt()
        canvas.drawCircle(px - playerRadius * 0.3f, py - playerRadius * 0.3f, playerRadius * 0.35f, highlight)

        // Gravity arrow
        drawGravityArrow(canvas)
    }

    private fun drawGravityArrow(canvas: Canvas) {
        canvas.save()
        canvas.rotate(gravityAngle - 90f, px, py)

        val arrowLen = playerRadius * 0.9f
        val arrowX = px
        val arrowY = py + arrowLen

        arrowPaint.color = 0xCCFFFFFF.toInt()
        arrowPaint.strokeWidth = 3f
        arrowPaint.style = Paint.Style.STROKE
        canvas.drawLine(arrowX, py - arrowLen * 0.5f, arrowX, arrowY, arrowPaint)

        // Arrowhead
        val arrowHead = Path()
        arrowHead.moveTo(arrowX, arrowY + 10f)
        arrowHead.lineTo(arrowX - 8f, arrowY - 4f)
        arrowHead.lineTo(arrowX + 8f, arrowY - 4f)
        arrowHead.close()
        arrowPaint.style = Paint.Style.FILL
        canvas.drawPath(arrowHead, arrowPaint)

        canvas.restore()
    }

    private fun drawHUD(canvas: Canvas) {
        if (state == GameState.PLAYING || state == GameState.PAUSED) {
            // Score
            scorePaint.textSize = 68f
            scorePaint.color = COLOR_SCORE
            canvas.drawText(score.toString(), screenW / 2f, 100f, scorePaint)

            // Level badge
            uiPaint.color = 0x80000000.toInt()
            canvas.drawRoundRect(RectF(screenW / 2f - 60f, 110f, screenW / 2f + 60f, 148f), 20f, 20f, uiPaint)
            textPaint.textSize = 28f
            textPaint.color = COLOR_ACCENT
            canvas.drawText("LVL $level", screenW / 2f, 138f, textPaint)

            // Pause button
            uiPaint.color = 0x80000000.toInt()
            canvas.drawRoundRect(pauseButtonRect, 16f, 16f, uiPaint)
            drawPauseIcon(canvas)
        }
    }

    private fun drawPauseIcon(canvas: Canvas) {
        val p = Paint()
        p.color = COLOR_TEXT
        p.style = Paint.Style.FILL
        val cx = pauseButtonRect.centerX()
        val cy = pauseButtonRect.centerY()
        canvas.drawRoundRect(RectF(cx - 16f, cy - 18f, cx - 4f, cy + 18f), 4f, 4f, p)
        canvas.drawRoundRect(RectF(cx + 4f, cy - 18f, cx + 16f, cy + 18f), 4f, 4f, p)
    }

    private fun drawGravityIndicator(canvas: Canvas) {
        if (state != GameState.PLAYING && state != GameState.PAUSED) return

        uiPaint.color = 0x80000000.toInt()
        canvas.drawRoundRect(gravityIndicatorRect, 16f, 16f, uiPaint)

        val cx = gravityIndicatorRect.centerX()
        val cy = gravityIndicatorRect.centerY()

        // Draw rotating arrow
        canvas.save()
        canvas.rotate(gravityAngle - 90f, cx, cy)
        val gArrow = Paint(Paint.ANTI_ALIAS_FLAG)
        gArrow.color = COLOR_PLAYER
        gArrow.style = Paint.Style.STROKE
        gArrow.strokeWidth = 3f
        gArrow.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(cx, cy - 25f, cx, cy + 25f, gArrow)
        val head = Path()
        head.moveTo(cx, cy + 32f)
        head.lineTo(cx - 10f, cy + 18f)
        head.lineTo(cx + 10f, cy + 18f)
        head.close()
        gArrow.style = Paint.Style.FILL
        canvas.drawPath(head, gArrow)
        canvas.restore()

        textPaint.textSize = 18f
        textPaint.color = 0xAAFFFFFF.toInt()
        canvas.drawText("GRAVITY", cx, gravityIndicatorRect.bottom + 22f, textPaint)
    }

    private fun drawFlash(canvas: Canvas) {
        if (flashAlpha > 0f) {
            val flash = Paint()
            flash.color = COLOR_OBS1 and 0x00FFFFFF or ((flashAlpha * 180).toInt() shl 24)
            canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), flash)
        }
    }

    private fun drawReadyScreen(canvas: Canvas) {
        // Dim overlay
        val overlay = Paint()
        overlay.color = 0xAA000000.toInt()
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), overlay)

        textPaint.textSize = 80f
        textPaint.color = COLOR_PLAYER
        canvas.drawText("NEURO", screenW / 2f, screenH / 2f - 60f, textPaint)
        canvas.drawText("SHIFT", screenW / 2f, screenH / 2f + 20f, textPaint)

        textPaint.textSize = 32f
        textPaint.color = 0xAAFFFFFF.toInt()
        canvas.drawText("TAP TO START", screenW / 2f, screenH / 2f + 100f, textPaint)

        textPaint.textSize = 26f
        textPaint.color = 0x88FFFFFF.toInt()
        canvas.drawText("TAP = ROTATE GRAVITY 90°", screenW / 2f, screenH / 2f + 160f, textPaint)
    }

    private fun drawPausedScreen(canvas: Canvas) {
        val overlay = Paint()
        overlay.color = 0xBB000000.toInt()
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), overlay)

        textPaint.textSize = 72f
        textPaint.color = COLOR_PLAYER
        canvas.drawText("PAUSED", screenW / 2f, screenH / 2f - 40f, textPaint)

        textPaint.textSize = 36f
        textPaint.color = 0xAAFFFFFF.toInt()
        canvas.drawText("TAP TO RESUME", screenW / 2f, screenH / 2f + 50f, textPaint)
    }

    private fun drawGameOverScreen(canvas: Canvas) {
        // Handled by GameOverActivity after delay
        val overlay = Paint()
        overlay.color = 0x99000000.toInt()
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), overlay)

        textPaint.textSize = 72f
        textPaint.color = COLOR_OBS1
        canvas.drawText("GAME OVER", screenW / 2f, screenH / 2f - 40f, textPaint)

        scorePaint.textSize = 52f
        canvas.drawText("SCORE: $score", screenW / 2f, screenH / 2f + 50f, scorePaint)
    }

    // ════════════════════════════════════════════════════════════════
    //  Touch
    // ════════════════════════════════════════════════════════════════

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return true

        val now = System.currentTimeMillis()
        if (now - lastTapTime < TAP_COOLDOWN) return true
        lastTapTime = now

        val tx = event.x
        val ty = event.y

        when (state) {
            GameState.READY -> state = GameState.PLAYING

            GameState.PLAYING -> {
                // Pause button
                if (pauseButtonRect.contains(tx, ty)) {
                    state = GameState.PAUSED
                    return true
                }
                rotateGravity()
            }

            GameState.PAUSED -> state = GameState.PLAYING

            GameState.GAME_OVER -> {}
        }
        return true
    }

    private fun rotateGravity() {
        when (gravityDir) {
            GravityDir.DOWN -> {
                gravityDir = GravityDir.LEFT
                targetAngle = 180f
            }
            GravityDir.LEFT -> {
                gravityDir = GravityDir.UP
                targetAngle = 270f
            }
            GravityDir.UP -> {
                gravityDir = GravityDir.RIGHT
                targetAngle = 0f
            }
            GravityDir.RIGHT -> {
                gravityDir = GravityDir.DOWN
                targetAngle = 90f
            }
        }

        // Kill velocity in the new gravity axis
        when (gravityDir) {
            GravityDir.DOWN, GravityDir.UP -> pvx *= 0.3f
            GravityDir.LEFT, GravityDir.RIGHT -> pvy *= 0.3f
        }

        isRotating = true
        spawnGravityParticles()
    }

    // ════════════════════════════════════════════════════════════════
    //  Pause / Resume
    // ════════════════════════════════════════════════════════════════

    fun pause() {
        if (state == GameState.PLAYING) state = GameState.PAUSED
        gameLoop?.let { loop ->
            loop.setRunning(false)
            try {
                loop.join(1000)
            } catch (_: InterruptedException) {
            }
        }
    }

    fun resume() {
        if (gameLoop == null || !gameLoop!!.isAlive) {
            gameLoop = GameLoop(this, holder)
            gameLoop!!.start()
        }
    }
}
