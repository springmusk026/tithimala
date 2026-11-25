# Installed Claude Code skills

All project skills live under `.claude/skills/` and are pinned to audited upstream commit SHAs. No upstream installer script or web script was executed during vendoring.

## Inventory

| Skill name | Upstream repository | Source path | Pinned commit SHA | License | Invocation | Notes and modifications |
| --- | --- | --- | --- | --- | --- | --- |
| `edge-to-edge` | https://github.com/android/skills | `system/edge-to-edge` | `7f7b21730a605d112ca9f79b6e40105ce7b670ee` | Apache-2.0 (`LICENSES/APACHE-2.0-android-skills.txt`) | Auto | Unmodified. |
| `navigation-3` | https://github.com/android/skills | `navigation/navigation-3` | `7f7b21730a605d112ca9f79b6e40105ce7b670ee` | Apache-2.0 (`LICENSES/APACHE-2.0-android-skills.txt`) | Auto | Unmodified. |
| `agp-9-upgrade` | https://github.com/android/skills | `build-system/agp/agp-9-upgrade` | `7f7b21730a605d112ca9f79b6e40105ce7b670ee` | Apache-2.0 (`LICENSES/APACHE-2.0-android-skills.txt`) | Auto | Unmodified. |
| `r8-analyzer` | https://github.com/android/skills | `performance/r8-analyzer` | `7f7b21730a605d112ca9f79b6e40105ce7b670ee` | Apache-2.0 (`LICENSES/APACHE-2.0-android-skills.txt`) | Auto | Unmodified. Includes `scripts/analyze.py`, `scripts/convert_pb_to_json.py`, and `scripts/keep_radius_pb2.py`. |
| `glance-appwidget-updates-and-memory` | https://github.com/android/skills | `system/glance-appwidget-updates-and-memory` | `7f7b21730a605d112ca9f79b6e40105ce7b670ee` | Apache-2.0 (`LICENSES/APACHE-2.0-android-skills.txt`) | Auto | Unmodified. |
| `testing-setup` | https://github.com/android/skills | `testing/testing-setup` | `7f7b21730a605d112ca9f79b6e40105ce7b670ee` | Apache-2.0 (`LICENSES/APACHE-2.0-android-skills.txt`) | Auto | Unmodified. |
| `compose-state-and-effects` | https://github.com/chrisbanes/skills | `skills/compose-state-and-effects` | `f1132f9b04edd4eb985703f8fd757aa3acbda0ed` | Apache-2.0 (`LICENSES/APACHE-2.0-chrisbanes-skills.txt`) | Auto | Unmodified. |
| `compose-performance` | https://github.com/chrisbanes/skills | `skills/compose-performance` | `f1132f9b04edd4eb985703f8fd757aa3acbda0ed` | Apache-2.0 (`LICENSES/APACHE-2.0-chrisbanes-skills.txt`) | Auto | Unmodified. References uninstalled `gradle-run`; use `./gradlew` directly per `CLAUDE.md`. |
| `compose-component-design` | https://github.com/chrisbanes/skills | `skills/compose-component-design` | `f1132f9b04edd4eb985703f8fd757aa3acbda0ed` | Apache-2.0 (`LICENSES/APACHE-2.0-chrisbanes-skills.txt`) | Auto | Unmodified. |
| `compose-animations` | https://github.com/chrisbanes/skills | `skills/compose-animations` | `f1132f9b04edd4eb985703f8fd757aa3acbda0ed` | Apache-2.0 (`LICENSES/APACHE-2.0-chrisbanes-skills.txt`) | Auto | Unmodified. |
| `compose-ui-testing-patterns` | https://github.com/chrisbanes/skills | `skills/compose-ui-testing-patterns` | `f1132f9b04edd4eb985703f8fd757aa3acbda0ed` | Apache-2.0 (`LICENSES/APACHE-2.0-chrisbanes-skills.txt`) | Auto | Unmodified. |
| `jetpack-compose-audit` | https://github.com/hamen/compose_skill | `skills/jetpack-compose-audit` | `f815c31d6cc1a4af4ce2796d08eb7d8b506785c9` | MIT (`LICENSES/MIT-hamen-compose-skill.txt`) | Auto | Includes `SKILL.md`, `references/`, and `scripts/compose-reports.init.gradle`. Omitted `evals/`, `.claude-plugin/`, and `.cursor-plugin/`. |
| `android-gradle-logic` | https://github.com/rcosteira79/android-skills | `plugins/android-skills/skills/android-gradle-logic` | `fa39ae26f224fac45b32280f66ac839ae57b4c64` | MIT (`LICENSES/MIT-rcosteira79-android-skills.txt`) | Auto | Unmodified. |
| `modularization` | https://github.com/rcosteira79/android-skills | `plugins/android-skills/skills/modularization` | `fa39ae26f224fac45b32280f66ac839ae57b4c64` | MIT (`LICENSES/MIT-rcosteira79-android-skills.txt`) | Auto | Unmodified. |
| `android-data-layer` | https://github.com/rcosteira79/android-skills | `plugins/android-skills/skills/android-data-layer` | `fa39ae26f224fac45b32280f66ac839ae57b4c64` | MIT (`LICENSES/MIT-rcosteira79-android-skills.txt`) | Auto | Unmodified. |
| `datastore` | https://github.com/rcosteira79/android-skills | `plugins/android-skills/skills/datastore` | `fa39ae26f224fac45b32280f66ac839ae57b4c64` | MIT (`LICENSES/MIT-rcosteira79-android-skills.txt`) | Auto | Unmodified. |
| `kotlin-coroutines` | https://github.com/rcosteira79/android-skills | `plugins/android-skills/skills/kotlin-coroutines` | `fa39ae26f224fac45b32280f66ac839ae57b4c64` | MIT (`LICENSES/MIT-rcosteira79-android-skills.txt`) | Auto | Unmodified. |
| `kotlin-flows` | https://github.com/rcosteira79/android-skills | `plugins/android-skills/skills/kotlin-flows` | `fa39ae26f224fac45b32280f66ac839ae57b4c64` | MIT (`LICENSES/MIT-rcosteira79-android-skills.txt`) | Auto | Unmodified. |
| `gradle-build-performance` | https://github.com/rcosteira79/android-skills | `plugins/android-skills/skills/gradle-build-performance` | `fa39ae26f224fac45b32280f66ac839ae57b4c64` | MIT (`LICENSES/MIT-rcosteira79-android-skills.txt`) | Auto | Unmodified. |
| `ui-unslop` | https://github.com/heydamir/unslop | `skills/unslop` | `620163eedb3529d9cd23a3994ed8d287d06f760d` | CC-BY-4.0 (`ui-unslop/LICENSE`) | Auto | Renamed frontmatter `name` from `unslop` to `ui-unslop` and added CC-BY-4.0 modification notice crediting Damir Sokolovsky (`@heydamir`). Omitted web-only `scripts/` (`snapshot.mjs`, `story.mjs`, `story-path.mjs`), `agents/openai.yaml`, and `.webp` files over 200 KB under `references/refs/` (`beacon-engraving.webp` 569 KB, `meadow-3d.webp` 287 KB, `unslop-hero-after.webp` 224 KB). Kept smaller references `story-dashboard-frames.webp` (187 KB) and `unslop-dashboard-after.webp` (75 KB). |
| `unslop` | https://github.com/cursor/plugins | `pstack/skills/unslop/SKILL.md` | `41e400932eca14f289e7db717a70455256b6d388` | MIT (`unslop/LICENSE`) | Manual (`disable-model-invocation: true`) | Unmodified. Invoke explicitly as `/unslop` on prose only. |
| `frontend-design` | https://github.com/anthropics/skills | `skills/frontend-design` | `e4bb5d960d87126ff3b3a8af1a104b7db67560ba` | Apache-2.0 (`frontend-design/LICENSE.txt`) | Auto | Unmodified. |

