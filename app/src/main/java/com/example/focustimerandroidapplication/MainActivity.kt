package com.example.focustimerandroidapplication

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var circularView: CircularCountdownView
    private lateinit var startButton: Button
    private lateinit var resetButton: Button
    private lateinit var themeSwitch: SwitchCompat
    private lateinit var themeIcon: TextView
    private lateinit var logoText: TextView
    private lateinit var logoRow: LinearLayout
    private lateinit var logoImage: ImageView

    private var timer: CountDownTimer? = null
    private var initialTime = 60000L
    private var totalTime = initialTime
    private var timeLeft = totalTime
    private var isTimerRunning = false
    private lateinit var mainLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {

        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", true)
//        AppCompatDelegate.setDefaultNightMode(
//            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
//        )

        logoImage = findViewById(R.id.logoImage)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainLayout = findViewById<ConstraintLayout>(R.id.main)

        circularView = findViewById(R.id.circularCountdown)
        startButton = findViewById(R.id.startButton)
        resetButton = findViewById(R.id.stopButton)
        themeSwitch = findViewById(R.id.themeSwitch)
        themeIcon = findViewById(R.id.themeIcon)
        logoText = findViewById(R.id.logoText)
        logoRow = findViewById(R.id.logoRow)
        resetButton.text = "Reset"
        resetButton.isEnabled = false
        themeSwitch.isChecked = isDarkMode
        updateThemeUI(isDarkMode)

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            recreate()
        }

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

    private fun updateThemeUI(isDark: Boolean) {
        themeIcon.text = if (isDark) "🌙" else "☀️"
        logoText.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        resetButton.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        startButton.setTextColor(Color.BLACK)
        circularView.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        mainLayout.setBackgroundColor(if (isDark) Color.BLACK else Color.WHITE)
        if(isDark){
            logoRow.background = ContextCompat.getDrawable(this, R.drawable.logo_background)
        }
        else {
            logoRow.background = ContextCompat.getDrawable(this, R.drawable.logo_background_white)
        }
//        if (isDark) {
//            logoImage.setImageResource(R.drawable.logo)
//        } else {
//            logoImage.setImageResource(R.drawable.logo_white)
//        }

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
                circularView.setProgress((millisUntilFinished - 1000).toFloat() / totalTime)
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
