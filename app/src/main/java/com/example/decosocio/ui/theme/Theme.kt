package com.example.decosocio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Red close to the DECO PROteste brand. Swap these values for the official brand tokens.
private val LightColors = lightColorScheme(
    primary = Color(0xFFC8102E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410004),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA6),
    onTertiaryContainer = Color(0xFF261900),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF231918),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF231918),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFD8C2BF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0EE),
    surfaceContainer = Color(0xFFFCEAE8),
    surfaceContainerHigh = Color(0xFFF6E4E2),
    surfaceContainerHighest = Color(0xFFF1DEDC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB3AC),
    onPrimary = Color(0xFF680010),
    primaryContainer = Color(0xFF930019),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFFF5BF48),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDEA6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1111),
    onBackground = Color(0xFFF1DEDC),
    surface = Color(0xFF1A1111),
    onSurface = Color(0xFFF1DEDC),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFFA08C8A),
    outlineVariant = Color(0xFF534341),
    surfaceContainerLowest = Color(0xFF140C0B),
    surfaceContainerLow = Color(0xFF231918),
    surfaceContainer = Color(0xFF271D1C),
    surfaceContainerHigh = Color(0xFF322827),
    surfaceContainerHighest = Color(0xFF3D3231),
)

/** Status colours; always paired with a text label, never used as the only signal. */
@Immutable
data class StatusColors(
    val positive: Color,
    val onPositive: Color,
    val warning: Color,
    val onWarning: Color,
    val negative: Color,
    val onNegative: Color,
    val neutral: Color,
    val onNeutral: Color,
)

private val LightStatus = StatusColors(
    positive = Color(0xFFC8F0CF), onPositive = Color(0xFF0B3D17),
    warning = Color(0xFFFFE08A), onWarning = Color(0xFF3F2E00),
    negative = Color(0xFFFFDAD6), onNegative = Color(0xFF5C0010),
    neutral = Color(0xFFE7E0DF), onNeutral = Color(0xFF3A302F),
)

private val DarkStatus = StatusColors(
    positive = Color(0xFF1F5130), onPositive = Color(0xFFC8F0CF),
    warning = Color(0xFF5C4300), onWarning = Color(0xFFFFE08A),
    negative = Color(0xFF7A1020), onNegative = Color(0xFFFFDAD6),
    neutral = Color(0xFF3D3231), onNeutral = Color(0xFFE7E0DF),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

@Composable
fun DecoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography(),
            content = content,
        )
    }
}