## Omitted upstream skills

Only skills directly needed for Tithimala are installed. Overlapping or out-of-scope skills from the upstream repositories were excluded:

- `chrisbanes/skills`: omitted `gradle-run` (Tithimala runs `./gradlew` directly) and `compose-focus-navigation` (TV/keyboard focus navigation is out of scope for v0.1).
- `rcosteira79/android-skills`: omitted Compose, navigation, and testing skills that overlap with `android/skills` and `chrisbanes/skills`.
- `android/skills`: omitted Wear OS, XR, Media3, camera, and Play billing skills outside Tithimala's scope.

## License files

Shared upstream licenses are stored in `.claude/skills/LICENSES/`:

- `.claude/skills/LICENSES/APACHE-2.0-android-skills.txt`
- `.claude/skills/LICENSES/APACHE-2.0-chrisbanes-skills.txt`
- `.claude/skills/LICENSES/MIT-hamen-compose-skill.txt`
- `.claude/skills/LICENSES/MIT-rcosteira79-android-skills.txt`

Single-skill licenses sit inside their skill directories:

- `.claude/skills/ui-unslop/LICENSE` (CC-BY-4.0)
- `.claude/skills/unslop/LICENSE` (MIT)
- `.claude/skills/frontend-design/LICENSE.txt` (Apache-2.0)
