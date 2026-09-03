package com.neuroshift.game;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoreManager {

    private static final String PREFS    = "neuroshift_scores";
    private static final String KEY_HIGH = "high_score";
    private static final String KEY_LIST = "score_list";
    private static final int    MAX_SCORES = 10;

    private SharedPreferences prefs;

    public ScoreManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveScore(int score) {
        // Update high score
        if (score > getHighScore()) {
            prefs.edit().putInt(KEY_HIGH, score).apply();
        }

        // Update list
        List<Integer> scores = getScoreList();
        scores.add(score);
        Collections.sort(scores, Collections.reverseOrder());
        if (scores.size() > MAX_SCORES) {
            scores = scores.subList(0, MAX_SCORES);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scores.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(scores.get(i));
        }
        prefs.edit().putString(KEY_LIST, sb.toString()).apply();
    }

    public int getHighScore() {
        return prefs.getInt(KEY_HIGH, 0);
    }

    public List<Integer> getScoreList() {
        String raw = prefs.getString(KEY_LIST, "");
        List<Integer> list = new ArrayList<>();
        if (raw.isEmpty()) return list;
        for (String s : raw.split(",")) {
            try { list.add(Integer.parseInt(s.trim())); }
            catch (NumberFormatException ignored) {}
        }
        return list;
    }
}