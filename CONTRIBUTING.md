# Contributing to EnGarde

## Dev setup

**Requirements:**
- Android Studio Ladybug (2024.2) or newer
- JDK 17 (temurin or zulu)
- Android SDK with API 24–36

**Clone and open:**

```bash
git clone https://github.com/<you>/EnGarde.git
cd EnGarde
# Copy dummy Firebase config (real google-services.json is git-ignored)
cp .github/google-services-dummy.json app/google-services.json
```

Open the project root in Android Studio. The IDE will sync Gradle automatically.

**First build:**

```bash
./gradlew assembleDebug
```

## Branch naming

| Change type | Pattern | Example |
|---|---|---|
| New feature | `feat/<descriptive-name>` | `feat/de-tableau-bracket` |
| Bug fix | `fix/<description>` | `fix/timer-resumes-on-rotation` |
| Refactor | `refactor/<scope>` | `refactor/pool-engine-cleanup` |

Target branch for PRs: `master`.

## Before submitting a PR

Run the full local check suite:

```bash
./gradlew assembleDebug        # must compile
./gradlew testDebugUnitTest    # must pass
./gradlew detekt               # must exit 0 (new issues fail the build)
./gradlew lintDebug            # must exit 0 (abortOnError = true)
```

The CI workflow (`.github/workflows/build.yml`) runs all four of these on every PR. Make them green locally first.

## Static analysis (detekt)

detekt 1.23.8 is configured with a baseline (`detekt-baseline.xml`). The baseline absorbs the pre-existing findings in the codebase; `./gradlew detekt` only reports **new** issues.

- Config file: `detekt.yml` (project root)
- Baseline file: `detekt-baseline.xml` (project root)
- The baseline must NOT be regenerated to silence new issues — fix the code instead
- Exception: if a deliberate large refactor absorbs a whole class of findings, update the baseline with a clear commit message explaining why

If detekt finds a false positive that genuinely cannot be fixed, suppress it inline and document why:

```kotlin
@Suppress("MagicNumber") // FIE rule: maximum score is always 15 for standard bouts
private const val MAX_SCORE = 15
```

## Code coverage (kover)

Coverage is measured for unit tests via kover 0.9.8. There is no minimum threshold gate — the report is informational only. To generate locally:

```bash
./gradlew koverXmlReportDebug    # app/build/reports/kover/reportDebug.xml
./gradlew koverHtmlReportDebug   # app/build/reports/kover/htmlDebug/
```

Domain and data layers (`domain/`, `data/`) are the primary targets. UI layers are excluded from coverage expectations.

## UI test tag convention

Every Compose screen that has UI tests must expose test tags following this pattern:

```
screenName_elementType_identifier
```

| Segment | Examples |
|---|---|
| `screenName` | `home`, `bout`, `groupSetup`, `groupDashboard`, `settings` |
| `elementType` | `button`, `text`, `input`, `icon`, `card`, `row` |
| `identifier` | `singleBout`, `leftScore`, `name_0`, `startTimer` |

Full examples: `home_button_singleBout`, `bout_text_leftScore`, `groupSetup_input_name_0`

Apply with `Modifier.testTag("bout_text_leftScore")` in the composable.

When adding a new screen:
1. Tag every interactive element and all text nodes that tests will assert
2. Add a Page Object in `app/src/androidTest/kotlin/.../page/`
3. Cover the screen with at least a smoke test in the corresponding test class

## i18n (string resources)

All user-visible strings must be added to **both** locale files:

- `app/src/main/res/values/strings.xml` — default (Russian)
- `app/src/main/res/values-en/strings.xml` — English

Never hardcode a string visible to the user. If you add a string in one file, add it in the other in the same PR.

## Room database migrations

EnGarde uses Room with schema export. Schema JSON files are stored in `app/schemas/` and are part of the repository.

Rules for schema changes:
1. Increment `version` in `EnGardeDatabase`
2. Provide a `Migration(oldVersion, newVersion)` object
3. Register the migration in the `databaseBuilder` call
4. The schema JSON (`app/schemas/<version>.json`) is auto-generated; commit it
5. **Never** use `fallbackToDestructiveMigration()` on production builds
6. Write a `MigrationTestHelper` test for every migration before merging

## Architecture notes

- **Navigation:** Decompose component stack (`RootComponent` → child components)
- **State:** each component owns a `State` data class; UI is stateless beyond observing it
- **Database:** Room DAOs accessed only via repository classes in `data/`
- **Domain:** `domain/` classes are pure Kotlin — no Android imports allowed
- **Platform-specific:** Android APIs live in `platform/` only; domain and data layers must not import them

## Dependency version ceilings

Do not bump past these without checking compatibility:

| Dependency | Ceiling |
|---|---|
| Kotlin | 2.2.21 (KSP for 2.3 not yet stable) |
| AGP | 8.13.2 (AGP 9 requires built-in-Kotlin migration) |
| Gradle wrapper | 8.14.5 |
| compileSdk | 36 |

Dependabot will open PRs for updates. Merge them only after verifying compatibility; do not auto-merge major bumps.
