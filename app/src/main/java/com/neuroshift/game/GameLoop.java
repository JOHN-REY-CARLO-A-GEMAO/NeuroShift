package com.neuroshift.game;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameLoop extends Thread {

    private static final int TARGET_FPS   = 60;
    private static final long FRAME_TIME  = 1000_000_000L / TARGET_FPS;

    private GameView gameView;
    private SurfaceHolder holder;
    private volatile boolean running = true;

    public GameLoop(GameView gameView, SurfaceHolder holder) {
        this.gameView = gameView;
        this.holder   = holder;
        setName("GameLoop");
        setDaemon(true);
    }

    public void setRunning(boolean r) { running = r; }

    @Override
    public void run() {
        long prevTime = System.nanoTime();

        while (running) {
            long now   = System.nanoTime();
            float dt   = (now - prevTime) / 1_000_000_000f;
            prevTime   = now;

            // Cap dt to avoid spiral of death
            if (dt > 0.05f) dt = 0.05f;

            gameView.update(dt);

            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    synchronized (holder) {
                        gameView.render(canvas);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // Frame rate cap
            long elapsed = System.nanoTime() - now;
            long sleep   = (FRAME_TIME - elapsed) / 1_000_000L;
            if (sleep > 0) {
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }
    }
}