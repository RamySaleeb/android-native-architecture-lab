package com.ramysaleeb.trailpack

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PackScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun checkboxIsNamedAndTogglesThroughOneMergedTarget() {
        var action: Pair<String, Boolean>? = null
        compose.setContent {
            TrailPackTheme {
                PackScreen(PackState(loading = false), { id, packed -> action = id to packed }, {}, {}, {})
            }
        }
        compose.onNode(hasText("Water bottle") and isToggleable()).performScrollTo().assertIsOff().performClick()
        assertEquals("bottle" to true, action)
    }

    @Test fun filteringShowsEmptyStateAndKeepsProgress() {
        compose.setContent {
            TrailPackTheme {
                PackScreen(PackState(loading = false, filter = PackFilter.Packed), { _, _ -> }, {}, {}, {})
            }
        }
        compose.onNodeWithText("Nothing packed yet. Start with one small thing.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("0 of 6 packed").performScrollTo().assertIsDisplayed()
    }

    @Test fun readFailureShowsRecoveryAndDisablesChecklist() {
        var retries = 0
        compose.setContent {
            TrailPackTheme {
                PackScreen(PackState(loading = false, readFailed = true, error = "Read failed"),
                    { _, _ -> }, {}, {}, { retries++ })
            }
        }
        compose.onNodeWithText("Retry reading").performScrollTo().performClick()
        assertEquals(1, retries)
        compose.onNode(hasText("Water bottle") and isToggleable()).assertIsNotEnabled()
    }

    @Test fun keyboardActivationAndFilteredRemovalKeepAUsefulFocusTarget() {
        var state by mutableStateOf(PackState(loading = false, filter = PackFilter.Remaining))
        lateinit var inputMode: InputModeManager
        compose.setContent {
            TrailPackTheme {
                inputMode = LocalInputModeManager.current
                PackScreen(state, { id, packed ->
                    state = state.copy(packedIds = if (packed) state.packedIds + id else state.packedIds - id)
                }, { state = state.copy(filter = it) }, {}, {})
            }
        }
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        val bottle = compose.onNodeWithTag("gear-bottle")
        bottle.performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus)
        bottle.assertIsFocused().performKeyInput { pressKey(Key.Spacebar) }
        bottle.assertDoesNotExist()
        compose.onNodeWithContentDescription("Remaining items filter").assertIsFocused()
        compose.onNodeWithContentDescription("Remaining items filter").performKeyInput { pressKey(Key.Tab) }
        compose.onNodeWithContentDescription("Packed items filter").assertIsFocused()
        compose.onNodeWithContentDescription("Packed items filter").performKeyInput {
            keyDown(Key.ShiftLeft); pressKey(Key.Tab); keyUp(Key.ShiftLeft)
        }
        compose.onNodeWithContentDescription("Remaining items filter").assertIsFocused()
    }

    @Test fun pendingSaveKeepsFocusAndIgnoresRepeatedActivation() {
        var state by mutableStateOf(PackState(loading = false))
        var writes = 0
        lateinit var inputMode: InputModeManager
        compose.setContent {
            TrailPackTheme {
                inputMode = LocalInputModeManager.current
                PackScreen(state, { _, _ -> writes++; state = state.copy(saving = true) }, {}, {}, {})
            }
        }
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        val bottle = compose.onNodeWithTag("gear-bottle")
        bottle.performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus)
        bottle.performKeyInput { pressKey(Key.Enter) }
        bottle.assertIsFocused().assertIsOff()
        bottle.performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(1, writes)
        compose.runOnIdle { state = state.copy(saving = false, error = "Save failed. Try again.") }
        bottle.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(2, writes)
    }

    @Test fun announcementsAndCheckboxSemanticsRemainSeparateFromRetryAction() {
        compose.setContent {
            TrailPackTheme {
                PackScreen(PackState(loading = false, readFailed = true, error = "Read failed"),
                    { _, _ -> }, {}, {}, {})
            }
        }
        compose.onAllNodes(hasText("Water bottle") and isToggleable()).assertCountEquals(1)
        compose.onNodeWithText("Read failed").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.onNodeWithText("Retry reading").assertHasClickAction()
        compose.onNodeWithText("0 of 6 packed").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test fun largeTextInNarrowShortViewportWrapsAndAllActionsRemainReachable() = verifyLayout(240, 2f)

    @Test fun largeTextInNarrowPortraitWrapsAndAllActionsRemainReachable() = verifyLayout(640, 2f)

    @Test fun defaultTextInNarrowPortraitWrapsAndAllActionsRemainReachable() = verifyLayout(640, 1f)

    private fun verifyLayout(height: Int, fontScale: Float) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Box(Modifier.requiredSize(320.dp, height.dp)) {
                    TrailPackTheme {
                        PackScreen(PackState(loading = false), { _, _ -> }, {}, {}, {})
                    }
                }
            }
        }
        for (filter in PackFilter.entries) {
            compose.onNodeWithContentDescription("${filter.name} items filter")
                .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
        for (gear in sampleGear) {
            compose.onNodeWithTag("gear-${gear.id}").performScrollTo().assertHasClickAction()
            for (text in listOf(gear.name, gear.detail)) {
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(text, useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTextFits(layouts)
            }
        }
        compose.onNodeWithText("Simulate next save failure").performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Simulate next save failure", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTextFits(layouts)
    }
    @Test fun unpackingLastVisibleItemRestoresPackedFilterFocus() {
        var state by mutableStateOf(PackState(loading = false, filter = PackFilter.Packed, packedIds = setOf("bottle")))
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode = LocalInputModeManager.current
            TrailPackTheme {
                PackScreen(state, { _, _ -> state = state.copy(packedIds = emptySet()) }, {}, {}, {})
            }
        }
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithTag("gear-bottle").performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag("gear-bottle").performKeyInput { pressKey(Key.Spacebar) }
        compose.onNodeWithContentDescription("Packed items filter").assertIsFocused()
        compose.onNodeWithTag("gear-bottle").assertDoesNotExist()
    }

    @Test fun keyboardReadRetryReturnsFocusToAvailableFilter() {
        var state by mutableStateOf(PackState(loading = false, readFailed = true, error = "Read failed"))
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode = LocalInputModeManager.current
            TrailPackTheme {
                PackScreen(state, { _, _ -> }, {}, {}, { state = PackState(loading = false) })
            }
        }
        compose.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        compose.onNodeWithText("Retry reading").performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Retry reading").performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithContentDescription("All items filter").assertIsFocused()
        compose.onNodeWithText("Retry reading").assertDoesNotExist()
    }

    private fun assertTextFits(layouts: List<TextLayoutResult>) {
        assertTrue("Text measurement must be available", layouts.isNotEmpty())
        for (layout in layouts) {
            // Paragraph allocation can exceed a Text node's natural width. Check actual
            // line bounds, with one pixel for rounding, rather than allocated paragraph width.
            for (line in 0 until layout.lineCount) {
                val message = "${layout.layoutInput.text.text}: line $line in ${layout.size}"
                assertFalse(message, layout.isLineEllipsized(line))
                assertTrue(message, layout.getLineLeft(line) >= -1f)
                assertTrue(message, layout.getLineRight(line) <= layout.size.width + 1f)
                assertTrue(message, layout.getLineBottom(line) <= layout.size.height + 1f)
            }
        }
    }
}
