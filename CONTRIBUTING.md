# Contributing to Trail Pack

Thanks for helping make this small Android sample easier to learn from.

## Start small

Open an issue describing the user-visible problem, a reproducible example, and a proposed scope. For a straightforward fix, a focused pull request is welcome. Discuss new dependencies, architecture changes, or new features before implementation.

Useful contributions:

- Test large text, TalkBack, switch access, and keyboard navigation; document device/API and steps.
- Move UI text to Android string resources, then add a reviewed translation.
- Propose a multiple-trip model with stable IDs, migration behavior, and persistence tests.

## Development

Follow the toolchain and run instructions in README.md. Keep `local.properties`, generated builds, signing keys, and credentials out of commits. Use only original or properly licensed content; never submit employer or client source.

Run `sh gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` before submitting. For UI or storage changes also run `sh gradlew :app:connectedDebugAndroidTest` on an emulator. Explain any check you could not run.

## Pull requests

Describe the problem and resulting behavior. Include relevant test evidence and screenshots for UI changes. Keep storage authoritative, preserve coroutine cancellation, and test recovery as well as success. Avoid broad formatting churn.

Contributions are under the repository's MIT license. Be respectful and specific in discussions.
