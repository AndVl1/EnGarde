# TEAM STATE

## Classification
- Type: REFACTOR (stack modernization)
- Complexity: COMPLEX (full migration to Compose + Decompose)
- Workflow: FULL 7-PHASE
- Confidence: HIGH

## Task
Modernize EnGarde Android app to current stack:
- Migrate from View-based UI to Jetpack Compose
- Add Decompose for navigation
- Update Kotlin from 1.3.72 to latest stable
- Update all dependencies to current versions
- Replace deprecated APIs (kotlin-android-extensions, etc.)
- Migrate Gradle files to Kotlin DSL
- Verify all functionality via manual QA
- Push to feature branch and create PR (no merge)

**User note**: "No questions, going to sleep" - proceed autonomously with best practices

## Progress
- [x] Phase 1: Discovery - COMPLETED
- [x] Phase 2: Exploration - COMPLETED
- [x] Phase 3: Questions - SKIPPED (user request)
- [x] Phase 4: Architecture - COMPLETED
- [x] Phase 5: Implementation - COMPLETED
- [x] Phase 6: Review + Manual QA - COMPLETED
- [x] Phase 6.5: Review Fixes - COMPLETED
- [ ] Phase 7: PR Creation - IN PROGRESS

## Current Project State
**Technology Stack (BEFORE):**
- Kotlin: 1.3.72 (released 2020)
- Android Gradle Plugin: 4.2.0-beta02
- Gradle: 6.x (inferred)
- compileSdk: 29
- minSdk: 21
- targetSdk: 29
- UI Framework: View-based (Fragments, Activities)
- Navigation: Manual fragment transactions
- DI: None detected
- Deprecated: kotlin-android-extensions

**Dependencies:**
- androidx.appcompat:appcompat:1.1.0
- androidx.core:core-ktx:1.3.0
- androidx.constraintlayout:constraintlayout:1.1.3
- material:1.3.0-alpha01
- Firebase Analytics + Crashlytics
- Third-party: flatui, snackbar

**Project Structure:**
- Single module app
- Activities: MainActivity, SettingsActivity, CardActivity
- Fragments: CardAlertFragment, SettingsFragment
- Data model: Fencer.kt

## Key Decisions
- Feature branch created: `refactor/modernize-stack-compose-decompose`
- Will proceed without user questions (autonomous mode)
- Target: Full modernization to Compose + Decompose

## Phase 2 Output
**App Purpose:** Fencing bout timer and scorekeeper for referees - manages timing, scoring, penalties with weapon-specific rules (Sabre vs Epee/Foil)

**Architecture Findings:**
- UI: 3 Activities, 2 Fragments, RelativeLayout-based (legacy)
- Navigation: Intent-based (no Navigation Component)
- State: All in MainActivity (1008 lines), 30+ mutable fields
- Timer: CountDownTimer with 10ms precision, complex state machine
- Undo: ArrayDeque-based action history

**Critical Dependencies to Update:**
- Kotlin: 1.3.72 → 2.1.0
- AGP: 4.2.0-beta02 → 8.7.3
- Gradle: 6.7.1 → 8.12
- jcenter() → mavenCentral() (CRITICAL - jcenter shutdown)
- kotlin-android-extensions (DEPRECATED - removed in Kotlin 1.8+)
- compileSdk: 29 → 35
- targetSdk: 29 → 35 (Google Play requirement)
- All AndroidX libraries 3-4 years old

**Platform-Specific Features (need expect/actual if going KMP):**
- Vibration (Vibrator service)
- Notifications (NotificationManager)
- Ringtone (RingtoneManager)
- Wake lock (WindowManager flags)
- ToneGenerator (system beeps)

**Migration Strategy Options:**
1. **Android-only Compose migration** - Stay single-platform, migrate to Compose + Decompose
2. **KMP migration** - Extract logic to common code, Compose UI, multi-platform ready
3. **Gradual update** - Just update dependencies first, keep View system

## Files Identified
**Build/Config (Priority 1):**
- /build.gradle - root build (jcenter, old AGP, Kotlin 1.3.72)
- /app/build.gradle - app module (kotlin-android-extensions, old deps)
- /gradle/wrapper/gradle-wrapper.properties - Gradle 6.7.1
- /gradle.properties - settings
- /settings.gradle - module includes

