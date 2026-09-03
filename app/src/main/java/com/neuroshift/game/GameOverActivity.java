package com.neuroshift.game;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.neuroshift.game.R;

public class GameOverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_game_over);

        int score = getIntent().getIntExtra("score", 0);
        int level = getIntent().getIntExtra("level", 1);

        ScoreManager sm = new ScoreManager(this);
        int best = sm.getHighScore();

        TextView tvScore = findViewById(R.id.tvScore);
        TextView tvBest  = findViewById(R.id.tvBest);
        TextView tvLevel = findViewById(R.id.tvLevel);
        Button   btnRetry= findViewById(R.id.btnRetry);
        Button   btnMenu = findViewById(R.id.btnMenu);

        tvScore.setText(String.valueOf(score));
        tvBest .setText("BEST: " + best);
        tvLevel.setText("LEVEL " + level);

        tvScore.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pop_in));

        btnRetry.setOnClickListener(v -> {
            startActivity(new Intent(this, GameActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finishAffinity();
        });
    }
}