package com.example.focustimerandroidapplication

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.graphics.Bitmap

class MediaNotificationListener : NotificationListenerService() {
    private var mediaController: MediaController? = null
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val extras = sbn?.notification?.extras ?: return
        if (!extras.containsKey("android.mediaSession")) return
        val token = extras.getParcelable<MediaSession.Token>("android.mediaSession") ?: return
        try {
            mediaController = MediaController(applicationContext, token)
            mediaController?.registerCallback(controllerCallback)

            updateMediaInfo(mediaController)
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
        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        val position = state?.position
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val intent = Intent("media_info_update")
        intent.putExtra("title", title)
        intent.putExtra("artist", artist)
        intent.putExtra("albumArt", albumArt)
        intent.putExtra("position", position)
        intent.putExtra("isPlaying", isPlaying)
        sendBroadcast(intent)
//        val message = "🎵 $title - $artist\n▶️ Playing: $isPlaying\n⏱️ Position: ${position}ms"
//        Toast.makeText(applicationContext, "message", Toast.LENGTH_LONG).show()

    }

    override fun onDestroy() {
        super.onDestroy()
        mediaController?.unregisterCallback(controllerCallback)
    }
}
