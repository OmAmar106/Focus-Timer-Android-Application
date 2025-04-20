package com.example.focustimerandroidapplication

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.*
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import android.content.ComponentName

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
    private var player: MediaPlayer? = null

    private lateinit var mediaNowPlaying: LinearLayout
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var songProgress: SeekBar
    private lateinit var playPauseButton: Button

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, MediaNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(cn.flattenToString()) == true
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", true)
//        AppCompatDelegate.setDefaultNightMode(
//            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
//        )
        if (!isNotificationListenerEnabled()) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

//        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
//        startActivity(intent)

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

        if (savedInstanceState != null) {
            initialTime = savedInstanceState.getLong("initialTime", 60000L)
            totalTime = savedInstanceState.getLong("totalTime", initialTime)
            timeLeft = savedInstanceState.getLong("timeLeft", totalTime)
            isTimerRunning = savedInstanceState.getBoolean("isTimerRunning", false)
            startButton.text = savedInstanceState.getString("startButton.text")
            resetButton.isEnabled = savedInstanceState.getBoolean("resetButton.isEnabled")
            val seconds = timeLeft / 1000
            circularView.setTime((seconds / 60).toInt(), (seconds % 60).toInt())
            circularView.setProgress(timeLeft.toFloat() / totalTime)

            if (isTimerRunning) startTimer(timeLeft)
        } else {
            val seconds = timeLeft / 1000
            circularView.setTime((seconds / 60).toInt(), (seconds % 60).toInt())
        }


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
            if(startButton.text.toString()=="Snooze"){
                player?.stop()
                player?.release()
                player = null
                startButton.text = "Start"
                timeLeft = initialTime
                val seconds = timeLeft / 1000
                circularView.setTime((seconds / 60).toInt(), (seconds % 60).toInt())
                circularView.setProgress(1f)
            }
            else if (!isTimerRunning) {
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

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isTimerRunning) {
                Toast.makeText(this, "Can't change theme while timer is running", Toast.LENGTH_SHORT).show()
                themeSwitch.isChecked = !isChecked
                return@setOnCheckedChangeListener
            }
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            recreate()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("initialTime", initialTime)
        outState.putLong("totalTime", totalTime)
        outState.putLong("timeLeft", timeLeft)
        outState.putBoolean("isTimerRunning", isTimerRunning)
        outState.putString("startButton.text",startButton.text.toString())
        outState.putBoolean("resetButton.isEnabled",resetButton.isEnabled)
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.stop()
        player?.release()
        player = null
    }

    private fun startTimer(startTime: Long) {
        requestDnd(true)
        setStatusBarColor(Color.parseColor("#FF69B4"))
        isTimerRunning = true

        timer = object : CountDownTimer(startTime, 50) {
            override fun onTick(millisUntilFinished: Long) {
                circularView.setEditable(false)
                timeLeft = millisUntilFinished

                circularView.setProgress(millisUntilFinished.toFloat() / totalTime)

                if (millisUntilFinished % 1000L < 50L) {
                    val seconds = millisUntilFinished / 1000
                    circularView.setTime((seconds / 60).toInt(), (seconds % 60).toInt())
                }
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
        if(startButton.text.toString()=="Snooze"){
            return
        }
        circularView.setEditable(true)
        timer?.cancel()
        isTimerRunning = false
        totalTime = initialTime
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
        startButton.text = "Snooze"
        resetButton.isEnabled = true
        requestDnd(false)
        player = MediaPlayer.create(this, R.raw.timer_end_sound)
        player?.isLooping = true
        player?.start()
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
                    if (enable) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                    else NotificationManager.INTERRUPTION_FILTER_ALL
                )
            }
        }
    }

    override fun onBackPressed() {
        if (!isTimerRunning) super.onBackPressed()
    }
}

// To do :
// Debug the Mode Switching Error - Done
// Make the timer go smoother - Done
// Enable music even when dnd is on - Done
// Add a Alarm like thing after the end of the timer - Done
// Show music being played