package io.celox.stupidisco.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Tests SessionLogger log format without Android Context dependency.
 * Replicates the formatting logic from SessionLogger.logQA to verify output format.
 */
class SessionLoggerFormatTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var sessionFile: File

    @Before
    fun setup() {
        sessionFile = File(tempFolder.root, "test-session.txt")
    }

    /** Replicates SessionLogger.logQA formatting logic for testability */
    private fun formatEntry(
        questionNumber: Int,
        question: String,
        answer: String,
        elapsedSeconds: Long,
        fileExistsAndNotEmpty: Boolean
    ): String {
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        val timestamp = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

        return buildString {
            if (fileExistsAndNotEmpty) {
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
    }

    @Test
    fun `first entry has no separator`() {
        val entry = formatEntry(1, "Was ist Kotlin?", "Eine Programmiersprache.", 5, false)
        assertFalse(entry.contains("===================================="))
        assertTrue(entry.startsWith("#1  00:00:05"))
    }

    @Test
    fun `second entry has separator`() {
        val entry = formatEntry(2, "Und Java?", "Auch eine Sprache.", 65, true)
        assertTrue(entry.contains("===================================="))
        assertTrue(entry.contains("#2  00:01:05"))
    }

    @Test
    fun `question numbering is correct`() {
        val entry = formatEntry(42, "Frage", "Antwort", 0, false)
        assertTrue(entry.contains("#42  00:00:00"))
    }

    @Test
    fun `timestamp format for hours`() {
        val entry = formatEntry(1, "Q", "A", 3661, false) // 1h 1m 1s
        assertTrue(entry.contains("01:01:01"))
    }

    @Test
    fun `entry contains FRAGE and ANTWORT blocks`() {
        val entry = formatEntry(1, "Meine Frage", "Meine Antwort", 0, false)
        assertTrue(entry.contains("FRAGE:"))
        assertTrue(entry.contains("Meine Frage"))
        assertTrue(entry.contains("ANTWORT:"))
        assertTrue(entry.contains("Meine Antwort"))
    }

    @Test
    fun `full session with multiple entries`() {
        // Simulate writing two entries
        val entry1 = formatEntry(1, "Frage 1", "Antwort 1", 5, false)
        sessionFile.appendText(entry1)

        val entry2 = formatEntry(2, "Frage 2", "Antwort 2", 70, true)
        sessionFile.appendText(entry2)

        val content = sessionFile.readText()
        assertTrue(content.contains("#1  00:00:05"))
        assertTrue(content.contains("#2  00:01:10"))
        // Separator should appear exactly once (between entries)
        val separatorCount = content.split("====================================").size - 1
        assertTrue("Expected 1 separator, got $separatorCount", separatorCount == 1)
    }

    @Test
    fun `multiline question and answer preserved`() {
        val question = "Line 1\nLine 2\nLine 3"
        val answer = "Answer line 1\nAnswer line 2"
        val entry = formatEntry(1, question, answer, 0, false)
        assertTrue(entry.contains("Line 1\nLine 2\nLine 3"))
        assertTrue(entry.contains("Answer line 1\nAnswer line 2"))
    }
}
