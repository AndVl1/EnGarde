# EnGarde

Android fencing scoring application for FIE-standard single bouts, group stage (pool) tournaments, and Direct Elimination (DE) brackets.

## What it does

- **Single bout** — score a match with timer, yellow/red/black cards, section tracking, and undo
- **Pool (group stage)** — round-robin pools for 5–8 fencers, automatic Berger bout order, result matrix, rankings, PDF/CSV export
- **Direct Elimination** — bracket generation from pool results, match progression to final
- Bilingual: Russian (default) and English UI

## Tech stack

| Layer | Library / Tool |
|---|---|
| Language | Kotlin 2.2.21 |
| UI | Jetpack Compose (BOM 2026.05.01), Material 3 |
| Navigation | Decompose 3.5.0 |
| Database | Room 2.8.4 |
| Analytics / Crash | Firebase (Analytics + Crashlytics) |
| Annotation processing | KSP 2.2.21-2.0.5 |
| Build | AGP 8.13.2, Gradle 8.14.5 |
| Min SDK | 24 | Target SDK 35 | Compile SDK 36 |

## Build commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build debug + test APKs (for instrumented runs)
./gradlew assembleDebug assembleDebugAndroidTest

# Run unit tests (JVM, no device needed)
./gradlew testDebugUnitTest

# Run instrumented UI tests (requires connected emulator/device)
./gradlew connectedDebugAndroidTest

# Static analysis (detekt — gates new issues via baseline)
./gradlew detekt

# Generate coverage report (HTML + XML, no threshold gate)
./gradlew koverXmlReportDebug    # XML  → app/build/reports/kover/reportDebug.xml
./gradlew koverHtmlReportDebug   # HTML → app/build/reports/kover/htmlDebug/

# Android lint
./gradlew lintDebug

# Clean build directory
./gradlew clean
```

## Source structure

```
app/src/main/kotlin/com/andvl1/engrade/
├── data/               # Room DB, DAOs, repositories
│   └── db/             # Database, entities, converters
├── domain/             # Pure business logic
│   ├── model/          # Domain models (Weapon, CardType, DeSlot, …)
│   ├── BoutEngine.kt   # Scoring state machine
│   ├── PoolEngine.kt   # Pool ranking + matrix
│   ├── DeTableau.kt    # DE bracket algorithm
│   └── FieBoutOrder.kt # Berger round-robin order
├── platform/           # Android-specific helpers (PDF, CSV, Sound, Notifications)
├── ui/
│   ├── bout/           # BoutScreen + BoutComponent (single-bout scoring)
│   ├── de/             # DE tableau screen
│   ├── group/          # Group setup, dashboard, confirm, result, bouts list
│   ├── home/           # Home screen (mode selection)
│   ├── root/           # RootComponent — navigation host
│   ├── settings/       # Settings screen (weapon, mode, toggles)
│   └── theme/          # App theme
└── EnGardeActivity.kt
```

## Testing

### Unit tests (JVM)

```bash
./gradlew testDebugUnitTest
```

Located in `app/src/test/java/`. Cover `BoutEngine`, `PoolEngine`, `DeTableau`, `FieBoutOrder`, and `PoolRepository`. No device or emulator required.

### Instrumented UI tests (Android emulator / device)

```bash
./gradlew connectedDebugAndroidTest
```

Located in `app/src/androidTest/`. Built with **Ultron 2.6.2** (Page Object pattern) and **Allure** reporting. Requires a connected emulator or physical device.

Generate Allure report after a run:

```bash
adb pull /sdcard/Download/allure-results/ allure-results/
allure generate allure-results/allure-results -o allure-report --single-file --clean
open allure-report/index.html
```

### Test tag convention

All production screens expose semantic test tags in the pattern:

```
screenName_elementType_identifier
```

Examples: `home_button_singleBout`, `bout_text_leftScore`, `groupSetup_input_name_0`

Tags are set via `Modifier.testTag(...)` in the Compose composable. Page Objects in tests reference them via `hasTestTag(...)`.

## CI

| Workflow | File | Trigger |
|---|---|---|
| Build, lint, unit tests, coverage | `.github/workflows/build.yml` | PR to master + push to master |
| Instrumented UI tests (emulator) | `.github/workflows/ui-tests.yml` | PR to master |

The build workflow runs `assembleDebug`, `testDebugUnitTest`, `detekt`, and `koverXmlReportDebug`. Coverage is uploaded as an artifact (no hard threshold gates the build).

## Dependency updates

Dependabot is configured (`.github/dependabot.yml`) and opens weekly PRs for Gradle and GitHub Actions dependencies. Kotlin, Compose, Firebase, and AndroidX updates are grouped per ecosystem.

Note: do not auto-merge Kotlin or AGP bumps — check the version ceilings documented in the project memory before accepting.

## License

See [LICENSE](LICENSE).
