package io.celox.stupidisco.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApiKeyDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTitle() {
        composeRule.setContent {
            StupidiscoTheme {
                ApiKeyDialog(onSave = { _, _ -> })
            }
        }
        composeRule.onNodeWithText("API Keys").assertIsDisplayed()
    }

    @Test
    fun showsInputFields() {
        composeRule.setContent {
            StupidiscoTheme {
                ApiKeyDialog(onSave = { _, _ -> })
            }
        }
        composeRule.onNodeWithText("Deepgram API Key").assertIsDisplayed()
        composeRule.onNodeWithText("Anthropic API Key").assertIsDisplayed()
    }

    @Test
    fun showsConsoleLinks() {
        composeRule.setContent {
            StupidiscoTheme {
                ApiKeyDialog(onSave = { _, _ -> })
            }
        }
        composeRule.onNodeWithText("console.deepgram.com").assertIsDisplayed()
        composeRule.onNodeWithText("console.anthropic.com").assertIsDisplayed()
    }

    @Test
    fun saveButton_isDisabled_whenBothFieldsEmpty() {
        composeRule.setContent {
            StupidiscoTheme {
                ApiKeyDialog(onSave = { _, _ -> })
            }
        }
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun saveButton_isDisabled_whenOnlyDeepgramFilled() {
        composeRule.setContent {
            StupidiscoTheme {
                ApiKeyDialog(onSave = { _, _ -> })
            }
        }
        composeRule.onNodeWithText("Deepgram API Key").performTextInput("some-key")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun saveButton_isDisabled_whenOnlyAnthropicFilled() {
        composeRule.setContent {
            StupidiscoTheme {
                ApiKeyDialog(onSave = { _, _ -> })
            }
        }
        composeRule.onNodeWithText("Anthropic API Key").performTextInput("some-key")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun saveButton_isEnabled_whenBothFieldsFilled() {
        composeRule.setContent {
            StupidiscoTheme {
                ApiKeyDialog(onSave = { _, _ -> })
            }
        }
        composeRule.onNodeWithText("Deepgram API Key").performTextInput("dg-key")
        composeRule.onNodeWithText("Anthropic API Key").performTextInput("ant-key")
        composeRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun showsCancelButton_whenOnDismissProvided() {
        composeRule.setContent {
            StupidiscoTheme {
                ApiKeyDialog(onSave = { _, _ -> }, onDismiss = {})
            }
        }
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun hidesCancelButton_whenOnDismissNull() {
        composeRule.setContent {
            StupidiscoTheme {
                ApiKeyDialog(onSave = { _, _ -> }, onDismiss = null)
            }
        }
        composeRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun saveButton_isEnabled_withInitialKeys() {
        composeRule.setContent {
            StupidiscoTheme {
                ApiKeyDialog(
                    initialDeepgramKey = "dg-initial",
                    initialAnthropicKey = "ant-initial",
                    onSave = { _, _ -> }
                )
            }
        }
        composeRule.onNodeWithText("Save").assertIsEnabled()
    }
}
