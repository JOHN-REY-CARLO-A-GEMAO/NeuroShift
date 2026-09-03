package com.neuroshift.game;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

import com.neuroshift.game.R;

public class LeaderboardActivity extends AppCompatActivity {

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

        setContentView(R.layout.activity_leaderboard);

        ScoreManager   sm      = new ScoreManager(this);
        List<Integer>  scores  = sm.getScoreList();
        LinearLayout   list    = findViewById(R.id.scoreList);
        Button         btnBack = findViewById(R.id.btnBack);

        String[] medals = {"🥇", "🥈", "🥉"};

        for (int i = 0; i < scores.size(); i++) {
            TextView tv = new TextView(this);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(24f);
            tv.setPadding(24, 16, 24, 16);
            String medal = (i < 3) ? medals[i] : "  ";
            tv.setText(medal + "  #" + (i + 1) + "   " + scores.get(i));
            tv.setTypeface(android.graphics.Typeface.MONOSPACE);
            list.addView(tv);
        }

        if (scores.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No scores yet!\nPlay to set a record.");
            empty.setTextColor(0xAAFFFFFF);
            empty.setTextSize(20f);
            empty.setGravity(android.view.Gravity.CENTER);
            list.addView(empty);
        }

        btnBack.setOnClickListener(v -> finish());
    }
}