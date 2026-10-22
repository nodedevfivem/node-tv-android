# NODE TV Android v0.1.6 — Floating Channel Browser

## Canonical base, scope and version

- Input: the existing complete `NODE-TV-Android-v0.1.5-FULL-SOURCE.zip` (NOT v0.1.0/Windows sources).
- Version updated to **versionName 0.1.6** and **versionCode 7**, package unchanged: `net.nodedevelopment.nodetv`.
- New code touches only `MainActivity.java` plus the Gradle version metadata. GitHub Actions, LibVLC, playlist parser, updater, icons, signing configuration, and original assets are preserved unchanged.
- No SQL, web hosting, API keys, or runtime permissions added.

## UI behavior

1. Portrait has a prominent `BROWSE CHANNELS` button and available-channel count instead of a two-row list.
2. Press it to slide up a dark translucent bottom sheet, occupying approximately 76% of the content height. TV video remains behind the sheet; media playback is NOT reloaded/reparented.
3. The sheet offers a searchable, compact recycled ListView, category selector and tabs for Worldwide / Philippines / Favorites / Recently Watched.
4. The current channel is highlighted with a LIVE badge. The hearts toggle favorites, including when browsing Favorites.
5. Selecting a channel closes the sheet and tunes the requested stream. Dismissing the sheet without selecting a channel does not stop or change playback.
6. Close by `✕ CLOSE`, tap outside, Android Back or dragging down on the grab-bar marked `SWIPE DOWN TO CLOSE`. Swiping the *list* continues to scroll instead of dismissing the panel.
7. Mandatory update lock still blocks browsing when a required GitHub update is verified.

## Validation checklist before an official release

1. Commit only UPDATE-ONLY ZIP contents to `main`, preserving relative paths; open GitHub Actions → `NODE TV Android APK`.
2. Confirm a successful Java/Gradle compile and APK signature validation, if release secrets exist.
3. On a real phone, run VLC fallback playback, open Browse Channels, scroll/search/use tabs and favorites, and check audio/video continue behind the sheet.
4. Tap Close/outside/Back/drag handle and verify original stream was NOT restarted. Select another channel and verify switch + dismiss; check NOW PLAYING badge.
5. Test category changes with large Worldwide list, and after switching themes.
6. Test portrait → fullscreen → portrait multiple times, including the v0.1.5 audio-only/black-video regression; the sheet should remain closed in fullscreen.
7. Test mandatory GitHub update flow with a signed older build (official signature must match); confirm Android asks for install approval.
8. Only after testing publish a **non-prerelease** GitHub Release `v0.1.6` with `GITHUB-RELEASE-v0.1.6.txt` description and the THREE official signed assets.

## Signing & artifacts

With all four existing GitHub Actions signing secrets configured, the unchanged workflow is expected to generate `NODE-TV.apk`, `NODE-TV-Android-v0.1.6.apk`, and `NODE-TV-Android-v0.1.6-SHA256.txt`, plus a bundled Actions artifact. Without signing secrets the output is test-only `NODE-TV-TEST-ONLY.apk`; do not publish that as a production auto-update. The SHA256 file is `.txt` and NOT an `.exe`.

## Actual status

**SOURCE-ONLY CANDIDATE.** A static source review and ZIP integrity validation are possible in this environment; real Android SDK/Gradle build, phone UI/rendering, real IPTV connectivity and signing tests have NOT been performed here. The source is ready for the existing GitHub Actions compile/test cycle. Do not interpret the planned behaviors as proven on-device until tested.
