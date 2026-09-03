package com.neuroshift.kotlin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnLeaderboard: Button
    private lateinit var btnQuit: Button
    private lateinit var highScoreText: TextView
    private lateinit var scoreManager: ScoreManager

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen immersive
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        setContentView(R.layout.activity_main)

        scoreManager = ScoreManager(this)

        initViews()
        setupAnimations()
        updateHighScore()
    }

    private fun initViews() {
        titleText = findViewById(R.id.titleText)
        btnPlay = findViewById(R.id.btnPlay)
        btnLeaderboard = findViewById(R.id.btnLeaderboard)
        btnQuit = findViewById(R.id.btnQuit)
        highScoreText = findViewById(R.id.highScoreText)

        btnPlay.setOnClickListener { startGame() }
        btnLeaderboard.setOnClickListener { openLeaderboard() }
        btnQuit.setOnClickListener { finish() }
    }

    private fun setupAnimations() {
        titleText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse))
        btnPlay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))
    }

    private fun updateHighScore() {
        val best = scoreManager.getHighScore()
        highScoreText.text = "BEST: $best"
    }

    private fun startGame() {
        val intent = Intent(this, GameActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun openLeaderboard() {
        val intent = Intent(this, LeaderboardActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateHighScore()
    }
}
