package com.neuroshift.kotlin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GameOverActivity : AppCompatActivity() {

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

        setContentView(R.layout.activity_game_over)

        val score = intent.getIntExtra("score", 0)
        val level = intent.getIntExtra("level", 1)

        val sm = ScoreManager(this)
        val best = sm.getHighScore()

        val tvScore = findViewById<TextView>(R.id.tvScore)
        val tvBest = findViewById<TextView>(R.id.tvBest)
        val tvLevel = findViewById<TextView>(R.id.tvLevel)
        val btnRetry = findViewById<Button>(R.id.btnRetry)
        val btnMenu = findViewById<Button>(R.id.btnMenu)

        tvScore.text = score.toString()
        tvBest.text = "BEST: $best"
        tvLevel.text = "LEVEL $level"

        tvScore.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pop_in))

        btnRetry.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }

        btnMenu.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}
