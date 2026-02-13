package io.celox.stupidisco.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import io.celox.stupidisco.R
import io.celox.stupidisco.model.AppState
import io.celox.stupidisco.model.AppStatus

@Composable
fun OverlayContent(
    state: AppState,
    answerAreaHeightDp: Int = 120,
    onMicClick: () -> Unit,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onClose: () -> Unit = {},
    onDrag: (Float, Float) -> Unit = { _, _ -> },
    onResize: (Float) -> Unit = {}
) {
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .width(340.dp)
            .clip(shape)
            .background(AppColors.Background)
            .border(1.dp, AppColors.Border, shape)
            .padding(12.dp)
    ) {
        // Title bar (drag handle + close)
        TitleBar(
            status = state.status,
            onClose = onClose,
            onDrag = onDrag
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Mic button
        MicButton(
            isRecording = state.status is AppStatus.Recording,
            onClick = onMicClick,
            enabled = state.status !is AppStatus.Thinking
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Transcript area
        if (state.transcript.isNotBlank() || state.partialTranscript.isNotBlank()) {
            TranscriptArea(
                finalTranscript = state.transcript,
                partialTranscript = state.partialTranscript
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Answer area
        if (state.answer.isNotBlank() || state.status is AppStatus.Thinking) {
            AnswerArea(
                answer = state.answer,
                isThinking = state.status is AppStatus.Thinking && state.answer.isBlank(),
                heightDp = answerAreaHeightDp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            if (state.answer.isNotBlank()) {
                ActionButtons(
                    onCopy = onCopy,
                    onRegenerate = onRegenerate,
                    enabled = state.status !is AppStatus.Thinking
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Error display
        if (state.status is AppStatus.Error) {
            Text(
                text = state.status.message,
                color = AppColors.Recording,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Footer
        Footer(questionCount = state.questionCount)

        // Resize handle
        ResizeHandle(onResize = onResize)
    }
}

@Composable
private fun TitleBar(
    status: AppStatus,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close button (red dot, macOS-style)
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(AppColors.Recording)
                .clickable { onClose() }
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Drag area (title + status)
        Row(
            modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "stupidisco",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AppColors.TextPrimary
            )

            val (statusText, statusColor) = when (status) {
                is AppStatus.Ready -> "ready" to AppColors.Ready
                is AppStatus.Recording -> "recording" to AppColors.Recording
                is AppStatus.Thinking -> "thinking" to AppColors.Thinking
                is AppStatus.Error -> "error" to AppColors.Recording
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = statusText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun MicButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val scale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isRecording) AppColors.MicButtonRecording else AppColors.MicButton,
        animationSpec = tween(300),
        label = "mic_bg"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isRecording) AppColors.Recording else AppColors.TextPrimary,
        animationSpec = tween(300),
        label = "mic_icon"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(64.dp)
                .scale(if (isRecording) scale else 1f)
                .clip(CircleShape)
                .background(bgColor)
                .border(
                    width = if (isRecording) 2.dp else 1.dp,
                    color = if (isRecording) AppColors.Recording else AppColors.Border,
                    shape = CircleShape
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_mic),
                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun TranscriptArea(finalTranscript: String, partialTranscript: String) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
            .padding(8.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "TRANSCRIPT",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = AppColors.TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = buildString {
                if (finalTranscript.isNotBlank()) append(finalTranscript)
                if (partialTranscript.isNotBlank()) {
                    if (isNotBlank()) append(" ")
                    append(partialTranscript)
                }
            },
            fontSize = 13.sp,
            color = AppColors.TextSecondary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun AnswerArea(answer: String, isThinking: Boolean, heightDp: Int) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
            .padding(8.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "ANSWER",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = AppColors.TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (isThinking) {
            Text(
                text = "Thinking...",
                fontSize = 13.sp,
                color = AppColors.Thinking,
                fontFamily = FontFamily.Monospace
            )
        } else {
            MarkdownText(
                markdown = answer,
                style = androidx.compose.ui.text.TextStyle(
                    color = AppColors.Answer,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
private fun ActionButtons(
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        IconButton(
            onClick = onCopy,
            enabled = enabled,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp))
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_copy),
                contentDescription = "Copy",
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onRegenerate,
            enabled = enabled,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp))
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_refresh),
                contentDescription = "Regenerate",
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun Footer(questionCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "v1.2.0 | celox.io",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = AppColors.TextSecondary
        )
        if (questionCount > 0) {
            Text(
                text = "Q: $questionCount",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ResizeHandle(onResize: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    onResize(dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AppColors.Border)
        )
    }
}
