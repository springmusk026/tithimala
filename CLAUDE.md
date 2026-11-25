# Tithimala (तिथिमाला)

Tithimala is an open-source, ad-free Nepali (Bikram Sambat) calendar for Android.

## Project identity

- Application ID: `app.tithimala`
- License: GNU General Public License v3.0 (`LICENSE`)
- Privacy and distribution: offline-first, ad-free, and free of Google Play Services, Firebase, analytics, tracking, accounts, news, and horoscope feeds.
- F-Droid compatibility: keep builds deterministic and free of proprietary binary dependencies so F-Droid reproducible builds succeed.

## Tech stack

Use the pinned versions below across `gradle/libs.versions.toml` and `build-logic/`:

- Android Gradle Plugin: 9.5.2 (uses built-in Kotlin support; do not apply `org.jetbrains.kotlin.android` or `kapt`)
- Kotlin: 2.4.21
- KSP: 2.3.14
- SDK levels: `minSdk = 26`, `compileSdk = 37`, `targetSdk = 37`
- Jetpack Compose BOM: 2026.11.01
- Navigation 3: 1.2.2
- Hilt: 2.60.1
- Room: 2.8.5
- DataStore: 1.3.0
- Glance: 1.2.0
- WorkManager: 2.12.0
- Roborazzi: 1.79.0
- ktlint: 14.2.0

## Architecture and module rules

The repository uses `build-logic/` for Gradle convention plugins and 15 Gradle modules:

- `:app`
- `:core:model` (pure JVM)
- `:core:calendar-engine` (pure JVM)
- `:core:data`
- `:core:database`
- `:core:datastore`
- `:core:designsystem`
- `:core:ui`
- `:core:notifications`
- `:feature:calendar`
- `:feature:day`
- `:feature:converter`
- `:feature:reminders`
- `:feature:settings`
- `:widget`

Follow these module and code boundaries:

- Feature modules never depend on other feature modules. Route cross-feature navigation through `:app`.
- `:core:model` and `:core:calendar-engine` stay pure JVM modules with no Android framework dependencies.
- Use MVVM with unidirectional data flow. Each ViewModel exposes a single immutable `UiState` via `StateFlow`, handles actions sent from the UI, and emits one-off effects via `Channel(Channel.BUFFERED).receiveAsFlow()`.
- Keep visibility as narrow as possible. Default to `private`, widen to `internal` within a module, and use `public` only for intentional cross-module APIs.

## Calendar data rules

- Bundle versioned Bikram Sambat calendar JSON sourced from `sanamhub/nepdate` (MIT). Preserve the upstream `NOTICE` file.
- Validate bundled and downloaded JSON against the schema and verify SHA-256 checksums before accepting an update.
- Lock date conversion, month lengths, and Tithi calculations with golden test vectors in `:core:calendar-engine`.
- Allow one optional weekly WorkManager static JSON check using `HttpURLConnection`. Do not add third-party HTTP client libraries. Any network, parsing, schema, or SHA-256 failure falls back silently to bundled data. Provide a setting to disable background data checks completely.

## Design rules

Tithimala uses the Himalayan Dawn design system:

- Color: define dedicated light and dark color tokens in `:core:designsystem`. Build hierarchy with tonal surfaces and subtle hairlines instead of heavy shadows or glowing gradients.
- Typography: bundle OFL fonts Mukta (interface and body) and Noto Serif Devanagari (editorial headings and Devanagari numerals/titles).
- Spacing and shape: follow an 8dp spacing grid with 4dp micro steps. Use nested corner radii of 8dp, 12dp, and 16dp.
- Motion: use damped Compose spring animations and respect system reduced-motion preferences.
- Accessibility: enforce 48dp minimum touch targets, WCAG AA color contrast, TalkBack content descriptions and semantics, and full layout integrity at 200% font scale.
- Launch splash: provide an animated-vector splash wrapper under `drawable-v31`.
- Screen structure: split stateful `Route` composables (Hilt injection and ViewModel collection) from pure stateless `Screen` composables.
- Components: keep reusable UI components stateless, accept `modifier: Modifier = Modifier`, add `@Preview` coverage for light, dark, Nepali, English, and 200% font scale, and cover key screens and components with Roborazzi screenshot tests.

