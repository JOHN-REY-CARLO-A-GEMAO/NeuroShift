package com.neuroshift.kotlin

import android.graphics.RectF

open class Obstacle(
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float,
    var vx: Float,
    var vy: Float,
    var color: Int
) {

    open fun update(dt: Float, speedMult: Float) {
        x += vx * speedMult * dt
        y += vy * speedMult * dt
    }

    fun isOffScreen(sw: Int, sh: Int): Boolean {
        return x + w < -100f || x > sw + 100f
            || y + h < -100f || y > sh + 100f
    }

    fun collidesWith(cx: Float, cy: Float, radius: Float): Boolean {
        val nearX = maxOf(x, minOf(cx, x + w))
        val nearY = maxOf(y, minOf(cy, y + h))
        val dx = cx - nearX
        val dy = cy - nearY
        return (dx * dx + dy * dy) < (radius * radius)
    }

    val rect: RectF
        get() = RectF(x, y, x + w, y + h)
}
