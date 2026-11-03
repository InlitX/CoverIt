package com.coverit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coverit.app.R
import com.coverit.app.data.ThemeMode
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

private val LightColors = lightColorScheme(
    primary = Color(0xFF0D0D0D),
    onPrimary = Color(0xFFF5F5F3),
    background = Color(0xFFF4F4F2),
    onBackground = Color(0xFF0D0D0D),
    surface = Color.White,
    onSurface = Color(0xFF0D0D0D),
    surfaceVariant = Color(0xFFEAEAE7),
    onSurfaceVariant = Color(0xFF74746F),
    outline = Color(0xFF9A9A95),
    error = Color(0xFFD93F45),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2F2F2),
    onPrimary = Color(0xFF0D0D0D),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF161616),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFF8C8C8C),
    outline = Color(0xFF6E6E6E),
    error = Color(0xFFFF6369),
)

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private fun style(weight: FontWeight, size: Int, line: Int, spacing: Float = 0f) =
    TextStyle(fontFamily = Poppins, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

private val AppTypography = Typography(
    displayLarge = style(FontWeight.SemiBold, 40, 46, -0.6f),
    displaySmall = style(FontWeight.SemiBold, 30, 36, -0.4f),
    headlineMedium = style(FontWeight.SemiBold, 24, 30, -0.2f),
    titleLarge = style(FontWeight.Medium, 19, 25),
    titleMedium = style(FontWeight.Medium, 15, 21),
    bodyLarge = style(FontWeight.Normal, 15, 22),
    bodyMedium = style(FontWeight.Normal, 14, 20),
    bodySmall = style(FontWeight.Normal, 12, 17),
    labelLarge = style(FontWeight.Medium, 15, 20),
    labelMedium = style(FontWeight.Medium, 13, 18),
    labelSmall = style(FontWeight.Medium, 11, 14, 1.2f),
)

val LocalIsDark = staticCompositionLocalOf { false }

val borderColor: Color
    @Composable @ReadOnlyComposable get() =
        if (LocalIsDark.current) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.07f)

object Glass {

    val Bar: HazeStyle
        @Composable @ReadOnlyComposable get() {
            val dark = LocalIsDark.current
            val base = MaterialTheme.colorScheme.surface
            return HazeStyle(
                backgroundColor = MaterialTheme.colorScheme.background,
                tints = listOf(HazeTint(base.copy(alpha = if (dark) 0.62f else 0.72f))),
                blurRadius = 28.dp,
                noiseFactor = 0f,
                fallbackTint = HazeTint(base.copy(alpha = 0.97f)),
            )
        }
}

@Composable
fun CoverItTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalIsDark provides dark) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography, content = content)
    }
}
