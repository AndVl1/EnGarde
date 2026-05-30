# Технический аудит EnGarde — 2026-05-30

**Тип:** REVIEW → REFACTOR (без новых фич)
**Метод:** 5 параллельных аудит-агентов (code-reviewer, qa, tech-researcher, security-tester, diagnostics) + manual-qa на эмуляторе pixel6_api34 (API 34) + детерминированная симуляция Berger-алгоритма.
**Состояние сборки:** `assembleDebug` BUILD SUCCESSFUL. App установлен, запущен, крашей за сессию manual-qa нет.

---

## Сводка: общее состояние

Кодовая база **аккуратная для side-проекта**: чистое разделение domain/data/ui, иммутабельный `BoutEngine`/`PoolEngine` без Android-зависимостей, корректная Decompose-навигация, выдержанная TestTag-конвенция. Security-постур хороший для оффлайн-утилиты (allowBackup=false, FileProvider locked, нет инъекций, нет WebView, R8 on).

**Главные слабости (реальный тех-долг):**
1. Полное отсутствие unit-тестов на доменную логику (FIE-правила!).
2. `getPoolById` игнорирует параметр — возврат чужого пула.
3. Строковые «enum'ы» статусов в data-слое вместо типобезопасных типов.
4. Каскад из 3 collect + N+1 запросов в GroupDashboard.
5. 67 MissingTranslation (групповой функционал не переведён на русский).
6. Закоммиченный google-services.json (по природе публичен, но гигиена).

---

## Развенчанные «ложные тревоги» (проверено детерминированно)

