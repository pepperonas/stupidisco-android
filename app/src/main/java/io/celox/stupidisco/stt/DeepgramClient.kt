package io.celox.stupidisco.stt

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DeepgramClient(
    private val apiKey: String,
    private val onPartialTranscript: (String) -> Unit,
    private val onFinalTranscript: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "DeepgramClient"
        private const val DEEPGRAM_URL =
            "wss://api.deepgram.com/v1/listen?" +
            "model=nova-3&" +
            "language=de&" +
            "encoding=linear16&" +
            "sample_rate=16000&" +
            "channels=1&" +
            "interim_results=true&" +
            "smart_format=true"
    }

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun connect() {
        val request = Request.Builder()
            .url(DEEPGRAM_URL)
            .addHeader("Authorization", "Token $apiKey")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val channel = json.optJSONObject("channel") ?: return
                    val alternatives = channel.optJSONArray("alternatives") ?: return
                    if (alternatives.length() == 0) return

                    val transcript = alternatives.getJSONObject(0)
                        .optString("transcript", "")

                    if (transcript.isBlank()) return

                    val isFinal = json.optBoolean("is_final", false)

                    if (isFinal) {
                        onFinalTranscript(transcript)
                    } else {
                        onPartialTranscript(transcript)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
                onError("Deepgram error: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
            }
        })
    }

    fun sendAudio(data: ByteArray) {
        webSocket?.send(data.toByteString(0, data.size))
    }

    fun disconnect() {
        try {
            // Send close message to Deepgram
            webSocket?.send(ByteString.EMPTY)
            webSocket?.close(1000, "Done")
        } catch (_: Exception) {
        }
        webSocket = null
    }
}
