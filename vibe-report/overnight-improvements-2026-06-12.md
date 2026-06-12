# Overnight improvements — EnGarde — 2026-06-12

**Mode:** autonomous overnight. Mandate: research project, generate & implement safe high-value improvements (features, bugfixes, infra, competitor-inspired). Nothing broken; all new code working & verified. Target >5h.

**Branch:** `feat/overnight-improvements-2026-06-12` (off master).

**Research:** 4 parallel exploration agents (code-map, competitor/FIE feature gaps, domain bug-hunt, infra/CI). Prior tech-debt audit (2026-05-30) already merged — not re-doing it.

---

## Backlog (prioritized, value/risk)

### Wave 1 — Domain correctness bugfixes (pure, unit-testable, low risk)
- **F1** `BoutEngine.undo()` score branches leave inconsistent `isOver`/winner when undoing a touch scored past threshold (twin of already-fixed H4 red-card path). + score/double controls not disabled after game-over in standalone bout. **MED, 88%**
- **F2** Double-touch during PRIORITY arbitrarily awards LEFT and ends bout. FIE t.40: simultaneous in priority is annulled. **MED, 80%**
- **F3** `PoolRepository.updateBoutScore` (edit dialog) persists FIE-illegal 4:4 draws as COMPLETED, corrupts standings. `recordBoutResult` already rejects it; edit path doesn't. **MED, 85%**
- **F5** `giveRedCard` skips sabre break-at-8 (penalty point landing on 8 won't trigger break). **LOW, 88%**
- **F4** `PoolEngine.buildMatrix` ignores `excludedSeeds` param (dead/misleading). **LOW, 90%**

### Wave 2 — FIE card escalation (feature, correctness)
- 2nd group-1 offense → red card (+point to opponent), not silent `AlreadyHasCard`. Black card → exclusion. FIE t.114+. Undo support + tests.

### Wave 3 — Quick pool score-entry (feature, high value)
- Referee enters final score (e.g. 5–3) directly on matrix cell without running full timer bout. Builds on existing `recordBoutResult`.

### Wave 4 — HEADLINE: Direct Elimination (DE) tableau
- Seed qualified fencers from pool rankings → bracket of next pow-2 with byes (FIE Organisation Rules). New `domain/DeTableau.kt` (TDD), `DeTableauEntity`/`DeBoutEntity` + Room migration v1→v2, bracket UI, nav, reuse `BoutEngine` for DE bouts, final classification. Unit tests for seeding/byes/progression.

### Wave 5 — Polish
- PDF multi-page (truncates at 8 fencers). i18n hardcoded strings (Bout #, Left/Right, V/D). BoutResult instrumented test. CSV results export.

### Wave 6 — Infra (independent files, parallel-safe)
- Detekt + baseline (non-blocking existing), unit-test CI job, Kover coverage, Dependabot, README + CONTRIBUTING.

---

## Execution log
(updated per wave)

### Wave 1 ✓ (commit 672ad1e)
F1-F5 fixed. +10 unit tests. testDebugUnitTest: 88 tests, 0 failures (independently re-verified via test-results XML). assembleDebug SUCCESSFUL. F4 decision: removed dead `excludedSeeds` param (MatrixCell had no field; wiring = separate feature).

### Wave 2 ✓ (commits a95f301 + 3fe2d54)
FIE t.114 card escalation: 2nd group-1 yellow→red(+pt), 3rd→black(exclusion), direct black-card action, all undoable, respects sabre break-at-8. Black-card UI button + strings in 3 locales. +10 BoutEngine tests → 98 total green, lintDebug clean (re-verified).

### Wave 3 ✓ (commit 5383cc5)
Quick pool score-entry: tap PENDING matrix cell → score dialog → recordBoutResult (same path as timer bouts), draw rejected via snackbar, timer flow untouched, pending-cell highlight + testTags. +2 instrumented tests. assembleDebug/AndroidTest/lint green.

### Wave 4 — DE tableau (in progress)
4a domain (seeding/byes/progression/classification) → 4b persistence (additive migration v1→v2 + migration test) → 4c UI+nav+instrumented.

### Wave 4 ✓ DE tableau (commits 99f2c26, 83a770e, 83fe8e3)
Full Direct Elimination after pools, end-to-end:
- 4a domain: seeding (canonical outer-bracket), byes on top seeds, immutable progression, FIE final classification (joint-3rd). +38 unit tests (136 total).
- 4b persistence: de_tableau/de_match entities, DeRepository, ADDITIVE migration v1→v2 (no destructive fallback — user pools survive), schema 2.json, MigrationTest (room-testing).
- 4c UI/nav: ui/de/ bracket screen + classification, GroupDashboard "Proceed to DE" (gated on pool complete), Config.DeTableau/DeBout, DE bouts reuse BoutComponent at mode=15 → recordMatchResult advances winner. +instrumented test.
All builds green (unit/assembleDebug/androidTest/lint), independently re-verified.
