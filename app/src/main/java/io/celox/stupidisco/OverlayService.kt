package io.celox.stupidisco

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.pm.ServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.celox.stupidisco.ai.ClaudeClient
import io.celox.stupidisco.audio.AudioRecorder
import io.celox.stupidisco.data.ApiKeyStore
import io.celox.stupidisco.data.SessionLogger
import io.celox.stupidisco.model.AppState
import io.celox.stupidisco.model.AppStatus
import io.celox.stupidisco.stt.DeepgramClient
import io.celox.stupidisco.ui.OverlayContent
import io.celox.stupidisco.ui.StupidiscoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OverlayService : Service() {
    companion object {
        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "stupidisco_overlay"
        private const val NOTIFICATION_ID = 1
        private const val MIN_ANSWER_HEIGHT_DP = 60
        private const val MAX_ANSWER_HEIGHT_DP = 400
        private const val DEFAULT_ANSWER_HEIGHT_DP = 120
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val state = MutableStateFlow(AppState())
    private val answerAreaHeightDp = MutableStateFlow(DEFAULT_ANSWER_HEIGHT_DP)

    private lateinit var windowManager: WindowManager
    private lateinit var apiKeyStore: ApiKeyStore
    private lateinit var sessionLogger: SessionLogger
    private var overlayView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null

    private var audioRecorder: AudioRecorder? = null
    private var deepgramClient: DeepgramClient? = null
    private var claudeClient: ClaudeClient? = null
    private var recordingJob: Job? = null

    private var finalTranscriptParts = mutableListOf<String>()
    private var recordingStartTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        apiKeyStore = ApiKeyStore(this)
        sessionLogger = SessionLogger(this)
        claudeClient = ClaudeClient(apiKeyStore.getAnthropicKey())

        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        showOverlay()
    }

    override fun onDestroy() {
        stopRecordingSession()
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "stupidisco Overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the overlay running"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("stupidisco")
            .setContentText("Interview-Assistent aktiv")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }
        overlayParams = params

        val density = resources.displayMetrics.density

        val lifecycleOwner = OverlayLifecycleOwner()

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                val currentState by state.collectAsState()
                val answerHeight by answerAreaHeightDp.collectAsState()
                StupidiscoTheme {
                    OverlayContent(
                        state = currentState,
                        answerAreaHeightDp = answerHeight,
                        onMicClick = { onMicButtonClicked() },
                        onCopy = { copyAnswer() },
                        onRegenerate = { regenerateAnswer() },
                        onClose = { stopSelf() },
                        onDrag = { dx, dy ->
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                            try {
                                windowManager.updateViewLayout(
                                    overlayView, params
                                )
                            } catch (_: Exception) {}
                        },
                        onResize = { deltaY ->
                            val deltaDp = (deltaY / density).toInt()
                            answerAreaHeightDp.update {
                                (it + deltaDp).coerceIn(
                                    MIN_ANSWER_HEIGHT_DP,
                                    MAX_ANSWER_HEIGHT_DP
                                )
                            }
                        }
                    )
                }
            }
        }

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        overlayView = composeView
        windowManager.addView(composeView, params)
    }

    private fun onMicButtonClicked() {
        when (state.value.status) {
            is AppStatus.Recording -> stopRecordingAndProcess()
            is AppStatus.Ready, is AppStatus.Error -> startRecordingSession()
            is AppStatus.Thinking -> {} // ignore during thinking
        }
    }

    private fun startRecordingSession() {
        finalTranscriptParts.clear()
        recordingStartTime = System.currentTimeMillis()

        state.update {
            it.copy(
                status = AppStatus.Recording,
                transcript = "",
                partialTranscript = "",
                answer = "",
                isAnswerComplete = false
            )
        }

        audioRecorder = AudioRecorder()

        deepgramClient = DeepgramClient(
            apiKey = apiKeyStore.getDeepgramKey(),
            onPartialTranscript = { text ->
                state.update { it.copy(partialTranscript = text) }
            },
            onFinalTranscript = { text ->
                finalTranscriptParts.add(text)
                val fullTranscript = finalTranscriptParts.joinToString(" ")
                state.update {
                    it.copy(
                        transcript = fullTranscript,
                        partialTranscript = ""
                    )
                }
            },
            onError = { error ->
                Log.e(TAG, "Deepgram error: $error")
                state.update { it.copy(status = AppStatus.Error(error)) }
            }
        )

        deepgramClient?.connect()

        recordingJob = scope.launch {
            audioRecorder?.startRecording()?.collect { chunk ->
                deepgramClient?.sendAudio(chunk)
            }
        }
    }

    private fun stopRecordingAndProcess() {
        recordingJob?.cancel()
        recordingJob = null
        audioRecorder?.stopRecording()
        deepgramClient?.disconnect()

        val fullTranscript = finalTranscriptParts.joinToString(" ").let { final ->
            val partial = state.value.partialTranscript
            if (partial.isNotBlank()) "$final $partial".trim() else final
        }

        if (fullTranscript.isBlank()) {
            state.update { it.copy(status = AppStatus.Ready) }
            return
        }

        state.update {
            it.copy(
                status = AppStatus.Thinking,
                transcript = fullTranscript,
                partialTranscript = "",
                answer = ""
            )
        }

        val firstChunkTime = System.currentTimeMillis()
        var firstTokenReceived = false

        scope.launch {
            claudeClient?.streamAnswer(
                question = fullTranscript,
                onToken = { token ->
                    if (!firstTokenReceived) {
                        firstTokenReceived = true
                        val latency = System.currentTimeMillis() - firstChunkTime
                        Log.d(TAG, "First token latency: ${latency}ms")
                    }
                    state.update { it.copy(answer = it.answer + token) }
                },
                onComplete = {
                    val totalLatency = System.currentTimeMillis() - firstChunkTime
                    Log.d(TAG, "Total answer latency: ${totalLatency}ms")

                    val newCount = state.value.questionCount + 1
                    state.update {
                        it.copy(
                            status = AppStatus.Ready,
                            isAnswerComplete = true,
                            questionCount = newCount
                        )
                    }
                    sessionLogger.logQA(newCount, fullTranscript, state.value.answer)
                },
                onError = { error ->
                    state.update { it.copy(status = AppStatus.Error(error)) }
                }
            )
        }
    }

    private fun stopRecordingSession() {
        recordingJob?.cancel()
        recordingJob = null
        audioRecorder?.stopRecording()
        audioRecorder = null
        deepgramClient?.disconnect()
        deepgramClient = null
    }

    private fun copyAnswer() {
        val answer = state.value.answer
        if (answer.isBlank()) return

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("stupidisco answer", answer))
        Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
    }

    private fun regenerateAnswer() {
        val transcript = state.value.transcript
        if (transcript.isBlank()) return

        state.update {
            it.copy(
                status = AppStatus.Thinking,
                answer = "",
                isAnswerComplete = false
            )
        }

        scope.launch {
            claudeClient?.streamAnswer(
                question = transcript,
                onToken = { token ->
                    state.update { it.copy(answer = it.answer + token) }
                },
                onComplete = {
                    state.update {
                        it.copy(
                            status = AppStatus.Ready,
                            isAnswerComplete = true
                        )
                    }
                },
                onError = { error ->
                    state.update { it.copy(status = AppStatus.Error(error)) }
                }
            )
        }
    }

    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        init {
            savedStateRegistryController.performRestore(null)
        }

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }
}
