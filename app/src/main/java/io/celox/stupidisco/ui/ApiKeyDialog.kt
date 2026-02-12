package io.celox.stupidisco.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ApiKeyDialog(
    initialDeepgramKey: String = "",
    initialAnthropicKey: String = "",
    onSave: (deepgramKey: String, anthropicKey: String) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var deepgramKey by remember { mutableStateOf(initialDeepgramKey) }
    var anthropicKey by remember { mutableStateOf(initialAnthropicKey) }
    val context = LocalContext.current

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AppColors.AccentBlue,
        unfocusedBorderColor = AppColors.Border,
        focusedTextColor = AppColors.TextPrimary,
        unfocusedTextColor = AppColors.TextPrimary,
        cursorColor = AppColors.AccentBlue,
        focusedLabelColor = AppColors.AccentBlue,
        unfocusedLabelColor = AppColors.TextSecondary
    )

    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        containerColor = AppColors.Surface,
        title = {
            Text(
                "API Keys",
                color = AppColors.TextPrimary,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = deepgramKey,
                    onValueChange = { deepgramKey = it },
                    label = { Text("Deepgram API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                LinkText(
                    text = "console.deepgram.com",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://console.deepgram.com"))
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = anthropicKey,
                    onValueChange = { anthropicKey = it },
                    label = { Text("Anthropic API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                LinkText(
                    text = "console.anthropic.com",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://console.anthropic.com"))
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(deepgramKey.trim(), anthropicKey.trim()) },
                enabled = deepgramKey.isNotBlank() && anthropicKey.isNotBlank()
            ) {
                Text("Save", color = if (deepgramKey.isNotBlank() && anthropicKey.isNotBlank()) AppColors.AccentBlue else AppColors.TextSecondary)
            }
        },
        dismissButton = {
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AppColors.TextSecondary)
                }
            }
        }
    )
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    ClickableText(
        text = AnnotatedString(
            text = text,
            spanStyle = SpanStyle(
                color = AppColors.AccentBlue,
                fontSize = 12.sp,
                textDecoration = TextDecoration.Underline
            )
        ),
        onClick = { onClick() },
        modifier = Modifier.padding(top = 4.dp)
    )
}
