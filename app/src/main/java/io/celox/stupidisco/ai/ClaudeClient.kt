package io.celox.stupidisco.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class ClaudeClient(
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = API_URL
) {
    companion object {
        private const val TAG = "ClaudeClient"
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-sonnet-4-5-20250929"
        private const val MAX_TOKENS = 600
        private val SYSTEM_PROMPT =
            "Du bist ein erfahrener Senior-Entwickler (15+ Jahre) in einem " +
            "Vorstellungsgespräch. Du erhältst ein Live-Transkript einer " +
            "gesprochenen Frage. Das Transkript kann Fragmente, Wiederholungen, " +
            "Echo, Versprecher oder fehlende Wörter enthalten.\n\n" +
            "SCHRITT 1 — FRAGE VERSTEHEN (intern, nicht ausgeben):\n" +
            "Lies das Transkript sehr genau. Rekonstruiere die tatsächlich " +
            "gemeinte Frage. Berücksichtige Kontext, Fachbegriffe und was in " +
            "einem Interview typischerweise gefragt wird. Bei Mehrdeutigkeit: " +
            "wähle die wahrscheinlichste Interpretation.\n\n" +
            "SCHRITT 2 — ANTWORT (das ist dein Output):\n" +
            "Beantworte exakt die erkannte Frage. Nicht ein verwandtes Thema, " +
            "nicht eine allgemeine Übersicht — sondern präzise das, was gefragt wurde.\n\n" +
            "FORMAT (Markdown):\n" +
            "**Kernaussage** in einem Satz.\n" +
            "- Detail, Trade-off oder Praxisbeispiel (max. 1 Satz)\n" +
            "- Weitere Details (max. 4-5 Stichpunkte insgesamt)\n" +
            "Nutze **fett** für Schlüsselbegriffe, `code` für technische Terme, " +
            "und - für Aufzählungen.\n\n" +
            "REGELN:\n" +
            "- Deutsch, fachlich korrekt, auf den Punkt\n" +
            "- Antworte IMMER — auch bei schlechtem Transkript\n" +
            "- Keine Vorrede, keine Meta-Kommentare\n" +
            "- Zeige Tiefenwissen: Trade-offs, Best Practices, konkrete Erfahrung\n" +
            "- Wenn die Frage nicht technisch ist (Soft Skills, Gehalt, " +
            "Motivation), antworte trotzdem souverän und überzeugend"
    }

    suspend fun streamAnswer(
        question: String,
        onToken: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", MAX_TOKENS)
                put("stream", true)
                put("system", SYSTEM_PROMPT)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", question)
                    })
                })
            }

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                onError("Claude API error ${response.code}: $errorBody")
                return@withContext
            }

            val reader = response.body?.byteStream()?.bufferedReader()
            if (reader == null) {
                onError("Empty response from Claude")
                return@withContext
            }

            parseSSE(reader, onToken, onComplete, onError)
        } catch (e: Exception) {
            Log.e(TAG, "Stream error", e)
            onError("Claude error: ${e.message}")
        }
    }

    private fun parseSSE(
        reader: BufferedReader,
        onToken: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        var currentEvent = ""

        reader.useLines { lines ->
            for (line in lines) {
                when {
                    line.startsWith("event: ") -> {
                        currentEvent = line.removePrefix("event: ").trim()
                    }
                    line.startsWith("data: ") -> {
                        val data = line.removePrefix("data: ").trim()
                        when (currentEvent) {
                            "content_block_delta" -> {
                                try {
                                    val json = JSONObject(data)
                                    val delta = json.optJSONObject("delta")
                                    val text = delta?.optString("text", "") ?: ""
                                    if (text.isNotEmpty()) {
                                        onToken(text)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing delta", e)
                                }
                            }
                            "message_stop" -> {
                                onComplete()
                                return
                            }
                            "error" -> {
                                try {
                                    val json = JSONObject(data)
                                    val error = json.optJSONObject("error")
                                    val msg = error?.optString("message", "Unknown error")
                                    onError("Claude: $msg")
                                } catch (_: Exception) {
                                    onError("Claude stream error")
                                }
                                return
                            }
                        }
                    }
                }
            }
        }
        // If we get here without message_stop, still complete
        onComplete()
    }
}
