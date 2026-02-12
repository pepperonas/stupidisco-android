package io.celox.stupidisco.model

sealed class AppStatus {
    data object Ready : AppStatus()
    data object Recording : AppStatus()
    data object Thinking : AppStatus()
    data class Error(val message: String) : AppStatus()
}

data class AppState(
    val status: AppStatus = AppStatus.Ready,
    val transcript: String = "",
    val partialTranscript: String = "",
    val answer: String = "",
    val isAnswerComplete: Boolean = false,
    val questionCount: Int = 0
)
