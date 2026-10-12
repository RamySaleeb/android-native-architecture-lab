# Native accessibility walkthrough

This guide covers the Kotlin Android app. Browser checks are separate. It records implementation and automated evidence, not a claim of accessibility conformance or a completed TalkBack listening audit.

## What changed

- One merged checkbox target per item, with the item name before category/detail. The decorative checkbox contributes no duplicate semantics.
- Filters have contextual labels and retain their selected state semantics. The filter group stays together during directional navigation.
- Keyboard-focused checklist rows have a visible green outline. Pending writes leave the focused row available while repeated activation is ignored.
- When a keyboard-activated row disappears from Remaining or Packed after a saved update, focus returns to the selected filter. Another keyboard action cancels that pending restoration.
- Loading, empty, failure-arm, and error messages expose polite live-region semantics. The read-retry button stays a separate action; keyboard activation returns focus to the selected filter before the retry control disappears.
- Six stable keyed rows live in a vertically scrolling column, so offscreen controls remain composed. Text wraps, filters wrap with vertical spacing, and controls have at least 48dp target height. The heading and outer margins leave more room for enlarged text.

The eager column is deliberate for this fixed six-item sample. Revisit virtualization and focus restoration if the data grows. Keyboard focus restoration does not programmatically move TalkBack accessibility focus. Read failures still disable editing; storage and ViewModel behavior are unchanged.

## Keyboard walkthrough

1. Connect a keyboard or enable keyboard input in an emulator. Use Tab and Shift+Tab to traverse filters, visible item rows, and the failure control; use Enter or Space to activate.
2. Select Remaining, focus Water bottle, and activate it. After it is saved, the row disappears and the selected Remaining filter receives focus. Tab continues from there.
3. Switch to Packed and unpack the final row. Confirm the empty-state message and a usable filter focus target.
4. Arm the failure control. Its label changes while the control stays focusable. Navigate to an item and activate it: the saved state must remain unchanged on failure. Activate again to recover.
5. For pending-write behavior, use the regression test's controlled state fixture; no arbitrary storage delay is injected into the real app.

## TalkBack listening pass — still required

On a device with TalkBack, record device model, API, TalkBack version, font/display settings, and input method. Do not mark this pass complete based on Compose tests alone.

- Swipe through the title, progress, filters, and each item. Check a meaningful order and a single checkbox announcement per item, including name and checked state.
- Check selected filter feedback and heading navigation.
- Double-tap an item and confirm saved progress feedback. Check that narration is useful without duplicate announcements.
- Arm failure, toggle an item, listen to the failure message, and retry. Verify focus stays usable when a filtered item disappears.
- Exercise a read-failure test fixture and verify that Retry reading is separately discoverable and actionable.

## Large text and small windows

Increase system font size to the largest available setting and test portrait and landscape. Check every item name/detail, filter, progress message, and failure/retry action for clipping and reachability. Record window dimensions and display scale. Automated constrained-layout tests complement this pass; they do not emulate every OEM font or Android's nonlinear system scaling.

## Verification record

On 12 October 2026, **7 JVM unit tests and 12 Android instrumented tests passed**, along with debug APK assembly and lint (zero errors; nine existing warnings). Device tests ran on the Android 16 / API 36.1 Medium Phone ARM64 emulator (1080×2400, 420dpi).

The 11 screen tests plus one persistence test cover keyboard activation, filtered removal in both directions, pending-save focus, read retry, merged semantics, and text layout. Layout fixtures constrain Compose to 320×640dp at 1× and 2× font scale and 320×240dp at 2×. They check line bounds, ellipsis, minimum target height, and scrolling to actions. These are controlled fixtures, not a manual system font-size or rotation test. Keyboard tests request keyboard input mode and inject keys; no physical keyboard or TalkBack listening pass was completed.

CI currently runs unit tests, lint, and APK assembly. Instrumented tests were run locally; emulator CI is tracked separately in issue #4.

## Reproduce automated checks

```sh
sh gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
sh gradlew :app:connectedDebugAndroidTest
```

Tests live in `PackScreenTest.kt`; persistence coverage remains in `PackPersistenceTest.kt`. The keyboard fixtures explicitly request keyboard input mode and inject key events. Semantics assertions verify one merged checkbox target, live-region properties, and separate retry actions. Layout assertions inspect text measurement and scroll to offscreen actions.

See [issue #2](https://github.com/RamySaleeb/android-native-architecture-lab/issues/2) for the remaining manual audit and verification record. Semantics follow the [Android accessibility guidance](https://developer.android.com/develop/ui/compose/accessibility/semantics); focus requests run in effects following [Compose focus guidance](https://developer.android.com/develop/ui/compose/touch-input/focus/request-focus).
