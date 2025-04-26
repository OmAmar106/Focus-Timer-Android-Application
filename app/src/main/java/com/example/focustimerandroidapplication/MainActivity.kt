package com.example.focustimerandroidapplication

import android.animation.ValueAnimator
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
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

interface MediaInfoListener {
    fun onMediaInfoUpdatedWithImage(
        title: String?,
        artist: String?,
        isPlaying: Boolean,
        position: Long?,
        albumArt: Bitmap?,
        duration: Long?
    )
}

object MediaInfoDispatcher {
    var listener: MediaInfoListener? = null
}

class MediaNotificationListener : NotificationListenerService() {
    private var mediaController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateMediaInfo(mediaController)
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications?.forEach { sbn ->
            handleNotification(sbn)
        }
    }

    private fun handleNotification(sbn: StatusBarNotification?){
        val extras = sbn?.notification?.extras ?: return
        if (!extras.containsKey("android.mediaSession")) return
        val token = extras.getParcelable<MediaSession.Token>("android.mediaSession") ?: return
        try {
            mediaController = MediaController(applicationContext, token)
            mediaController?.registerCallback(controllerCallback)
            updateMediaInfo(mediaController)
            handler.post(updateRunnable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val extras = sbn?.notification?.extras ?: return
        if (!extras.containsKey("android.mediaSession")) return
        val token = extras.getParcelable<MediaSession.Token>("android.mediaSession") ?: return
        try {
            mediaController = MediaController(applicationContext, token)
            mediaController?.registerCallback(controllerCallback)
            updateMediaInfo(mediaController)
            handler.post(updateRunnable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaInfo(mediaController)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaInfo(mediaController)
        }
    }

    private fun updateMediaInfo(controller: MediaController?) {
        val metadata = controller?.metadata
        val state = controller?.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING

        val basePosition = state?.position ?: 0L
        val lastUpdateTime = state?.lastPositionUpdateTime ?: 0L
        val timeDiff = SystemClock.elapsedRealtime() - lastUpdateTime
        val position = if (isPlaying) basePosition + timeDiff else basePosition
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)

        val imageBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        MediaInfoDispatcher.listener?.onMediaInfoUpdatedWithImage(
            title, artist, isPlaying, position, imageBitmap,duration
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaController?.unregisterCallback(controllerCallback)
        handler.removeCallbacks(updateRunnable)
    }
}

class MainActivity : AppCompatActivity(), MediaInfoListener {

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

    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var musicProgress: ProgressBar
    private lateinit var PlayPauseButton: ImageButton
    private lateinit var albumArt: ImageView
    private lateinit var musicPlayer: LinearLayout
    private lateinit var timeElapsed: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, MediaNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(cn.flattenToString()) == true
    }

    private var isAnimating = false
    private val handler = Handler(Looper.getMainLooper())
    private var colorIndex = 0

    private val colors = listOf(
        Color.parseColor("#FF6F61"),
        Color.parseColor("#3F88C5"),
        Color.parseColor("#2F3061"),
        Color.parseColor("#43B929")
    )

    private val updateBackgroundRunnable = object : Runnable {
        override fun run() {
            val startColor = colors[colorIndex % colors.size]
            val endColor = colors[(colorIndex + 1) % colors.size]

            val colorAnimator = ValueAnimator.ofArgb(startColor, endColor)
            colorAnimator.duration = 800
            colorAnimator.addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                val drawable = musicPlayer.background.mutate() as GradientDrawable
                drawable.setColor(color)
            }
            colorAnimator.start()

            colorIndex++
            handler.postDelayed(this, 800)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        MediaInfoDispatcher.listener = this
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val handler = Handler(Looper.getMainLooper())

        handler.post(updateBackgroundRunnable)

        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", true)

        if (!isNotificationListenerEnabled()) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }


        songTitle = findViewById(R.id.songTitle)
        songArtist = findViewById(R.id.artistName)
        musicProgress = findViewById(R.id.musicProgress)
        musicPlayer = findViewById(R.id.musicPlayer)
        logoImage = findViewById(R.id.logoImage)
        mainLayout = findViewById(R.id.main)
        circularView = findViewById(R.id.circularCountdown)
        startButton = findViewById(R.id.startButton)
        resetButton = findViewById(R.id.stopButton)
        themeSwitch = findViewById(R.id.themeSwitch)
        themeIcon = findViewById(R.id.themeIcon)
        logoText = findViewById(R.id.logoText)
        logoRow = findViewById(R.id.logoRow)
        totalTimeText = findViewById(R.id.totalTime)
        timeElapsed = findViewById(R.id.timeElapsed)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        PlayPauseButton = findViewById(R.id.playPauseButton)

        albumArt = findViewById(R.id.albumArt)
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
            if (startButton.text.toString() == "Snooze") {
                player?.stop()
                player?.release()
                player = null
                startButton.text = "Start"
                timeLeft = initialTime
                val seconds = timeLeft / 1000
                circularView.setTime((seconds / 60).toInt(), (seconds % 60).toInt())
                circularView.setProgress(1f)
            } else if (!isTimerRunning) {
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


    override fun onMediaInfoUpdatedWithImage(title: String?, artist: String?, isPlaying: Boolean, position: Long?, albumArt1: Bitmap?,duration: Long?) {
        songTitle.text = title ?: "Me, Myself &amp; I"
        songArtist.text = artist ?: "G-Eazy, Bebe Rexha"
        PlayPauseButton.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        val hasSong = !title.isNullOrEmpty() || !artist.isNullOrEmpty()

        if (hasSong && isPlaying && !isAnimating) {
            isAnimating = true
            handler.post(updateBackgroundRunnable)
        } else if ((!hasSong || !isPlaying) && isAnimating) {
            isAnimating = false
            handler.removeCallbacks(updateBackgroundRunnable)
        }
        if (albumArt1 != null) {
            albumArt.setImageBitmap(albumArt1)
            val drawable = BitmapDrawable(resources, albumArt1)
//            val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.TRANSPARENT, Color.parseColor("#80000000")))
//            gradientDrawable.cornerRadius = 16f  // Optional: adjust the corner radius to match your design
//            val layerDrawable = LayerDrawable(arrayOf(drawable, gradientDrawable))
//            musicPlayer.background = layerDrawable
        }
        if (position != null && duration != null && duration > 0) {
            musicProgress.max = duration.toInt()
            musicProgress.progress = position.toInt()
            totalTimeText.text = String.format("%01d:%02d", (duration / 1000 / 60).toInt(), (duration / 1000 % 60).toInt())
            timeElapsed.text = String.format("%01d:%02d", (position / 1000 / 60).toInt(), (position / 1000 % 60).toInt())+'/'
        }
    }


    private fun updateThemeUI(isDark: Boolean) {
        themeIcon.text = if (isDark) "\uD83C\uDF19" else "\u2600\uFE0F"
        logoText.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        resetButton.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        startButton.setTextColor(Color.BLACK)
        circularView.setTextColor(if (isDark) Color.WHITE else Color.BLACK)
        mainLayout.setBackgroundColor(if (isDark) Color.BLACK else Color.WHITE)
        logoRow.background = ContextCompat.getDrawable(this,
            if (isDark) R.drawable.logo_background else R.drawable.logo_background_white)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("initialTime", initialTime)
        outState.putLong("totalTime", totalTime)
        outState.putLong("timeLeft", timeLeft)
        outState.putBoolean("isTimerRunning", isTimerRunning)
        outState.putString("startButton.text", startButton.text.toString())
        outState.putBoolean("resetButton.isEnabled", resetButton.isEnabled)
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaInfoDispatcher.listener = null
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
        if (startButton.text.toString() == "Snooze") return
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
            if (notificationManager.isNotificationPolicyAccessGranted) {
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