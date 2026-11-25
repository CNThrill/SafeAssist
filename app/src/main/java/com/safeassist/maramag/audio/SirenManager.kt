package com.safeassist.maramag.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.safeassist.maramag.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the optional emergency siren audio.
 * Respects the device's permitted volume settings and never forces volume to maximum.
 */
class SirenManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun startSiren(): Boolean {
        if (_isPlaying.value) return true
        var player: MediaPlayer? = null
        return try {
            stopSiren()
            player = MediaPlayer().apply {
                // Set alarm attributes before setDataSource/prepare so Android routes through Alarm volume
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                context.resources.openRawResourceFd(R.raw.emergency_siren).use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                isLooping = true
                setOnErrorListener { _, _, _ ->
                    stopSiren()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
            _isPlaying.value = true
            true
        } catch (e: Exception) {
            Log.e("SirenManager", "Failed to start siren: ${e.localizedMessage}", e)
            try {
                player?.release()
            } catch (_: Exception) {}
            stopSiren()
            false
        }
    }

    fun stopSiren() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Log.w("SirenManager", "Error stopping siren: ${e.localizedMessage}")
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
        }
    }
}
