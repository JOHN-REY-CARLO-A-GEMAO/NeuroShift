package com.neuroshift.kotlin

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LeaderboardActivity : AppCompatActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        setContentView(R.layout.activity_leaderboard)

        val sm = ScoreManager(this)
        val scores = sm.getScoreList()
        val list = findViewById<LinearLayout>(R.id.scoreList)
        val btnBack = findViewById<Button>(R.id.btnBack)

        val medals = arrayOf("🥇", "🥈", "🥉")

        for (i in scores.indices) {
            val tv = TextView(this)
            tv.setTextColor(0xFFFFFFFF.toInt())
            tv.textSize = 24f
            tv.setPadding(24, 16, 24, 16)
            val medal = if (i < 3) medals[i] else "  "
            tv.text = "$medal  #${i + 1}   ${scores[i]}"
            tv.typeface = Typeface.MONOSPACE
            list.addView(tv)
        }

        if (scores.isEmpty()) {
            val empty = TextView(this)
            empty.text = "No scores yet!\nPlay to set a record."
            empty.setTextColor(0xAAFFFFFF.toInt())
            empty.textSize = 20f
            empty.gravity = Gravity.CENTER
            list.addView(empty)
        }

        btnBack.setOnClickListener { finish() }
    }
}
