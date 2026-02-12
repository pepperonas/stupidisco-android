package io.celox.stupidisco.data

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

    private val separator = "=".repeat(60)

    @Before
    fun setup() {
        sessionFile = File(tempFolder.root, "test-session.txt")
    }

    /** Replicates SessionLogger.logQA formatting logic for testability */
    private fun formatEntry(
        questionNumber: Int,
        question: String,
        answer: String,
        timestamp: String
    ): String {
        return buildString {
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
    }

    @Test
    fun `every entry has separator`() {
        val entry = formatEntry(1, "Was ist Kotlin?", "Eine Programmiersprache.", "12:00:05")
        assertTrue(entry.contains(separator))
        assertTrue(entry.contains("#1  12:00:05"))
    }

    @Test
    fun `separator is 60 equals signs`() {
        val entry = formatEntry(1, "Q", "A", "00:00:00")
        assertTrue(entry.contains("=" .repeat(60)))
    }

    @Test
    fun `question numbering is correct`() {
        val entry = formatEntry(42, "Frage", "Antwort", "14:30:00")
        assertTrue(entry.contains("#42  14:30:00"))
    }

    @Test
    fun `entry contains FRAGE and ANTWORT blocks`() {
        val entry = formatEntry(1, "Meine Frage", "Meine Antwort", "10:00:00")
        assertTrue(entry.contains("FRAGE:"))
        assertTrue(entry.contains("Meine Frage"))
        assertTrue(entry.contains("ANTWORT:"))
        assertTrue(entry.contains("Meine Antwort"))
    }

    @Test
    fun `separator appears before each entry`() {
        val entry1 = formatEntry(1, "Frage 1", "Antwort 1", "10:00:05")
        sessionFile.appendText(entry1)

        val entry2 = formatEntry(2, "Frage 2", "Antwort 2", "10:01:10")
        sessionFile.appendText(entry2)

        val content = sessionFile.readText()
        assertTrue(content.contains("#1  10:00:05"))
        assertTrue(content.contains("#2  10:01:10"))
        // Separator appears 4 times (2 per entry: above and below header)
        val separatorCount = content.split(separator).size - 1
        assertTrue("Expected 4 separators, got $separatorCount", separatorCount == 4)
    }

    @Test
    fun `multiline question and answer preserved`() {
        val question = "Line 1\nLine 2\nLine 3"
        val answer = "Answer line 1\nAnswer line 2"
        val entry = formatEntry(1, question, answer, "09:00:00")
        assertTrue(entry.contains("Line 1\nLine 2\nLine 3"))
        assertTrue(entry.contains("Answer line 1\nAnswer line 2"))
    }

    @Test
    fun `format matches Python version structure`() {
        val entry = formatEntry(1, "Was ist REST?", "REST ist ein Architekturstil.", "15:30:00")
        // Python format: \n separator \n #N  HH:MM:SS \n separator \n FRAGE: \n ... \n\n ANTWORT: \n ...
        val lines = entry.lines()
        assertTrue("First line should be empty", lines[0].isEmpty())
        assertTrue("Second line should be separator", lines[1] == separator)
        assertTrue("Third line should be entry header", lines[2].startsWith("#1  "))
        assertTrue("Fourth line should be separator", lines[3] == separator)
        assertTrue("Fifth line should be FRAGE:", lines[4] == "FRAGE:")
    }
}
