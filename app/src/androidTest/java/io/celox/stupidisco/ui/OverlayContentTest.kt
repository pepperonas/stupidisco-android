package io.celox.stupidisco.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.celox.stupidisco.model.AppState
import io.celox.stupidisco.model.AppStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(state: AppState) {
        composeRule.setContent {
            StupidiscoTheme {
                OverlayContent(
                    state = state,
                    onMicClick = {},
                    onCopy = {},
                    onRegenerate = {}
                )
            }
        }
    }

    @Test
    fun showsReadyStatus() {
        setContent(AppState())
        composeRule.onNodeWithText("ready").assertIsDisplayed()
        composeRule.onNodeWithText("stupidisco").assertIsDisplayed()
    }

    @Test
    fun showsRecordingStatus() {
        setContent(AppState(status = AppStatus.Recording))
        composeRule.onNodeWithText("recording").assertIsDisplayed()
    }

    @Test
    fun showsThinkingStatus() {
        setContent(AppState(status = AppStatus.Thinking))
        composeRule.onNodeWithText("thinking").assertIsDisplayed()
    }

    @Test
    fun showsErrorStatus() {
        setContent(AppState(status = AppStatus.Error("Network error")))
        composeRule.onNodeWithText("error").assertIsDisplayed()
        composeRule.onNodeWithText("Network error").assertIsDisplayed()
    }

    @Test
    fun micButton_isVisible() {
        setContent(AppState())
        composeRule.onNodeWithContentDescription("Start recording").assertIsDisplayed()
    }

    @Test
    fun micButton_showsStopRecording_whenRecording() {
        setContent(AppState(status = AppStatus.Recording))
        composeRule.onNodeWithContentDescription("Stop recording").assertIsDisplayed()
    }

    @Test
    fun micButton_isDisabled_whenThinking() {
        setContent(AppState(status = AppStatus.Thinking))
        composeRule.onNodeWithContentDescription("Start recording").assertIsNotEnabled()
    }

    @Test
    fun micButton_isEnabled_whenReady() {
        setContent(AppState())
        composeRule.onNodeWithContentDescription("Start recording").assertIsEnabled()
    }

    @Test
    fun showsTranscript_whenNotBlank() {
        setContent(AppState(transcript = "Was ist Kotlin?"))
        composeRule.onNodeWithText("TRANSCRIPT").assertIsDisplayed()
        composeRule.onNodeWithText("Was ist Kotlin?", substring = true).assertIsDisplayed()
    }

    @Test
    fun hidesTranscript_whenBlank() {
        setContent(AppState(transcript = "", partialTranscript = ""))
        composeRule.onNodeWithText("TRANSCRIPT").assertDoesNotExist()
    }

    @Test
    fun showsAnswer_whenPresent() {
        setContent(AppState(answer = "Kotlin ist eine Sprache.", status = AppStatus.Ready))
        composeRule.onNodeWithText("ANSWER").assertIsDisplayed()
        // MarkdownText uses AndroidView internally, so text isn't in Compose test tree
        // Verify answer section is visible via ANSWER header + action buttons
        composeRule.onNodeWithContentDescription("Copy").assertIsDisplayed()
    }

    @Test
    fun showsThinkingIndicator() {
        setContent(AppState(status = AppStatus.Thinking, answer = ""))
        composeRule.onNodeWithText("Thinking...").assertIsDisplayed()
    }

    @Test
    fun showsQuestionCount_whenGreaterThanZero() {
        setContent(AppState(questionCount = 3))
        composeRule.onNodeWithText("Q: 3").assertIsDisplayed()
    }

    @Test
    fun hidesQuestionCount_whenZero() {
        setContent(AppState(questionCount = 0))
        composeRule.onNodeWithText("Q: 0").assertDoesNotExist()
    }

    @Test
    fun showsVersionInfo() {
        setContent(AppState())
        composeRule.onNodeWithText("v1.3.0 | celox.io").assertIsDisplayed()
    }

    @Test
    fun showsCopyButton_whenAnswerPresent() {
        setContent(AppState(answer = "Answer text", status = AppStatus.Ready))
        composeRule.onNodeWithContentDescription("Copy").assertIsDisplayed()
    }

    @Test
    fun showsRegenerateButton_whenAnswerPresent() {
        setContent(AppState(answer = "Answer text", status = AppStatus.Ready))
        composeRule.onNodeWithContentDescription("Regenerate").assertIsDisplayed()
    }
}
