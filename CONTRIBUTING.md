# Contributing to Trail Pack

Welcome! Trail Pack is a small, original mobile architecture teaching project. Contributions that make its behavior more dependable or easier to understand are welcome: focused fixes, tests, accessibility findings, documentation, and design discussions.

Try the [browser companion](https://ramysaleeb.github.io/android-native-architecture-lab/) and read the [walkthrough and architecture](README.md) first. The Kotlin Android app and HTML/JavaScript companion are separate implementations with separate local data.

## Pick a starting point

| Contribution | Starting issue | Experience |
| --- | --- | --- |
| Extract English UI strings into resources | [#1](https://github.com/RamySaleeb/android-native-architecture-lab/issues/1) | Good first issue for someone comfortable with Compose |
| Check TalkBack, keyboard, and large-text behavior | [#2](https://github.com/RamySaleeb/android-native-architecture-lab/issues/2) | Android UI/testing |
| Automate the browser walkthrough and focus behavior | [#3](https://github.com/RamySaleeb/android-native-architecture-lab/issues/3) | Browser testing |
| Run device tests in GitHub Actions | [#4](https://github.com/RamySaleeb/android-native-architecture-lab/issues/4) | Android tooling/CI |
| Design multiple independent trip lists | [#5](https://github.com/RamySaleeb/android-native-architecture-lab/issues/5) | Architecture discussion; design document first |

These are open directions, not release commitments. Check the live issue for current scope and activity. Comment with your intended approach before substantial work to avoid duplication. A small typo or isolated fix can go straight to a pull request. Discuss new dependencies, storage changes, and larger features before implementation; a draft PR is welcome.

No Android environment? Documentation, browser contributions, and reproducible usability reports are useful too.

## Set up a contribution

1. Fork the repository into your own account and clone your fork.
2. Create a descriptive branch from `main`, such as `fix/filter-focus`.
3. Keep the change focused on one issue or behavior.

```sh
git clone https://github.com/YOUR-USERNAME/android-native-architecture-lab.git
cd android-native-architecture-lab
git switch -c fix/filter-focus
```

### Native Android

Use Android Studio supporting AGP 9.1, JDK 17+, SDK 36, and an Android 8.0+ device/emulator (API 26+). Open the repository root, let Gradle sync, and run `app`. Use the checked-in wrapper (`sh gradlew`), not a globally installed Gradle. Set `ANDROID_HOME` or a local, uncommitted `local.properties` to your SDK location. Install required SDK components through Android Studio and review any license prompts yourself.

```sh
sh gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
# With a device/emulator connected and visible to adb:
sh gradlew :app:connectedDebugAndroidTest
```

The APK is at `app/build/outputs/apk/debug/app-debug.apk`. If setup fails, include the first relevant error, JDK/Studio versions, and SDK/API information in your report. Unit-test reports are under `app/build/reports/tests/`; lint and device reports are under `app/build/reports/`.

### Browser companion

Use Node.js 22 (the version used by Pages CI) for tests and Python 3 or another static HTTP server for preview. There is no npm install or backend setup for the current implementation.

```sh
node --test web/model.test.mjs
python3 -m http.server 8770 --directory web
# Open http://localhost:8770
```

Serve the files over HTTP rather than opening `index.html` directly. Preview storage is scoped to the browser origin and is separate from the deployed demo. Use a separate browser profile for destructive storage experiments. The app intentionally does not silently reset unreadable data.

## Find the right boundary

| Area | Files |
| --- | --- |
| Native rendering and semantics | [MainActivity.kt](app/src/main/java/com/ramysaleeb/trailpack/MainActivity.kt) |
| Native state, filtering, and recovery | [PackViewModel.kt](app/src/main/java/com/ramysaleeb/trailpack/PackViewModel.kt) |
| Native durable packed IDs | [PackRepository.kt](app/src/main/java/com/ramysaleeb/trailpack/PackRepository.kt) |
| Native tests | [Unit tests](app/src/test/java/com/ramysaleeb/trailpack/PackViewModelTest.kt), [device tests](app/src/androidTest/java/com/ramysaleeb/trailpack/) |
| Browser state and storage | [model.mjs](web/model.mjs), [model tests](web/model.test.mjs) |
| Browser controls and layout | [app.mjs](web/app.mjs), [index.html](web/index.html), [style.css](web/style.css) |

Preserve the sample's useful constraints: saved state is authoritative; failures remain visible and recoverable; coroutine cancellation stays cancellation; stored IDs remain stable. DataStore provides atomic native updates. Browser localStorage does not provide equivalent cross-tab transactions, so do not claim those guarantees for the web companion. Prefer simple boundaries over adding a framework or module solely for demonstration.

## Verify the behavior you changed

| Change | Evidence to include |
| --- | --- |
| Native logic | Unit tests, lint, debug build; regression case for changed behavior |
| Native UI or persistence | Native checks plus device tests; device/API and relevant manual steps |
| Browser model | Node tests covering success, failure, and recovery where applicable |
| Browser UI | Node tests plus manual checklist below; browser/version and screenshots |
| Documentation only | Check commands against current configuration and verify paths/links; no app rebuild required |

Browser checklist: pack/unpack; All/Remaining/Packed and empty states; reload persistence; simulated failure then successful retry; Tab/Shift+Tab and Space activation; focus after a filtered row disappears; narrow layout at 320px and enlarged text. Report what you actually checked and any limitation.

Current CI runs native unit tests, lint, and APK assembly. The Pages workflow runs browser model tests for web changes and deploys only after successful checks on `main`; PRs do not deploy. Native device tests and browser interaction checks are currently local/manual work, with automation tracked above. Do not present unrun checks as passed.

## Report a useful issue

Search [existing issues](https://github.com/RamySaleeb/android-native-architecture-lab/issues) first. Include:

- Native Android or browser companion, and the commit/demo URL tested.
- Steps to reproduce, expected behavior, and actual behavior.
- Device/API or browser/OS, viewport/font scale, and input method when relevant.
- Small relevant logs or screenshots, with credentials and personal data removed.
- For a proposal: the user problem, smallest useful scope, tradeoffs, and verification idea.

Use only fictional sample data. Never post private or startup source, signing keys, credentials, or personal information in issues or PRs.

## Submit a focused pull request

Target `main` and link the issue. Explain the concrete problem, resulting behavior, and any tradeoff. Include exact check commands and results, screenshots for UI changes, and migration/recovery evidence for storage changes. State any check you could not run and why; this helps review and is preferable to omitting it.

Keep generated builds, `local.properties`, signing material, and unrelated formatting out of the diff. Update the walkthrough if behavior changes. Respond respectfully to review and keep discussion focused on observable behavior. There is no guaranteed response time; issues are not exclusive reservations.

Contribute only original or appropriately licensed work. Contributions are made under the repository's [MIT license](LICENSE). No employer or client source belongs in this public sample.
