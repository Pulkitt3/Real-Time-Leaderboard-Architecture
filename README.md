# Real-Time Leaderboard Architecture Assignment

This project implements a modular, real-time leaderboard system for Android using Kotlin, Coroutines, Flow, MVVM, and Jetpack Compose.
## Video for reference
Screen_recording_20260316_105703.mp4
## How to run

1. Open project in Android Studio (latest stable).
2. Let Gradle sync and download dependencies.
3. Run the `app` module on an emulator/device (minSdk 24+).
4. The leaderboard updates automatically in real time.

## Architecture overview

The app is split into three layers:

- Data/Engine layer (score event producer)
- Domain layer (ranking and leaderboard state logic)
- UI/Presentation layer (ViewModel + Compose screen)

Gradle module layout:

- `:gameplay` -> gameplay score event producer
- `:leaderboard` -> score consumer and ranking logic
- `:app` -> Android UI and orchestration

### Module responsibilities

#### 1) Score Generator / Game Engine module

Gradle module: `:gameplay`

Package: `app.gameleaderboard.engine`

- `ScoreGenerator`: UI-agnostic contract that emits `Flow<ScoreUpdate>`.
- `RandomScoreGenerator`: reusable implementation simulating backend score events.
- Uses a seeded `Random` for deterministic event generation behavior per session instance.
- Emits updates at random intervals (500ms to 2000ms).
- Picks random users and only increases scores.

#### 2) Leaderboard module (consumer)

Gradle module: `:leaderboard`

Package: `app.gameleaderboard.leaderboard`

- `LeaderboardEngine`: consumes score updates and maintains derived leaderboard state.
- `RankingCalculator` + `DefaultRankingCalculator`: pure ranking logic, testable and separated from UI/ViewModel.
- Ranking rules:
  - Sort by score descending
  - Same score -> same rank
  - Next rank skips accordingly (competition ranking)

#### 3) UI layer

Gradle module: `:app`

Package: `app.gameleaderboard.presentation`

- `LeaderboardViewModel`: starts/stops score engine with lifecycle-safe ownership.
- Exposes leaderboard as `StateFlow<List<LeaderboardEntry>>`.
- `LeaderboardScreen`: Compose UI using `LazyColumn` and row highlight animation for recently updated player.

## Why Compose

Compose is used because this assignment is real-time and state-driven; Compose maps naturally to Flow/StateFlow and makes update-driven UI simpler while keeping UI code concise.

## Performance and lifecycle decisions

### How UI thread blocking is avoided

- Score generation and leaderboard computation run on coroutine flows and default dispatcher.
- No heavy or blocking work on main thread.

### How unnecessary recompositions are reduced

- State is exposed as single immutable list stream (`StateFlow`).
- `LazyColumn` uses stable key (`playerId`) for item identity.
- Ranking logic lives in domain and emits already-computed UI-ready state.

### Memory leak prevention

- Generator owns a dedicated coroutine scope and is explicitly released in `ViewModel.onCleared()`.
- UI only collects lifecycle-aware state (`collectAsStateWithLifecycle()`).

### Rotation behavior

- `ViewModel` survives configuration changes, so leaderboard stream and state remain active.
- UI re-collects state after recomposition with minimal disruption.

### Background behavior

- `collectAsStateWithLifecycle()` respects lifecycle state and avoids unnecessary active collection in background.
- Generator is tied to ViewModel lifetime; it continues while ViewModel is alive and is cleaned when owner is destroyed.

## Scaling strategy

### 1K users

- Keep in-memory map of scores and incremental updates.
- Use efficient ranking recomputation (current version recomputes full sort each update; acceptable at this size depending on update rate).

### 100K users

- Move ranking to backend and stream top-N deltas to client.
- Use batched updates and debounce windows.
- Use specialized structures (indexed heaps/trees) for incremental rank updates instead of full sort.
- Paginate leaderboard UI and keep only visible window plus small cache.

## Trade-offs made

- Chosen architecture favors clarity and testability over full DI setup (manual factory instead of Hilt).
- Full ranking recomputation per event is simpler and correct, but not optimal for very large data volumes.
- UI uses one clear highlight animation instead of multiple simultaneous animations to keep behavior deterministic and stable.

## Leadership and ownership note

### Why modules are split this way

- Score generation and leaderboard consumption have different responsibilities and change rates.
- Separation allows reuse of engine in other surfaces (game replay, observer mode, testing tools).
- Domain logic becomes testable without Android/UI dependencies.

### Where ranking logic lives and why

- Ranking logic is in `DefaultRankingCalculator` (domain layer), not UI/ViewModel.
- This ensures deterministic behavior, easy testing, and clear ownership boundaries.

