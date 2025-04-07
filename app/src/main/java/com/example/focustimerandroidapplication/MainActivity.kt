package com.example.focustimerandroidapplication

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var circularView: CircularCountdownView
    private lateinit var startButton: Button
    private lateinit var resetButton: Button
    private var timer: CountDownTimer? = null

    private var initialTime = 60000L
    private var totalTime = initialTime
    private var prev = initialTime
    private var timeLeft = totalTime
    private var isTimerRunning = false
    private var isTimerPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBarColor(ContextCompat.getColor(this, android.R.color.background_dark))
        setContentView(R.layout.activity_main)

        circularView = findViewById(R.id.circularCountdown)
        startButton = findViewById(R.id.startButton)
        resetButton = findViewById(R.id.stopButton)
        resetButton.text = "Reset"
        resetButton.isEnabled = false

        if (savedInstanceState != null) {
            timeLeft = savedInstanceState.getLong("timeLeft", totalTime)
            isTimerRunning = savedInstanceState.getBoolean("isRunning", false)
            if (isTimerRunning) {
                startTimer(timeLeft)
                startButton.text = "Pause"
                resetButton.isEnabled = true
//                val k = 1
//                circularView.setProgress(k.toFloat())
            } else {
                val seconds = timeLeft / 1000
                circularView.setTimeText(String.format("%02d:%02d", seconds / 60, seconds % 60))
                circularView.setProgress(timeLeft.toFloat() / totalTime)
            }
        }

        startButton.setOnClickListener {
            if (!isTimerRunning) {
                startTimer(timeLeft)
                startButton.text = "Pause"
                resetButton.isEnabled = true
            } else {
                pauseTimer()
                startButton.text = "Start"
            }
        }

        resetButton.setOnClickListener {
            resetTimer()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("timeLeft", timeLeft)
        outState.putBoolean("isRunning", isTimerRunning)
    }

    private fun startTimer(startTime: Long, isTimerPaused1: Boolean = false) {
        requestDnd(true)
        setStatusBarColor(Color.parseColor("#FF69B4"))
        isTimerRunning = true
        isTimerPaused = false
        if (isTimerPaused1) {
            prev = startTime
        }
        timer = object : CountDownTimer(startTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                val seconds = millisUntilFinished / 1000
                circularView.setTimeText(String.format("%02d:%02d", seconds / 60, seconds % 60))
                circularView.setProgress(millisUntilFinished.toFloat() / totalTime)
            }

            override fun onFinish() {
                finishTimer()
            }
        }.start()
    }

    private fun pauseTimer() {
        timer?.cancel()
        isTimerRunning = false
        isTimerPaused = true
    }

    private fun resetTimer() {
        timer?.cancel()
        isTimerRunning = false
        isTimerPaused = false
        timeLeft = initialTime
        val seconds = initialTime / 1000
        circularView.setTimeText(String.format("%02d:%02d", seconds / 60, seconds % 60))
        circularView.setProgress(1f)
        prev = initialTime
        startButton.text = "Start"
        startButton.isEnabled = true
        resetButton.isEnabled = false
        requestDnd(false)
        setStatusBarColor(ContextCompat.getColor(this, android.R.color.background_dark))
    }

    private fun finishTimer() {
        isTimerRunning = false
        circularView.setTimeText("Done!")
        circularView.setProgress(0f)
        startButton.text = "Start"
        startButton.isEnabled = true
        resetButton.isEnabled = false
        requestDnd(false)
        setStatusBarColor(ContextCompat.getColor(this, android.R.color.background_dark))
    }

    override fun onBackPressed() {
        if (!isTimerRunning) super.onBackPressed()
    }

    private fun setStatusBarColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = color
        }
    }

    private fun requestDnd(enable: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            } else {
                notificationManager.setInterruptionFilter(
                    if (enable) NotificationManager.INTERRUPTION_FILTER_NONE
                    else NotificationManager.INTERRUPTION_FILTER_ALL
                )
            }
        }
    }
}
