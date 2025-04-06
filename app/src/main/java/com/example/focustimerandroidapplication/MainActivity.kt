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
    private lateinit var stopButton: Button
    private var timer: CountDownTimer? = null

    private val totalTime = 60000L
    private var timeLeft = totalTime
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBarColor(ContextCompat.getColor(this, android.R.color.background_dark))
        setContentView(R.layout.activity_main)

        circularView = findViewById(R.id.circularCountdown)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        startButton.setOnClickListener {
            startTimer()
            startButton.isEnabled = false
            stopButton.isEnabled = true
        }

        stopButton.setOnClickListener {
            stopTimer()
        }
    }

    private fun startTimer() {
        requestDnd(true)
        setStatusBarColor(Color.parseColor("#FF69B4")) // pink
        isTimerRunning = true

        timer = object : CountDownTimer(totalTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                val seconds = millisUntilFinished / 1000
                circularView.setTimeText(String.format("%02d:%02d", seconds / 60, seconds % 60))
                circularView.setProgress((totalTime - timeLeft).toFloat() / totalTime)
            }

            override fun onFinish() {
                finishTimer()
            }
        }.start()
    }

    private fun stopTimer() {
        timer?.cancel()
        finishTimer()
    }

    private fun finishTimer() {
        isTimerRunning = false
        circularView.setTimeText("Done!")
        circularView.setProgress(1f)
        startButton.isEnabled = true
        stopButton.isEnabled = false
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
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
