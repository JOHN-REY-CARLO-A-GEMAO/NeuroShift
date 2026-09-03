package com.neuroshift.kotlin

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameLoop(
    private val gameView: GameView,
    private val holder: SurfaceHolder
) : Thread() {

    companion object {
        private const val TARGET_FPS = 60
        private const val FRAME_TIME = 1_000_000_000L / TARGET_FPS
    }

    @Volatile
    var running: Boolean = true

    init {
        name = "GameLoop"
        isDaemon = true
    }

    fun setRunning(r: Boolean) {
        running = r
    }

    override fun run() {
        var prevTime = System.nanoTime()

        while (running) {
            val now = System.nanoTime()
            var dt = (now - prevTime) / 1_000_000_000f
            prevTime = now

            // Cap dt to avoid spiral of death
            if (dt > 0.05f) dt = 0.05f

            gameView.update(dt)

            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) {
                        gameView.render(canvas)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Frame rate cap
            val elapsed = System.nanoTime() - now
            val sleep = (FRAME_TIME - elapsed) / 1_000_000L
            if (sleep > 0) {
                try {
                    sleep(sleep)
                } catch (_: InterruptedException) {
                }
            }
        }
    }
}
