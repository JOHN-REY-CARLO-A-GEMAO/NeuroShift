package com.neuroshift.kotlin

import android.content.Context
import android.content.SharedPreferences

class ScoreManager(ctx: Context) {

    companion object {
        private const val PREFS = "neuroshift_scores"
        private const val KEY_HIGH = "high_score"
        private const val KEY_LIST = "score_list"
        private const val MAX_SCORES = 10
    }

    private val prefs: SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveScore(score: Int) {
        // Update high score
        if (score > getHighScore()) {
            prefs.edit().putInt(KEY_HIGH, score).apply()
        }

        // Update list
        val scores = getScoreList().toMutableList()
        scores.add(score)
        scores.sortDescending()
        val trimmed = if (scores.size > MAX_SCORES) {
            scores.subList(0, MAX_SCORES).toList()
        } else {
            scores
        }

        val sb = StringBuilder()
        trimmed.forEachIndexed { i, s ->
            if (i > 0) sb.append(",")
            sb.append(s)
        }
        prefs.edit().putString(KEY_LIST, sb.toString()).apply()
    }

    fun getHighScore(): Int = prefs.getInt(KEY_HIGH, 0)

    fun getScoreList(): List<Int> {
        val raw = prefs.getString(KEY_LIST, "") ?: ""
        val list = ArrayList<Int>()
        if (raw.isEmpty()) return list
        for (s in raw.split(",")) {
            s.trim().toIntOrNull()?.let { list.add(it) }
        }
        return list
    }
}
