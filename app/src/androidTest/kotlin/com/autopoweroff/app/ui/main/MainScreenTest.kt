package com.autopoweroff.app.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.autopoweroff.app.ui.main.MainScreen]. */
class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mainScreen_displaysCoreComponents() {
        composeTestRule.setContent {
            MainScreen()
        }

        composeTestRule.onNodeWithText("AutoPowerOff").assertIsDisplayed()
        composeTestRule.onNodeWithText("System Diagnostics & Permissions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Turn-Off Mechanism").assertIsDisplayed()
    }
}