| Находка агента | Severity заявл. | Вердикт после проверки |
|---|---|---|
| Berger-алгоритм некорректен (code-reviewer #1, diag BUG-10) | CRITICAL | **ЛОЖНАЯ.** Симуляция N=5..8: все C(N,2) пары уникальны, полны, 0 дублей/пропусков. Алгоритм верен. Реальная проблема — лишь отсутствие теста. |
| Compose Compiler mismatch 1.5.15 vs Kotlin 2.1.21 (architect) | CRITICAL | **ЛОЖНАЯ.** `compose-compiler=1.5.15` в toml — мёртвая запись, нигде не используется. Проект на `kotlin.plugin.compose`, версия гейтится автоматически. Сборка ОК. Это просто мусорная строка → удалить. |
| BUG-4 race инициализации Bout | HIGH | **НЕ ВОСПРОИЗВЁЛСЯ** (manual-qa): mode 15 применился, таймер 3:00 при мгновенном старте. Риск реальный, но окно мало. |
| BUG-1 потеря результата при быстром Back | CRITICAL | **НЕ ВОСПРОИЗВЁЛСЯ** (manual-qa): бой записался COMPLETED. Но запись в `init` всё равно хрупкая — стоит укрепить. |

---

## Подтверждённые находки (приоритизировано)

### CRITICAL

**C1. `getPoolById` игнорирует `poolId`** — `data/PoolRepository.kt:82-83`
```kotlin
fun getPoolById(poolId: Long): Flow<PoolEntity?> = db.poolDao().getActivePool() // игнорирует poolId
```
При >1 пуле Dashboard/Confirm/PDF получат данные чужого пула (оружие/режим/имена). Confidence 99%.
**Fix:** добавить Flow-`getById(id)` в PoolDao, использовать реальный poolId.

### HIGH

**H1. Нет unit-тестов доменной логики.** Только placeholder `2+2`. Не покрыты: `FieBoutOrder` (Berger), `BoutEngine` (502 строки: sabre break@8, приоритет, undo 8 типов, секции), `PoolEngine` (FIE-ранжирование V/M%/Index/TD/head-to-head). Все — pure JVM, легко тестируются. Confidence 100%.

**H2. 67 MissingTranslation** — `values/strings.xml:82-162` весь групповой функционал не в `values-ru-rRU`. lintDebug FAILED (abortOnError). Русскоязычные видят групповые экраны на английском. Confidence 100%.

**H3. Каскад collect + N+1 в GroupDashboard** — `GroupDashboardComponent.kt:90-189` + `PoolRepository.kt:105-124`. Три независимых collect, каждый дёргает recalculateStandings → повторное чтение БД; `getPoolBoutsWithNames` делает getById на каждого фехтовальщика (5-8 запросов на эмит). Race: rankings могут считаться при fencerCount=0. Confidence 80-95%.

**H4. BUG-6: undo красной карты в завершённом бою** — `BoutEngine.kt:456-468`. **ВОСПРОИЗВЁЛСЯ** (manual-qa): бой 5:0 WINNER → красная карта (5:1) → Undo → счёт 4:1, но WINNER остаётся. Несогласованное состояние. Confidence 90%.

**H5. Строковые «enum'ы» в data-слое** — `PoolRepository.kt` ("IN_PROGRESS","PENDING","COMPLETED","FORFEIT","LEFT","RIGHT") при наличии enum `PoolStatus`/`BoutStatus`/`FencerSide`. `BoutStatus.valueOf(bout.status)` упадёт при рассинхроне, опечатки не ловятся компилятором. Confidence 85%.

**H6. createPool не атомарен** — `PoolRepository.kt:20-76`. 4 отдельные DAO-операции без транзакции → «полупул» при сбое. **Fix:** `database.withTransaction { }`. Confidence 85%.

**H7. Room без миграций** — `EnGardeDatabase.kt` version=1, нет `fallbackToDestructiveMigration` и миграций. Любое изменение схемы → краш у пользователей с данными. Confidence 75%.

### MEDIUM

- **M1.** Несогласованность mode: `createPool` требует `mode in listOf(4,5)`, остальной код 5/15 → `IllegalArgumentException` при пуле «до 15». `PoolRepository.kt:22`. Confidence 85%.
- **M2.** Проглоченные ошибки: `e.printStackTrace()` при экспорте PDF — пользователь не узнаёт о провале. `GroupDashboardComponent.kt:228`. Заменить на Crashlytics.recordException + snackbar.
- **M3.** Ничья (winner=null) сохраняется как COMPLETED — в FIE ничьих в бою нет, искажает рейтинг. `PoolRepository.kt:137-141`.
- **M4.** Таймер 100 Гц (delay(10)) на Main + полный copy() — jank/батарея. Таймер не стопается в фоне (привязан к onDestroy, не onStop). `BoutComponent.kt:85-106`.
- **M5.** UI: матрица результатов обрезается с 5+ фехтовальщиками, горизонтальный скролл не работает (manual-qa). При 6-8 — критично.
- **M6.** Hardcoded строки в компонентах («Bout #», «Left»/«Right»). `GroupDashboardComponent.kt`, `BoutComponent.kt:187`.
- **M7.** CI: Allure-результаты не собираются (`adb pull` отсутствует в workflow); lint не запускается в CI; JUnit glob неполный.
- **M8.** Неиспользуемые ресурсы (`array.xml`, ~15 цветов) от старой Activity-архитектуры. `DefaultLocale` warnings в String.format.

### LOW
- Deprecated иконки/Divider (4 compiler warnings): `Icons.AutoMirrored`, `HorizontalDivider`.
- Мёртвый `compose-compiler=1.5.15` в toml.
- Ручной DI без `retainedComponent` (пересоздание БД при rotation).
- Мёртвые `val result` в BoutComponent, недостижимая ветка PRIORITY в undo.
- `@OptIn(DelicateDecomposeApi)` + FQN вместо импортов в RootComponent.
- PdfExporter без многостраничности (break обрезает данные при 8 фехтовальщиках).
- BoutResult-экран не покрыт тестами (Page Object есть).

### SECURITY (отдельно)
- **SEC-001 (HIGH-гигиена):** `app/google-services.json` закоммичен (commit 3e95e5f), `.gitignore` добавлен позже → файл всё ещё tracked. Android Firebase-ключ публичен по природе, но: настроить App/API restrictions в GCP, убрать из tracking. CI уже на dummy.
- Остальное — LOW/INFO. Реальных уязвимостей нет.

---

## manual-qa: smoke-тест (всё OK, крашей нет)
Home, Settings, Single Bout (счёт/таймер/undo/карты/double-touch), Group Setup, Dashboard, BoutConfirm, BoutResult, PDF — рендерятся и работают. Double-touch корректно запрещён при (mode-1):(mode-1).
Скриншоты: `vibe-report/screens/`.

---

## Реализация (ветка refactor/tech-debt-2026-05)

Объём: полный тех-долг. Волнами по cohesion (агенты не конфликтовали на файлах), верификация между волнами.

| Коммит | Что |
|---|---|
| `ba24537` Wave A | 78 domain unit-тестов (страховка); 54 ru-перевода (MissingTranslation 67→0); google-services.json untracked; мёртвый compose-compiler удалён; Room ProGuard; lint{}; CI (adb pull allure + recursive JUnit glob + gradle cache) |
| `691d8db` Wave B | **C1** getPoolById→реальный id; **H5** enum через Room TypeConverter (схема БД та же, миграция не нужна); **H6** атомарный createPool; **M3** priority-победитель; **M1** mode-валидация задокументирована (4/5 — реальный UI-контракт) |
| `6345bf3` Wave C-1 | **H4** пересчёт isOver после undo; таймер deprecated-чистка |
| `db8409a` Wave C-2 | **H3** Dashboard combine + N+1 (getByIds); **M2** PDF-ошибки→Snackbar+Crashlytics; матрица; AutoMirrored/HorizontalDivider/Locale.US |
| `631d099` refix | **M4** таймер реально встаёт в фоне (инкрементальный delta вместо wall-clock от стартовой точки); **M5** матрица реально скроллится (единый scrollState + фикс. ширина ячеек вместо weight(1f) — он схлопывал контент в horizontalScroll) |

### Верификация — всё зелёное
- `assembleDebug` + `assembleDebugAndroidTest`: BUILD SUCCESSFUL
- Unit: **78 passed, 0 failed** (домен покрыт впервые)
- Instrumented: **33/33 Ultron passed** на pixel6_api34
- manual-qa (под caffeinate): H4/M5/M4 подтверждены на устройстве (таймер 2:51→2:51 после HOME+6с; 6-fencer матрица скроллит все колонки). Крашей нет. PDF без молчаливого проглатывания.

### Первая попытка M4/M5 провалилась — manual-qa поймал
Первый фикс M4 (doOnStop) и M5 (horizontalScroll на header) НЕ работали на устройстве. manual-qa это обнаружил, developer-mobile инструментировал (Log.d+logcat), нашёл истинные причины и переделал (`631d099`). Урок: фиксы UI/lifecycle нельзя считать готовыми без прогона на устройстве.

### Развенчанные ложные CRITICAL
- Berger-алгоритм КОРРЕКТЕН (симуляция N=5-8: все C(N,2) пары, 0 дублей). Теперь покрыт тестом.
- compose-compiler 1.5.15 — мёртвая toml-строка. Удалена.

### Требует ручных действий (вне кода)
- GCP Console: Application/API restrictions для Firebase-ключа.
- LOW не сделано: ручной DI без retainedComponent; PdfExporter многостраничность; BoutResult без instrumented-теста; UX Group Setup (перескок фокуса ввода).

Скриншоты: `vibe-report/screens/` (audit_*, regress_*, refix_*).
