package io.celox.stupidisco.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionLogger(context: Context) {
    private val sessionDir = File(context.filesDir, "sessions").also { it.mkdirs() }
    private val sessionFile: File
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        sessionFile = File(sessionDir, "${dateFormat.format(Date())}.txt")
    }

    fun logQA(questionNumber: Int, question: String, answer: String) {
        val timestamp = timeFormat.format(Date())
        val separator = "=".repeat(60)

        val entry = buildString {
            appendLine()
            appendLine(separator)
            appendLine("#$questionNumber  $timestamp")
            appendLine(separator)
            appendLine("FRAGE:")
            appendLine(question)
            appendLine()
            appendLine("ANTWORT:")
            append(answer)
            appendLine()
        }

        sessionFile.appendText(entry)
    }
}
