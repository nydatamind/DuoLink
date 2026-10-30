package com.duolink.app.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VoicePlayerManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    companion object {
        private const val TAG = "VoicePlayerManager"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentUri = MutableStateFlow<String?>(null)
    val currentUri: StateFlow<String?> = _currentUri.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun play(uriString: String) {
        if (_currentUri.value == uriString && mediaPlayer != null) {
            if (!_isPlaying.value) {
                mediaPlayer?.start()
                _isPlaying.value = true
                startProgressTicker()
                return
            }
        }

        stop()

        try {
            val player = MediaPlayer()
            if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                player.setDataSource(context, Uri.parse(uriString))
            } else {
                player.setDataSource(uriString)
            }

            player.prepare()
            applySpeed(player, _playbackSpeed.value)
            player.start()

            _currentUri.value = uriString
            _durationMs.value = player.duration.toLong()
            _isPlaying.value = true
            mediaPlayer = player

            player.setOnCompletionListener {
                _isPlaying.value = false
                _currentPositionMs.value = 0L
                progressJob?.cancel()
            }

            startProgressTicker()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}")
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                progressJob?.cancel()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        }
    }

    fun togglePlaybackSpeed(): Float {
        val nextSpeed = when (_playbackSpeed.value) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        _playbackSpeed.value = nextSpeed
        mediaPlayer?.let { applySpeed(it, nextSpeed) }
        return nextSpeed
    }

    private fun applySpeed(player: MediaPlayer, speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = player.playbackParams ?: PlaybackParams()
                params.speed = speed
                player.playbackParams = params
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting playback speed: ${e.message}")
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPositionMs.value = it.currentPosition.toLong()
                    }
                }
                delay(100)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentUri.value = null
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
    }
}

