package com.mfme.kernel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

private val lightTokens = ThemeTokens(
    colors = ThemeColors(
        primary = Color(0xFF0061A4),
        secondary = Color(0xFF005CB2),
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color.Black,
        onSurface = Color.Black
    ),
    typeScale = ThemeTypeScale(
        body = TextStyle(fontSize = 16.sp),
        title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
        label = TextStyle(fontSize = 14.sp)
    )
)

private val darkTokens = ThemeTokens(
    colors = ThemeColors(
        primary = Color(0xFF9CD3FF),
        secondary = Color(0xFF9CCBFF),
        background = Color(0xFF000000),
        surface = Color(0xFF1E1E1E),
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White
    ),
    typeScale = ThemeTypeScale(
        body = TextStyle(fontSize = 16.sp),
        title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
        label = TextStyle(fontSize = 14.sp)
    )
)

private val LocalThemeTokens = staticCompositionLocalOf { lightTokens }

@Composable
fun KernelTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val tokens = if (useDarkTheme) darkTokens else lightTokens
    val colorScheme = if (useDarkTheme) {
        darkColorScheme(
            primary = tokens.colors.primary,
            secondary = tokens.colors.secondary,
            background = tokens.colors.background,
            surface = tokens.colors.surface,
            onPrimary = tokens.colors.onPrimary,
            onSecondary = tokens.colors.onSecondary,
            onBackground = tokens.colors.onBackground,
            onSurface = tokens.colors.onSurface,
        )
    } else {
        lightColorScheme(
            primary = tokens.colors.primary,
            secondary = tokens.colors.secondary,
            background = tokens.colors.background,
            surface = tokens.colors.surface,
            onPrimary = tokens.colors.onPrimary,
            onSecondary = tokens.colors.onSecondary,
            onBackground = tokens.colors.onBackground,
            onSurface = tokens.colors.onSurface,
        )
    }
    val typography = Typography(
        bodyLarge = tokens.typeScale.body,
        titleMedium = tokens.typeScale.title,
        labelMedium = tokens.typeScale.label,
    )
    CompositionLocalProvider(LocalThemeTokens provides tokens) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
    }
}

object KernelTheme {
    val tokens: ThemeTokens
        @Composable get() = LocalThemeTokens.current
}
