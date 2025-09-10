package com.mfme.kernel.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Basic color set for the kernel theme. */
data class ThemeColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color,
)

/** Spacing tokens used throughout the UI. */
data class ThemeSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
)

/** Corner radius tokens. */
data class ThemeRadius(
    val sm: Dp = 4.dp,
    val md: Dp = 8.dp,
    val lg: Dp = 12.dp,
)

/** Typography scale used by the kernel. */
data class ThemeTypeScale(
    val body: TextStyle,
    val title: TextStyle,
    val label: TextStyle,
)

/** Aggregated theme tokens. */
data class ThemeTokens(
    val colors: ThemeColors,
    val spacing: ThemeSpacing = ThemeSpacing(),
    val radius: ThemeRadius = ThemeRadius(),
    val typeScale: ThemeTypeScale,
)
