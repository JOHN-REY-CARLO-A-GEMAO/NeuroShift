package com.neuroshift.game;

import android.graphics.RectF;

public class Obstacle {

    protected float x, y, w, h;
    protected float vx, vy;
    public    int   color;

    public Obstacle(float x, float y, float w, float h, float vx, float vy, int color) {
        this.x = x; this.y = y;
        this.w = w; this.h = h;
        this.vx = vx; this.vy = vy;
        this.color = color;
    }

    public void update(float dt, float speedMult) {
        x += vx * speedMult * dt;
        y += vy * speedMult * dt;
    }

    public boolean isOffScreen(int sw, int sh) {
        return x + w < -100 || x > sw + 100
            || y + h < -100 || y > sh + 100;
    }

    public boolean collidesWith(float cx, float cy, float radius) {
        float nearX = Math.max(x, Math.min(cx, x + w));
        float nearY = Math.max(y, Math.min(cy, y + h));
        float dx    = cx - nearX;
        float dy    = cy - nearY;
        return (dx * dx + dy * dy) < (radius * radius);
    }

    public RectF getRect() {
        return new RectF(x, y, x + w, y + h);
    }
}