package com.neuroshift.kotlin

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Int,
    var radius: Float,
    lifetime: Float
) {

    private var lifetime: Float
    private val maxLifetime: Float
    var alpha: Float = 1f

    init {
        this.lifetime = lifetime
        this.maxLifetime = lifetime
    }

    fun update(dt: Float) {
        x += vx * dt
        y += vy * dt
        vx *= (1f - dt * 3f)   // drag
        vy *= (1f - dt * 3f)
        lifetime -= dt
        alpha = maxOf(0f, lifetime / maxLifetime)
    }

    fun isDead(): Boolean = lifetime <= 0f
}
