package com.neuroshift.game;

public class Particle {

    public float x, y, vx, vy;
    public int   color;
    public float radius;
    public float alpha;
    private float lifetime;
    private float maxLifetime;

    public Particle(float x, float y, float vx, float vy,
                    int color, float radius, float lifetime) {
        this.x = x; this.y = y;
        this.vx = vx; this.vy = vy;
        this.color = color;
        this.radius = radius;
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
        this.alpha = 1f;
    }

    public void update(float dt) {
        x        += vx * dt;
        y        += vy * dt;
        vx       *= (1f - dt * 3f);   // drag
        vy       *= (1f - dt * 3f);
        lifetime -= dt;
        alpha     = Math.max(0f, lifetime / maxLifetime);
    }

    public boolean isDead() { return lifetime <= 0f; }
}