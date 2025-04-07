package com.example.focustimerandroidapplication

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
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
    private var timeLeft = totalTime
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBarColor(ContextCompat.getColor(this, android.R.color.background_dark))
        setContentView(R.layout.activity_main)

        circularView = findViewById(R.id.circularCountdown)
        startButton = findViewById(R.id.startButton)
        resetButton = findViewById(R.id.stopButton)

        resetButton.text = "Reset"
        resetButton.isEnabled = false

        circularView.onTimeEdited = { minutes, seconds ->
            val newTime = (minutes * 60 + seconds) * 1000L
            initialTime = newTime
            totalTime = newTime
            timeLeft = newTime
            circularView.setProgress(1f)
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

    private fun startTimer(startTime: Long) {
        requestDnd(true)
        setStatusBarColor(Color.parseColor("#FF69B4"))
        isTimerRunning = true

        timer = object : CountDownTimer(startTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                circularView.setEditable(false)
                timeLeft = millisUntilFinished
                val seconds = millisUntilFinished / 1000
                circularView.setTime((seconds / 60).toInt(), (seconds % 60).toInt())
                circularView.setProgress((millisUntilFinished-1000).toFloat() / totalTime)
            }

            override fun onFinish() {
                finishTimer()
            }
        }.start()
    }

    private fun pauseTimer() {
        timer?.cancel()
        isTimerRunning = false
    }

    private fun resetTimer() {
        circularView.setEditable(true)
        timer?.cancel()
        isTimerRunning = false
        timeLeft = initialTime
        val seconds = timeLeft / 1000
        circularView.setTime((seconds / 60).toInt(), (seconds % 60).toInt())
        circularView.setProgress(1f)
        startButton.text = "Start"
        resetButton.isEnabled = false
        requestDnd(false)
        setStatusBarColor(ContextCompat.getColor(this, android.R.color.background_dark))
    }

    private fun finishTimer() {
        circularView.setEditable(true)
        isTimerRunning = false
        circularView.setTimeText("Done!")
        circularView.setProgress(0f)
        startButton.text = "Start"
        resetButton.isEnabled = true
        requestDnd(false)
        setStatusBarColor(ContextCompat.getColor(this, android.R.color.background_dark))
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

    override fun onBackPressed() {
        if (!isTimerRunning) super.onBackPressed()
    }
}
