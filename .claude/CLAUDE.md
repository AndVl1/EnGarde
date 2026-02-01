# EnGarde - Fencing Scoring App

## Project Overview

Android fencing scoring application for tracking bouts and group stage (pool) tournaments following FIE rules. Single-activity Compose app with Decompose navigation.

**Tech stack:** Kotlin 2.1.21, Jetpack Compose (BOM 2025.05.00), Material3, Decompose 3.2.0, Room 2.7.1, Firebase (Analytics + Crashlytics), KSP

**Min SDK:** 24 | **Target SDK:** 35

## Architecture

- **Navigation:** Decompose component-based stack (`RootComponent` → child components)
- **State management:** MVI-like with `State` data classes and component logic
- **Database:** Room with DAOs for pool/bout/fencer persistence
- **i18n:** Russian (default) + English (`values/strings.xml`, `values-en/strings.xml`)

## Source Structure

```
app/src/main/kotlin/com/andvl1/engrade/
├── data/           # Room DB, DAOs, PoolRepository
├── domain/         # FieBoutOrder (Berger tables for round-robin)
├── platform/       # SoundManager, PDF export
├── ui/
│   ├── bout/       # BoutScreen, BoutComponent, BoutState (single bout scoring)
│   ├── common/     # Shared UI components
│   ├── group/      # Group stage: setup/, dashboard/, confirm/, result/, boutslist/
│   ├── home/       # HomeScreen (entry point)
│   ├── root/       # RootComponent, RootContent (navigation host)
│   ├── settings/   # SettingsScreen (weapon, mode, toggles)
│   └── theme/      # App theme
└── EnGardeActivity.kt
```

## Key Screens

- **Home** — mode selection (single bout, group stage, continue pool)
- **Settings** — weapon (sabre/foil+epee), mode (5/15), display toggles
- **Bout** — scoring screen with timer, cards (yellow/red), undo, sections
- **Group Setup** — fencer count (5-8), names, org/region fields
- **Group Dashboard** — result matrix, rankings, bout progression
- **Bout Confirm** — pre-bout screen with swap sides
- **Bout Result** — post-bout score display

## UI Tests

### Framework
- **Ultron 2.6.2** (ultron-compose + ultron-android + ultron-allure)
- **PageObject pattern** with `Page<T>` base class
- **Allure reporting** with steps, epics, features, auto-screenshots on failure

### Test Runner
`com.atiurin.ultron.allure.UltronAllureTestRunner` (set in `app/build.gradle.kts`)

### Test Structure
```
app/src/androidTest/kotlin/com/andvl1/engrade/
├── base/BaseTest.kt          # UltronComposeRule + config setup
├── page/                     # 8 Page Objects (HomePage, BoutPage, etc.)
├── test/                     # 6 screen test classes (28 tests)
└── e2e/                      # 2 E2E test classes (5 tests)
```

**Total: 33 tests, all passing**

### TestTag Convention
All production screens use testTag modifiers: `screenName_elementType_identifier`
- Example: `home_button_singleBout`, `bout_text_leftScore`, `groupSetup_input_name_0`

### Allure Configuration
- `UltronAllureConfig.applyRecommended()` in `BaseTest`
- Results written to `/sdcard/Download/allure-results/` (survives app uninstall)
- Annotations: `@Epic("...")`, `@Feature("...")` on test classes
- `step("description") { ... }` wrappers in test methods
- CI generates single-file report (`allure generate --single-file`)

### Important Patterns
- **Semantic merging:** Material3 `Surface(onClick=...)` merges descendant semantics. Use `withUseUnmergedTree(true)` to access inner Text nodes (scores, names)
- **Scrolling:** Use `.scrollTo()` (not `performScrollTo()`) for off-screen elements
- **Async loading:** Dashboard loads from Room DB — use `withTimeout(15000)` for assertions
- **API compatibility:** Avoid `List.removeLast()` — only works on API 35+, use `removeAt(size - 1)`

### Running Tests
```bash
# Local (requires connected emulator/device)
./gradlew :app:connectedDebugAndroidTest

# Generate Allure report after test run
adb pull /sdcard/Download/allure-results/ allure-results/
allure generate allure-results/allure-results -o allure-report --single-file --clean
open allure-report/index.html
```

### CI
GitHub Actions workflow at `.github/workflows/ui-tests.yml`:
- Triggers on PRs to master
- API 30 emulator with KVM acceleration
- Publishes JUnit results as PR check annotation
- Uploads single-file Allure report as artifact

## Build Commands
```bash
./gradlew assembleDebug                    # Build debug APK
./gradlew assembleDebugAndroidTest         # Build test APK
./gradlew connectedDebugAndroidTest        # Run all instrumented tests
```

## Dependencies (versions in gradle/libs.versions.toml)
Key versions: Kotlin 2.1.21, Compose BOM 2025.05.00, Room 2.7.1, Decompose 3.2.0, Ultron 2.6.2, KSP 2.1.21-2.0.1
