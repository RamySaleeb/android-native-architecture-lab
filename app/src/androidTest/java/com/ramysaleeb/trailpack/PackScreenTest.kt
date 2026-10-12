package com.ramysaleeb.trailpack

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PackScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun checkboxIsNamedAndTogglesThroughOneMergedTarget() {
        var action: Pair<String, Boolean>? = null
        compose.setContent {
            TrailPackTheme {
                PackScreen(PackState(loading = false), { id, packed -> action = id to packed }, {}, {}, {})
            }
        }
        compose.onNode(hasText("Water bottle") and isToggleable()).assertIsOff().performClick()
        assertEquals("bottle" to true, action)
    }

    @Test fun filteringShowsEmptyStateAndKeepsProgress() {
        compose.setContent {
            TrailPackTheme {
                PackScreen(PackState(loading = false, filter = PackFilter.Packed), { _, _ -> }, {}, {}, {})
            }
        }
        compose.onNodeWithText("Nothing packed yet. Start with one small thing.").assertIsDisplayed()
        compose.onNodeWithText("0 of 6 packed").assertIsDisplayed()
    }

    @Test fun readFailureShowsRecoveryAndDisablesChecklist() {
        var retries = 0
        compose.setContent {
            TrailPackTheme {
                PackScreen(PackState(loading = false, readFailed = true, error = "Read failed"),
                    { _, _ -> }, {}, {}, { retries++ })
            }
        }
        compose.onNodeWithText("Retry reading").performClick()
        assertEquals(1, retries)
        compose.onNode(hasText("Water bottle") and isToggleable()).assertIsNotEnabled()
    }
}