### Conscious decisions

- Prioritized correctness, modularity, and lifecycle safety.
- Kept implementation straightforward for interview readability.
- Added unit tests for ranking rules as optional lead-signal item.

## Code review simulation (6-8 comments)

### Must Fix

1. **Missing lifecycle-aware collection in UI**
   - If plain `collectAsState()` is used, background collection may continue unnecessarily.
   - Use `collectAsStateWithLifecycle()`.

2. **Ranking logic embedded in ViewModel**
   - Business rules become hard to test and reuse.
   - Move to domain `RankingCalculator`.

3. **Generator not stopped/released**
   - Long-running coroutine can leak after screen close.
   - Ensure cancellation in lifecycle owner (`onCleared`).

### Improvement

4. **No stable keys in LazyColumn**
   - List reorder can cause item flicker and poor animations.
   - Key items by stable `playerId`.

5. **No deterministic seed support**
   - Debugging and tests become flaky.
   - Inject seed/config into score generator.

6. **No explicit module contracts**
   - Tight coupling risk between producer and consumer.
   - Keep producer behind `ScoreGenerator` interface.

### Tech Debt

7. **No DI framework**
   - Manual wiring is fine for assignment, but can scale poorly.
   - Migrate to Hilt/Koin for larger project.

8. **No observability hooks**
   - Hard to debug production ranking incidents.
   - Add structured logs and metrics around update latency/ranking duration.

## If this must ship in 7 days

### Non-negotiable

- Correct ranking rules and deterministic behavior
- Lifecycle-safe real-time stream handling
- Unit tests for ranking logic
- Crash-free baseline and basic performance verification

### Defer/cut

- Advanced theming/polish
- Multi-screen navigation
- Deep analytics dashboards
- Complex offline sync

## Work split by level

### Junior developer

- Build static leaderboard row components
- Write preview/sample UI states
- Add string resources and small UI refinements

### Mid-level developer

- Implement score generator module
- Integrate ViewModel flow pipeline and UI rendering
- Add unit tests for domain logic

### Lead (me)

- Define architecture boundaries and contracts
- Own ranking correctness and lifecycle/performance strategy
- Review code quality and production-readiness decisions
- Drive risk management and scope cuts for deadline

## What I would improve with more time

- Add benchmark test for ranking cost under high update frequency.
- Add integration tests for end-to-end flow behavior.
- Introduce DI (Hilt) and configuration-driven engine parameters.
- Add anti-cheat heuristics (score velocity thresholds, anomaly flags).

## CI and quality checks suggestion (ktlint + detekt)

Suggested CI stages:

1. Build and unit tests
   - `./gradlew testDebugUnitTest :leaderboard:test :gameplay:test`
2. Android static checks
   - `./gradlew lintDebug`
3. Kotlin style and static analysis
   - `./gradlew ktlintCheck detekt`

Recommended CI gate policy:

- PR cannot merge unless all checks pass.
- Run checks on every PR and push to main branch.
- Upload test reports and lint artifacts for debugging failed pipelines.

Example GitHub Actions steps:

- Checkout + JDK 17 setup
- Cache Gradle
- Run `./gradlew lintDebug testDebugUnitTest :leaderboard:test :gameplay:test`
- Run `./gradlew ktlintCheck detekt`

Implemented in this repository:

- CI workflow: `.github/workflows/ci.yml`
- Ktlint plugin enabled on `:app`, `:gameplay`, `:leaderboard`
- Detekt plugin enabled on `:app`, `:gameplay`, `:leaderboard`

## Anti-cheat ideas for live tournaments

- Score velocity rules: flag impossible score jumps per second/minute.
- Event integrity: sign score events server-side and verify signature on ingest.
- Device/session fingerprinting: detect multi-account abuse patterns.
- Outlier detection: compare player progression against cohort baselines.
- Risk scoring: combine weak signals into a confidence score and queue manual review.
- Progressive enforcement: warn, shadow-ban, then hard-ban for repeated high-confidence abuse.

## Production readiness improvements

- Observability:
  - Add structured logs for update latency and ranking computation duration.
  - Export metrics for event throughput, dropped events, and UI render delay.
- Reliability:
  - Add retry/backoff and reconnection strategy for real backend streams.
  - Add bounded buffers and overflow strategy to prevent memory spikes.
- Security:
  - Move score authority fully to backend; client should render, not authorize.
  - Add auth token refresh and replay protection for score event transport.
- Performance:
  - Use diff-based updates for large lists and benchmark under high frequency updates.
  - For large tournaments, stream top-N + windowed pagination instead of full list.
- Delivery quality:
  - Enforce branch protection + required CI checks.
  - Add release checklist with smoke tests and rollback plan.