## Localization rules

- Store all user-facing strings in `res/values/strings.xml` (English) and `res/values-ne/strings.xml` (Nepali). Never hardcode user-visible strings in Kotlin code.
- Implement in-app language switching with the Android per-app language API (`AppCompatDelegate.setApplicationLocales` / `LocaleManager`).
- Format Devanagari numerals exclusively in `:core:calendar-engine`.

## Skills usage and conflict precedence

Project skills are vendored at pinned commits in `.claude/skills/` and cataloged in `.claude/skills/SOURCES.md`.

Installed skills:

- Android platform and build: `agp-9-upgrade`, `android-gradle-logic`, `gradle-build-performance`, `modularization`, `edge-to-edge`, `navigation-3`, `glance-appwidget-updates-and-memory`, `r8-analyzer`, `testing-setup`
- Data and Kotlin concurrency: `android-data-layer`, `datastore`, `kotlin-coroutines`, `kotlin-flows`
- Compose and UI craft: `compose-state-and-effects`, `compose-performance`, `compose-component-design`, `compose-animations`, `compose-ui-testing-patterns`, `jetpack-compose-audit`, `frontend-design`, `ui-unslop`
- Prose editing (manual only): `unslop` (`disable-model-invocation: true`)

Required skill checkpoints:

- Run `jetpack-compose-audit` and `ui-unslop` at the end of every UI milestone.
- Run `/unslop` manually on `README.md`, `strings.xml` user copy, store listing copy, `CHANGELOG.md`, and release notes. Never run `/unslop` on Kotlin source files, Gradle scripts, XML resource keys, or code comments.

Conflict precedence:

- The Tithimala project brief and `CLAUDE.md` take precedence over any conflicting rule in an installed skill.
- Spacing grid: Tithimala's 8dp grid (4dp micro) overrides web spacing tables in `ui-unslop`.
- Motion: damped Compose springs respecting reduced motion override CSS easing rules in `ui-unslop`.
- Theme and typography: Himalayan Dawn light and dark tokens and bundled Mukta plus Noto Serif Devanagari fonts override `ui-unslop` neutral ramps and web font tables.
- Build execution: invoke `./gradlew` directly instead of the uninstalled `gradle-run` skill referenced by `compose-performance`.

## Git and GitHub workflow

- Follow trunk-based development. `main` stays releasable at all times. Never push directly to `main`.
- Use short-lived issue branches named `feat/N-slug`, `fix/N-slug`, `chore/N-slug`, `docs/N-slug`, or `refactor/N-slug`.
- Write Conventional Commits and end commit messages with:

```text
Co-Authored-By: Claude Code <noreply@anthropic.com>
```

- Keep code pull requests small (target under 400 changed lines where practical).
- Squash-merge pull requests only after CI is green.
- Scan staged changes for secrets before pushing. Never commit `local.properties`, `*.keystore`, `*.jks`, `keystore.properties`, `signing.properties`, `.env`, or `.claude/settings.local.json`.

## Multi-agent discipline

- Keep the main agent focused on observation, coordination, and architectural guidance.
- Delegate independent implementation, exploration, audit, and review tasks to subagents in parallel when the work splits cleanly.

## Local build and device commands

Set Java and Android SDK paths before running Gradle or `adb` from a shell:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

Common commands:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew ktlintCheck
./gradlew ktlintFormat
./gradlew verifyRoborazziDebug
./gradlew recordRoborazziDebug
./gradlew installDebug
./gradlew assembleRelease
"$ANDROID_HOME/platform-tools/adb" devices
```
