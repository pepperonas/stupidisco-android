package io.celox.stupidisco.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class AudioRecorder {
    companion object {
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        // 100ms chunks: 16000 samples/sec * 0.1 sec * 2 bytes/sample = 3200 bytes
        private const val CHUNK_SIZE = 3200
    }

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<ByteArray> = callbackFlow {
        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT),
            CHUNK_SIZE * 2
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        withContext(Dispatchers.IO) {
            val buffer = ByteArray(CHUNK_SIZE)
            while (isActive && isRecording) {
                val bytesRead = audioRecord?.read(buffer, 0, CHUNK_SIZE) ?: -1
                if (bytesRead > 0) {
                    trySend(buffer.copyOf(bytesRead))
                }
            }
        }

        awaitClose {
            stopRecording()
        }
    }

    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
    }
}
