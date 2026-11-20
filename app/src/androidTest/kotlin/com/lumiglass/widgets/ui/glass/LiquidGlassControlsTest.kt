package com.lumiglass.widgets.ui.glass

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiquidGlassControlsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun toggleCanBeTurnedOnAndOffWithRepeatedTouches() {
        var checked by mutableStateOf(false)
        composeRule.setContent {
            MaterialTheme {
                Row(modifier = Modifier.clickable { checked = !checked }) {
                    LiquidGlassToggle(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        modifier = Modifier.testTag("widget-toggle")
                    )
                }
            }
        }

        val toggle = composeRule.onNodeWithTag("widget-toggle")
        toggle.assertIsOff()
        toggle.performTouchInput { click() }
        composeRule.runOnIdle {
            assertTrue(checked)
        }
        toggle.assertIsOn()

        toggle.performTouchInput { click() }
        composeRule.runOnIdle {
            assertFalse(checked)
        }
        toggle.assertIsOff()

        toggle.performTouchInput { click() }
        composeRule.runOnIdle {
            assertTrue(checked)
        }
    }

    @Test
    fun disabledToggleDoesNotChangeValue() {
        var checked by mutableStateOf(false)
        composeRule.setContent {
            MaterialTheme {
                LiquidGlassToggle(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    enabled = false,
                    modifier = Modifier.testTag("disabled-toggle")
                )
            }
        }

        composeRule.onNodeWithTag("disabled-toggle").performTouchInput { click() }
        composeRule.runOnIdle {
            assertFalse(checked)
        }
    }

    @Test
    fun sliderRespondsToTrackTapAndHorizontalDrag() {
        var value by mutableFloatStateOf(0.2f)
        composeRule.setContent {
            MaterialTheme {
                LiquidGlassSlider(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.testTag("widget-slider")
                )
            }
        }

        val slider = composeRule.onNodeWithTag("widget-slider")
        slider.performTouchInput {
            click(Offset(width * 0.8f, height / 2f))
        }
        composeRule.runOnIdle {
            assertTrue("Track tap should move the value forward", value > 0.65f)
        }

        slider.performTouchInput {
            swipe(
                start = Offset(width * 0.8f, height / 2f),
                end = Offset(width * 0.2f, height / 2f),
                durationMillis = 500
            )
        }
        composeRule.runOnIdle {
            assertTrue("Dragging left should move the value back", value < 0.35f)
        }
    }

    @Test
    fun buttonUsesLatestClickHandlerAndExposesAccessibleAction() {
        var clickCount by mutableIntStateOf(0)
        var clickHandler by mutableStateOf<() -> Unit>({ clickCount += 1 })
        composeRule.setContent {
            MaterialTheme {
                LiquidGlassButton(
                    onClick = clickHandler,
                    modifier = Modifier.testTag("widget-button")
                ) {
                    Text("Action")
                }
            }
        }

        val button = composeRule.onNodeWithTag("widget-button")
        button.assertHasClickAction()
        button.performClick()
        composeRule.runOnIdle {
            assertEquals(1, clickCount)
            clickHandler = { clickCount += 10 }
        }

        button.performTouchInput { click() }
        composeRule.runOnIdle {
            assertEquals(11, clickCount)
        }
    }
}
