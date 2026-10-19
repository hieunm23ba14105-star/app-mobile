# Chess Stats Java Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Replace the Android starter screen with a native Java Chess.com player stats app matching the approved design.

**Architecture:** Use one Java `Activity` with Android Views/XML, a small Java repository/model layer, and platform networking/media APIs. The landing page transitions to a results layout whose brand/profile chrome stays outside the statistics `ScrollView`.

**Tech Stack:** Java 11, Android SDK (min 24 / target 37), XML layouts/drawables, `HttpURLConnection`, `org.json`, `ExecutorService`, and `VideoView`; no WebView or third-party app dependencies.

**Spec:** `docs/superpowers/specs/2026-10-19-chess-stats-native-android-design.md`

## Global Constraints

- Build the user interface and app behavior in Java; do not use WebView.
- Keep application ID `com.chess.stats`, min SDK 24, and target SDK 37.
- Read public Chess.com data; do not add sign-in or username persistence.
- Default to Vietnamese and support Vietnamese/English in the visible interface.
- Keep the landing video optional with a gradient fallback.
- Keep the player header and identity card pinned above the scrollable results.
- Do not restore the removed practice section or PubAPI footer note.
- Do not add or run automated tests in this implementation.

## Review Focus

- Stats API failures (404, 429, HTTP, connection) must leave the username available for retry — owned by Task 3.
- A missing profile response or avatar must not block valid stats and must show a fallback — owned by Tasks 2 and 3.
- Empty or malformed stats data must not crash rendering — owned by Tasks 2 and 3.
- A failed or unsupported landing video must leave the landing screen usable — owned by Task 4.
- Language changes while results are visible must preserve the selected game type — owned by Task 3.

---

### Task 1: Replace the Compose starter with a Java Android shell

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Remove: `app/src/main/java/com/chess/stats/MainActivity.kt`
- Remove: `app/src/main/java/com/chess/stats/ui/theme/Color.kt`
- Remove: `app/src/main/java/com/chess/stats/ui/theme/Theme.kt`
- Remove: `app/src/main/java/com/chess/stats/ui/theme/Type.kt`
- Remove: Kotlin starter tests under `app/src/test` and `app/src/androidTest`
- Create: `app/src/main/java/com/chess/stats/MainActivity.java`
- Create: `app/src/main/res/layout/activity_main.xml`

**Interfaces:**
- `MainActivity` is the exported launcher activity and inflates `activity_main.xml`.
- The module uses only the Android application plugin and Java 11 compile settings.

- [x] Remove the Compose/Kotlin starter plugin, dependencies, Activity, theme source, and starter Kotlin tests.
- [x] Add the Android Internet permission and keep `MainActivity` as the launcher.
- [x] Add the Java `MainActivity` and an initial native XML view shell that builds with the existing application ID and SDK levels.
- [x] Run `gradlew.bat :app:assembleDebug` and confirm it exits successfully.

### Task 2: Add public API models and asynchronous repository

**Files:**
- Create: `app/src/main/java/com/chess/stats/data/ChessComRepository.java`
- Create: `app/src/main/java/com/chess/stats/data/PlayerProfile.java`
- Create: `app/src/main/java/com/chess/stats/data/GameModeStats.java`
- Create: `app/src/main/java/com/chess/stats/data/LookupResult.java`

**Interfaces:**
- `ChessComRepository.lookup(String username, Callback callback)` performs requests off the main thread and delivers one `LookupResult` through `Callback.onComplete(LookupResult result)` on the main thread.
- `LookupResult` contains an optional `PlayerProfile`, a list of `GameModeStats`, and a typed error (`NONE`, `NOT_FOUND`, `RATE_LIMITED`, `HTTP`, `CONNECTION`, or `INVALID_DATA`) plus an HTTP code when applicable.
- `GameModeStats` exposes game key/rule/speed, current rating/date, peak rating/date/game URL, record values, and optional API extras.

- [x] Define the small immutable profile, game mode, and lookup result models with nullable optional API values.
- [x] Implement stats retrieval from `/pub/player/{username}/stats` and best-effort profile retrieval from `/pub/player/{username}` using `HttpURLConnection` and `org.json`.
- [x] Parse supported game keys and sort them in the same order as the web page.
- [x] Map 404, 429, other HTTP statuses, connection failures, and malformed JSON into the typed result without losing successful stats when the profile request fails.
- [x] Run `gradlew.bat :app:assembleDebug` and confirm it exits successfully.

### Task 3: Build the results UI and lookup state flow

**Files:**
- Modify: `app/src/main/java/com/chess/stats/MainActivity.java`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/java/com/chess/stats/ui/Translations.java`
- Create: `app/src/main/java/com/chess/stats/ui/StatsViews.java`
- Create or modify: `app/src/main/res/drawable/*.xml`

**Interfaces:**
- `MainActivity` submits a normalized username to `ChessComRepository.lookup` and renders its `LookupResult`.
- `Translations.text(String key, Language language)` returns the visible VI/EN text for a UI key.
- Selecting a `GameModeStats` rerenders the summary for that mode; changing language rerenders labels while preserving selection.

- [x] Create the landing layout with brand, VI/EN selector, username input, lookup button, and bottom lookup card.
- [x] Implement empty/loading/success/error states; preserve the input on errors and provide a change-player action after a successful lookup.
- [x] Render player name, username, avatar with silhouette fallback, selectable game modes, current/peak ratings, dates, peak-game link, record bar/counts, and optional extra statistics.
- [x] Match the blue/white card styling and ensure the compact header and profile card stay pinned while only the results scroll.
- [x] Preserve the selected mode and status when switching languages.
- [x] Run `gradlew.bat :app:assembleDebug` and confirm it exits successfully.

### Task 4: Add the landing video, transition, and final app integration

**Files:**
- Modify: `app/src/main/java/com/chess/stats/MainActivity.java`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- The landing `VideoView` is active only before lookup; search transitions to the compact header/profile results state.
- Video playback failure leaves the blue gradient visible.

- [x] Load the supplied Chess.com WebM in the landing background, mute and loop it, and stop/hide it when lookup begins.
- [x] Animate the landing-to-results transition and implement the change-player reset path.
- [x] Handle missing avatar and video playback errors with the specified visual fallbacks.
- [x] Run `gradlew.bat :app:assembleDebug` and confirm it exits successfully.

## Plan Self-Review

- Spec coverage: Java-only native shell and build conversion (Task 1); Chess.com APIs and failure mapping (Task 2); bilingual lookup/results and pinned chrome (Task 3); landing video and animation (Task 4).
- No user interface requirements are left without an owning task.
- Network errors are kept in `LookupResult`; UI translation and input retention belong to Task 3.
- No automated tests are added or run, per the task constraints; each task uses a Gradle debug build as compilation verification.
- The project is not a Git repository, so plan/spec commits are unavailable.

## Execution Rulings

- Ruling: Work directly in the user-provided project folder — the user explicitly asked for this Android project to be coded, and the folder is not a Git repository, so a worktree or commit cannot be used. Risk: there is no VCS diff to restore from if an edit is wrong.
- Ruling: Build repeated game/summary cards from focused Java view helpers instead of separate item XML files — the number and optional fields are driven by API data, and the helper keeps the interface in Java as requested. Risk: the view helper may need to be split if it grows beyond the scope of these cards.
- Ruling: Use Gradle assemble for compile verification, with no automated tests — the developer instruction forbids adding or running tests unless asked.


## Completion

- Debug APK build: `.\gradlew.bat :app:assembleDebug` — successful.
- Automated tests were not added or run, per the plan constraint.
