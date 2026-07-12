package com.journalog.app.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val InstagramPink = Color(0xFFE1306C)
val InstagramPurple = Color(0xFF833AB4)
val InstagramOrange = Color(0xFFF77737)
val InstagramYellow = Color(0xFFFCAF45)
val InstagramBlue = Color(0xFF405DE6)

val StoryGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFF58529),
        Color(0xFFDD2A7B),
        Color(0xFF8134AF)
    )
)

private val LightColors = lightColorScheme(
    primary = InstagramPink,
    onPrimary = Color.White,
    secondary = InstagramPurple,
    onSecondary = Color.White,
    tertiary = InstagramBlue,
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF636363),
    background = Color.White,
    onBackground = Color(0xFF1A1A1A),
    outline = Color(0xFFDBDBDB),
)

private val DarkColors = darkColorScheme(
    primary = InstagramPink,
    onPrimary = Color.White,
    secondary = InstagramPurple,
    onSecondary = Color.White,
    tertiary = InstagramBlue,
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFBDBDBD),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF0F0F0),
    outline = Color(0xFF3A3A3A),
)

val JournalogTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
    ),
)

val JournalogShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun JournalogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = JournalogTypography,
        shapes = JournalogShapes,
        content = content
    )
}