**Core Logic (Priority 2):**
- /app/src/main/java/com/andvl1/engrade/MainActivity.kt - All bout logic (1008 lines)
- /app/src/main/java/com/andvl1/engrade/Fencer.kt - Data model
- /app/src/main/java/com/andvl1/engrade/CardActivity.kt - Card display
- /app/src/main/java/com/andvl1/engrade/CardAlertFragment.kt - Card selection dialog
- /app/src/main/java/com/andvl1/engrade/SettingsActivity.kt - Settings host
- /app/src/main/java/com/andvl1/engrade/SettingsFragment.kt - Preferences

**Resources (Priority 3):**
- /app/src/main/res/layout-normal/main_activity.xml - Main UI
- /app/src/main/res/xml/preferences.xml - Settings schema
- /app/src/main/AndroidManifest.xml - Activities, permissions

## Phase 4 Output - Architecture Approaches

**Three approaches designed by parallel architect agents:**

### APPROACH 1: Minimal Changes (Architect Agent)
- **Strategy**: Keep Groovy build files, migrate UI to Compose gradually
- **Build**: Groovy DSL (minimal changes), update dependencies
- **UI**: Full Compose rewrite (simpler than hybrid for this small app)
- **Architecture**: Decompose components, extract logic from MainActivity
- **Files**: ~30 new files, delete 8 old files
- **Pros**: Focused on meeting requirements without over-engineering
- **Cons**: Still a significant rewrite

### APPROACH 2: Clean Architecture (Architect Agent)
- **Strategy**: Full layered architecture (Domain/Platform/Component/UI)
- **Build**: Kotlin DSL + version catalog
- **UI**: Complete Compose + Material3
- **Architecture**: MVVM-like with Decompose, Koin DI, separate domain layer
- **Files**: ~36 new files (full separation)
- **Pros**: Maximum testability, clean separation, maintainable
- **Cons**: Most complex, highest effort

### APPROACH 3: Pragmatic Balance (Architect Agent) **RECOMMENDED**
- **Strategy**: Modern stack without over-engineering
- **Build**: Kotlin DSL + version catalog (standard practice)
- **UI**: Full Compose rewrite (small app = complete migration)
- **Architecture**: Decompose components + extracted domain logic (BoutEngine)
- **DI**: Manual construction (no framework for 3-4 dependencies)
- **Settings**: DataStore Preferences
- **Timer**: Coroutine-based (replaces CountDownTimer)
- **Files**: ~42 new files, delete old View code
- **Pros**: Modern, testable, balanced effort, future-ready
- **Cons**: Full rewrite (but app is small enough)

**Version Targets:**
- Kotlin 2.1.0
- AGP 8.7.3
- Gradle 8.11+
- Compose BOM 2024.12.01
- Decompose 3.2.2
- compileSdk/targetSdk 35
- minSdk 24 (up from 21 for Compose compatibility)

## Chosen Approach

**APPROACH 3: Pragmatic Balance** (autonomous decision based on user request)

**Rationale:**
- User requested "update all dependencies and migrate to Compose + Decompose"
- App is small (5 screens, 1 main logic file) → full migration is cleaner than hybrid
- Kotlin DSL + version catalog is current best practice
- Extracting BoutEngine (pure logic) enables unit testing
- No DI framework needed for this size
- DataStore over SharedPreferences (modern standard)
- Decompose requested explicitly by user

**Key Architecture Decisions:**
1. Single-module Android app (no KMP - not needed)
2. Decompose ChildStack navigation (RootComponent → Bout/Settings/CardOverlay)
3. BoutEngine: Pure Kotlin class with all scoring/timer logic (testable)
4. Platform services (sound, vibration, notifications) isolated from logic
5. Coroutine-based timer (replaces CountDownTimer)
6. Manual DI (construct dependencies in Activity)
7. Full Compose UI rewrite (all XML layouts deleted)

## Recovery
Currently finished Phase 4. Next: Phase 5 Implementation with developer-mobile agent.
