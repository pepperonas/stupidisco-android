package io.celox.stupidisco.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionLogger(context: Context) {
    private val sessionDir = File(context.filesDir, "sessions").also { it.mkdirs() }
    private val sessionFile: File
    private val startTime: Long = System.currentTimeMillis()

    init {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        sessionFile = File(sessionDir, "${dateFormat.format(Date())}.txt")
    }

    fun logQA(questionNumber: Int, question: String, answer: String) {
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        val hours = elapsed / 3600
        val minutes = (elapsed % 3600) / 60
        val seconds = elapsed % 60
        val timestamp = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

        val entry = buildString {
            if (sessionFile.exists() && sessionFile.length() > 0) {
                appendLine()
                appendLine("====================================")
                appendLine()
            }
            appendLine("#$questionNumber  $timestamp")
            appendLine()
            appendLine("FRAGE:")
            appendLine(question)
            appendLine()
            appendLine("ANTWORT:")
            appendLine(answer)
        }

        sessionFile.appendText(entry)
    }
}
