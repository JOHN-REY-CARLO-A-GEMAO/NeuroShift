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

public class MainActivity extends AppCompatActivity {

    private TextView titleText;
    private Button btnPlay, btnLeaderboard, btnQuit;
    private TextView highScoreText;
    private ScoreManager scoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen immersive
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_main);

        scoreManager = new ScoreManager(this);

        initViews();
        setupAnimations();
        updateHighScore();
    }

    private void initViews() {
        titleText = findViewById(R.id.titleText);
        btnPlay = findViewById(R.id.btnPlay);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        btnQuit = findViewById(R.id.btnQuit);
        highScoreText = findViewById(R.id.highScoreText);

        btnPlay.setOnClickListener(v -> startGame());
        btnLeaderboard.setOnClickListener(v -> openLeaderboard());
        btnQuit.setOnClickListener(v -> finish());
    }

    private void setupAnimations() {
        titleText.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.pulse)
        );
        btnPlay.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_up)
        );
    }

    private void updateHighScore() {
        int best = scoreManager.getHighScore();
        highScoreText.setText("BEST: " + best);
    }

    private void startGame() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void openLeaderboard() {
        Intent intent = new Intent(this, LeaderboardActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHighScore();
    }
}