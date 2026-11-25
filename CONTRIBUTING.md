# Contributing to Tithimala

Thank you for your interest in contributing to Tithimala. This guide covers local setup, architecture boundaries, git workflow, data corrections, and localization rules.

## Local development setup

Install the following tools before building the project:

- JDK 21 (or the Android Studio bundled JBR 21).
- Android SDK 37 (`compileSdk = 37`, `targetSdk = 37`, `minSdk = 26`).
- Android Studio (latest stable release) or command-line SDK tools.

Run the Gradle wrapper from the repository root to verify a change:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew :core:calendar-engine:test
./gradlew ktlintCheck lintDebug
./gradlew verifyRoborazziDebug
```

Some modules and tasks appear as milestones land. Check the root `settings.gradle.kts` for the active module list.

## Architecture overview

Tithimala separates calendar math, data storage, design tokens, and UI features into focused Gradle modules:

- `:core:calendar-engine` is a pure-JVM Kotlin module with zero Android dependencies. It handles Bikram Sambat to Gregorian conversion, month lengths, date arithmetic, and Devanagari numeral formatting.
- `:core:*` modules hold shared infrastructure such as calendar data storage, preferences, design system components, and common models.
- `:feature:*` modules implement individual screens and widgets (month grid, day detail, date converter, reminders, settings, and Glance widgets). Feature modules depend on `:core:*` modules and never on each other.

Tithimala targets reproducible builds on F-Droid. Do not add Google Play Services, Firebase, crash reporters, ad SDKs, or analytics libraries. Keep every dependency open source and verifiable.

## Git workflow

1. Fork the repository or create a branch from `main`.
2. Name branches using `<type>/<issue-number>-<short-slug>`, such as `feat/12-month-grid`, `fix/18-leap-day`, or `chore/2-github-community-health`.
3. Write commit messages in [Conventional Commits](https://www.conventionalcommits.org/) format, such as `feat(engine): add BS to AD converter` or `fix(data): correct Dashain date in 2082`.
4. Keep pull requests focused on one change and under 400 lines where practical.
5. Fill out every applicable section of the pull request template, including light, dark, Nepali, and English screenshots for UI changes.

Pull requests are squash-merged into `main` after review and CI checks pass.

## Agent skills

Project Claude Code skills live under `.claude/skills/`. Every skill is permissively licensed, committed directly to the repository, and documented with upstream provenance in `.claude/skills/SOURCES.md`. Update `SOURCES.md` whenever you add or refresh a skill.

## Reporting calendar data errors

If you spot an incorrect BS month length, AD conversion, tithi, festival, holiday, or spelling, open a [Calendar data error](https://github.com/springmusk026/tithimala/issues/new?template=data_error.yml) issue. Include an official reference such as Nepal Panchanga Nirnayak Samiti, the Nepal Gazette (Rajpatra), or an official government notice.

Bundled calendar data draws from upstream community datasets including `sanamhub/nepdate`. When a bug originates upstream, note that in the issue so fixes stay traceable.

## Localization

Tithimala supports English (`values/strings.xml`) and Nepali (`values-ne/strings.xml`).

- Write natural, idiomatic Nepali in `values-ne/strings.xml` instead of literal word-for-word translations from English.
- Format Devanagari numerals through `:core:calendar-engine` helpers rather than hardcoding digits in UI strings.
- Test both languages and both numeral settings (Devanagari and Latin) when changing UI layouts so labels do not clip or wrap awkwardly.
