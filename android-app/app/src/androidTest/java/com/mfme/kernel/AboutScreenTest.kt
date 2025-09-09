package com.mfme.kernel

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mfme.kernel.ui.KernelActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutScreenTest {
    @get:Rule val composeRule = createAndroidComposeRule<KernelActivity>()

    @Test fun loadsKernelBrief() {
        composeRule.onNodeWithText("About").performClick()
        composeRule.onNodeWithText("Kernel Brief").assertExists()
    }
}
