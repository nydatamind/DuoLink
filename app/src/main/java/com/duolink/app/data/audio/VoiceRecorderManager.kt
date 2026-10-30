package com.duolink.app.data.audio

import android.content.Context
import android.media.MediaRecorder
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
import java.io.File

data class VoiceRecordResult(
    val file: File,
    val durationMs: Long,
    val waveform: List<Int>
)

class VoiceRecorderManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    companion object {
        private const val TAG = "VoiceRecorderManager"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var startTimeMs = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Int>>(emptyList())
    val amplitudes: StateFlow<List<Int>> = _amplitudes.asStateFlow()

    private var meterJob: Job? = null

    fun startRecording(): Boolean {
        try {
            val audioDir = File(context.filesDir, "voice_messages").apply { mkdirs() }
            val file = File(audioDir, "duolink_voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(64000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            startTimeMs = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationMs.value = 0L
            _amplitudes.value = emptyList()

            startAmplitudeMeter()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            cancelRecording()
            return false
        }
    }

    private fun startAmplitudeMeter() {
        meterJob?.cancel()
        meterJob = scope.launch {
            val ampList = mutableListOf<Int>()
            while (isActive && _isRecording.value) {
                delay(100)
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    // Normalize between 5 and 100
                    val normalized = (maxAmp / 327.67).toInt().coerceIn(5, 100)
                    ampList.add(normalized)
                    _amplitudes.value = ampList.takeLast(50)
                    _recordingDurationMs.value = System.currentTimeMillis() - startTimeMs
                } catch (e: Exception) {}
            }
        }
    }

    fun stopRecording(): VoiceRecordResult? {
        if (!_isRecording.value) return null
        meterJob?.cancel()

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder: ${e.message}")
        }
        mediaRecorder = null
        _isRecording.value = false

        val duration = System.currentTimeMillis() - startTimeMs
        val file = currentOutputFile
        val wave = _amplitudes.value

        return if (file != null && file.exists() && duration >= 500) {
            VoiceRecordResult(file, duration, wave)
        } else {
            file?.delete()
            null
        }
    }

    fun cancelRecording() {
        meterJob?.cancel()
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {}
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {}
        mediaRecorder = null
        _isRecording.value = false
        currentOutputFile?.delete()
        currentOutputFile = null
        _amplitudes.value = emptyList()
        _recordingDurationMs.value = 0L
    }
}

