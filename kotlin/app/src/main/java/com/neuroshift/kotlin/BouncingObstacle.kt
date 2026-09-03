package com.neuroshift.kotlin

import kotlin.math.abs

class BouncingObstacle(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    vx: Float,
    vy: Float,
    color: Int,
    private val screenW: Int
) : Obstacle(x, y, w, h, vx, vy, color) {

    override fun update(dt: Float, speedMult: Float) {
        x += vx * speedMult * dt
        y += vy * speedMult * dt

        if (x < 0f) {
            x = 0f
            vx = abs(vx)
        }
        if (x + w > screenW) {
            x = screenW.toFloat() - w
            vx = -abs(vx)
        }
    }
}
