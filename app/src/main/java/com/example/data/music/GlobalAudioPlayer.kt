package com.example.data.music

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object GlobalAudioPlayer {
  private var mediaPlayer: MediaPlayer? = null
  private var currentTrackUrl: String? = null

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private val _activeTrackUrl = MutableStateFlow<String?>(null)
  val activeTrackUrl: StateFlow<String?> = _activeTrackUrl.asStateFlow()

  fun play(url: String, onCompletion: (() -> Unit)? = null) {
    playTrack(url, onCompletion)
  }

  fun playTrack(url: String, onCompletion: (() -> Unit)? = null) {
    if (url.isBlank()) return

    // If already playing this track, pause or toggle
    if (currentTrackUrl == url && mediaPlayer?.isPlaying == true) {
      pause()
      return
    }

    // Stop and reset existing player
    stop()

    try {
      mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        )
        setDataSource(url)
        setOnPreparedListener { mp ->
          mp.start()
          _isPlaying.value = true
          _activeTrackUrl.value = url
          currentTrackUrl = url
        }
        setOnCompletionListener {
          _isPlaying.value = false
          _activeTrackUrl.value = null
          currentTrackUrl = null
          onCompletion?.invoke()
        }
        setOnErrorListener { _, what, extra ->
          Log.e("GlobalAudioPlayer", "Error playing audio what=$what extra=$extra")
          stop()
          true
        }
        prepareAsync()
      }
    } catch (e: Exception) {
      Log.e("GlobalAudioPlayer", "Failed to start audio playback", e)
      stop()
    }
  }

  fun pause() {
    try {
      mediaPlayer?.pause()
      _isPlaying.value = false
    } catch (e: Exception) {
      // ignore
    }
  }

  fun resume() {
    try {
      mediaPlayer?.start()
      _isPlaying.value = true
    } catch (e: Exception) {
      // ignore
    }
  }

  fun togglePlayPause(url: String) {
    if (_activeTrackUrl.value == url && _isPlaying.value) {
      pause()
    } else if (_activeTrackUrl.value == url && !_isPlaying.value) {
      resume()
    } else {
      playTrack(url)
    }
  }

  fun stop() {
    try {
      mediaPlayer?.stop()
      mediaPlayer?.release()
    } catch (e: Exception) {
      // ignore
    } finally {
      mediaPlayer = null
      currentTrackUrl = null
      _isPlaying.value = false
      _activeTrackUrl.value = null
    }
  }
}
