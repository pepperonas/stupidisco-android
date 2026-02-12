package io.celox.stupidisco.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppColors {
    val Background = Color(0xFF18181C)
    val Surface = Color(0xFF1E1E24)
    val Border = Color(0xFF333338)
    val TextPrimary = Color(0xFFE0E0E0)
    val TextSecondary = Color(0xFF8E8E93)
    val Answer = Color(0xFF34C759)
    val Recording = Color(0xFFFF3B30)
    val Thinking = Color(0xFFFF9500)
    val Ready = Color(0xFF34C759)
    val AccentBlue = Color(0xFF007AFF)
    val MicButton = Color(0xFF2C2C30)
    val MicButtonRecording = Color(0xFF3A1515)
}

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.AccentBlue,
    secondary = AppColors.Answer,
    background = AppColors.Background,
    surface = AppColors.Surface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AppColors.TextPrimary,
    onSurface = AppColors.TextPrimary
)

private val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = AppColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = AppColors.TextPrimary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = AppColors.TextSecondary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = AppColors.TextPrimary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        color = AppColors.TextSecondary
    )
)

@Composable
fun StupidiscoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
