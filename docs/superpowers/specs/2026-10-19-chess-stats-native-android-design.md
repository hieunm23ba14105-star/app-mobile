# Chess Stats Android App Design

## Goal

Port the existing `chesscom-stats.html` experience into a native Android app written in Java. Do not embed the page in a WebView. Preserve the existing visual direction and the requested interaction where the brand header and player profile stay attached at the top while statistics scroll below them.

## Current project

The Android project is an untouched Kotlin/Jetpack Compose starter. The selected implementation is Java with Android Views and XML layouts. The app should not require Kotlin or Compose for its runtime UI after the port.

## User experience

- Start in Vietnamese and offer a VI/EN selector.
- Landing screen: Chess.com blue video background, brand row, username input, and lookup button. Keep a blue gradient fallback if the remote video cannot play.
- Lookup uses a Chess.com username, accepts an optional leading `@`, and shows loading, empty-input, not-found, rate-limit, HTTP, and connection errors without losing the entered username.
- On a successful lookup, animate into a compact top brand header and player identity card with Chess.com avatar (silhouette fallback), display name, username, game-type count, and a change-player action.
- Keep the compact brand header and profile card pinned together above the scrollable statistics.
- Show available game types as selectable cards. Selecting a type updates its summary: current and peak rating, dates, peak-game link when available, win/draw/loss counts and bar, and optional public API extras.
- Preserve the selected game type and visible status when switching language. Do not add the removed practice section or the removed PubAPI footer note.

## Implementation shape

- `MainActivity.java`: owns screen state, input events, language changes, and the native view hierarchy.
- `ChessComRepository.java`: makes the public profile and stats requests off the UI thread, parses JSON, and returns success or typed errors to the Activity.
- Java model classes for player profile, game type, and rating/record summary, kept small and only as complex as required by the API responses.
- XML layouts and drawable resources define the brand header, lookup card, profile card, game cards, and summary panels.
- The results screen uses a fixed top chrome container for the compact brand and profile card, with a separate scroll view for statistics. The landing and results states share the same visual elements and animate between the expanded and compact layouts.
- Use platform Java APIs (`HttpURLConnection`, `org.json`, `ExecutorService`, `ImageView`, and `VideoView`) unless an implementation need proves they are insufficient. Do not add a networking or image library by default.
- Add the Android Internet permission. Remove the unused Compose/Kotlin starter activity, theme code, plugin, and dependencies so the app UI/build is Java-based; retain the existing package/application ID and launcher icon.

## API and failure handling

- Fetch `https://api.chess.com/pub/player/{username}/stats` and `https://api.chess.com/pub/player/{username}`.
- Treat the stats response as required and the profile response as optional; a profile failure should not discard valid statistics.
- Perform network and image work off the main thread and update views on the main thread. Close streams/connections and restore the search button in all outcomes.
- If the avatar or landing video fails, keep the screen usable with the silhouette or blue gradient fallback.
- Do not persist usernames or add authentication.

## Validation

- Build a debug APK with the project's Gradle wrapper to confirm the app compiles.
- No automated test suite is included in this implementation scope.

## Assumptions

- This is an online public-profile lookup app and does not need Chess.com sign-in.
- The existing remote WebM remains the landing background source, with a local color fallback.
- The requested sticky behavior applies after a lookup, when the results page can scroll; the landing screen remains viewport-filling.
