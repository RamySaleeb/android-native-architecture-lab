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

The API 36.1 instrumented run above was local. CI now runs the full device suite on API 35 as described below, alongside unit tests, lint, and APK assembly.

## Reproduce automated checks

```sh
sh gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
sh gradlew :app:connectedDebugAndroidTest
```

Tests live in `PackScreenTest.kt`; persistence coverage remains in `PackPersistenceTest.kt`. The keyboard fixtures explicitly request keyboard input mode and inject key events. Semantics assertions verify one merged checkbox target, live-region properties, and separate retry actions. Layout assertions inspect text measurement and scroll to offscreen actions.

See [issue #2](https://github.com/RamySaleeb/android-native-architecture-lab/issues/2) for the remaining manual audit and verification record. Semantics follow the [Android accessibility guidance](https://developer.android.com/develop/ui/compose/accessibility/semantics); focus requests run in effects following [Compose focus guidance](https://developer.android.com/develop/ui/compose/touch-input/focus/request-focus).

## Emulator CI

The `Native accessibility and persistence (API 35)` job in [Android CI](https://github.com/RamySaleeb/android-native-architecture-lab/actions/workflows/android.yml) runs all instrumented tests, including keyboard, semantics, large-text, and DataStore coverage, on every pull request, every push to `main`, and manual dispatch. It has no path filter or dependency on the unit-test job, so a separate failure cannot silently skip device coverage.

Configuration: `ubuntu-24.04`, Temurin JDK 17, Android 15 / API 35, `system-images;android-35;google_apis;x86_64`, Pixel 2 profile, two emulator cores, KVM acceleration, software graphics, and a fresh AVD with snapshots disabled. Compile SDK stays 36. The OS label and Android package coordinate are fixed; hosted-runner patches, emulator binaries, and SDK package revisions can change. The artifact records the actual emulator version and image package metadata. This single configuration does not replace API 26–36, OEM, TalkBack listening, or physical keyboard testing.

The job is capped at 30 minutes: SDK setup at eight minutes, emulator boot at five minutes, and Gradle device testing at 12 minutes. Failure status survives log collection; no automatic test retry hides a failure. Exit/signal cleanup collects logcat and terminates the emulator; the hosted runner is disposable if forcibly cancelled. New commits cancel superseded runs for the same PR or branch.

On success or failure, the job uploads `trail-pack-device-api35-<attempt>` for 14 days: HTML reports, XML/raw instrumentation results, Gradle output, emulator output, logcat, device properties, and image/version metadata. A setup failure may produce only setup logs; hard cancellation or runner loss can prevent upload. Open the failed step first, then inspect the artifact.

Forked PRs use `pull_request`, read-only repository permissions, no secrets, and no persisted checkout credentials in the device job. First-time contributors can still require GitHub’s maintainer approval. Running checks does not itself configure branch protection or make them mandatory for merging.

SDK installation uses existing hosted-runner license records and closed stdin. It never runs `sdkmanager --licenses` or automatically agrees to new terms. Missing license acceptance fails setup; a maintainer must review the requirement. For local reproduction, install the same image through Android Studio after personally reviewing any license prompts, start a fresh API 35 emulator, then run:

```sh
sh gradlew --no-daemon :app:connectedDebugAndroidTest --stacktrace
```

Find local reports in `app/build/reports/androidTests/connected/` and `app/build/outputs/androidTest-results/connected/`. For hosted execution evidence and measured runtime, see [issue #4](https://github.com/RamySaleeb/android-native-architecture-lab/issues/4).
