package com.neuroshift.game;

public class BouncingObstacle extends Obstacle {

    private int screenW;

    public BouncingObstacle(float x, float y, float w, float h,
                            float vx, float vy, int color, int screenW) {
        super(x, y, w, h, vx, vy, color);
        this.screenW = screenW;
    }

    @Override
    public void update(float dt, float speedMult) {
        x += vx * speedMult * dt;
        y += vy * speedMult * dt;

        if (x < 0)             { x = 0;             vx = Math.abs(vx); }
        if (x + w > screenW)   { x = screenW - w;   vx = -Math.abs(vx); }
    }
}