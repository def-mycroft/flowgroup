package com.mfme.kernel.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class MinimalThemeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun tokensExposeExpectedPrimaryColor() {
        var color: Color? = null
        compose.setContent {
            KernelTheme(useDarkTheme = false) {
                color = KernelTheme.tokens.colors.primary
            }
        }
        assertEquals(Color(0xFF0061A4), color)
    }
}
