# NODE TV Android v0.1.2 — Fullscreen Gestures + GitHub-Mandatory Updates

## Canonical lineage and testing status

- This version is an incremental update based on the supplied **v0.1.1 Compiler Hotfix #2 FULL SOURCE**. It does NOT reset the project to v0.1.0, remove any existing channel and preference features, or remove LibVLC fallback.
- v0.1.1 Hotfix #2 has not yet been shown to build successfully on GitHub. **This v0.1.2 source is not compiled / phone-tested in this delivery environment**; run the existing GitHub Actions workflow and report the earliest compiler error if it fails.
- `applicationId`: `net.nodedevelopment.nodetv`; `versionName`: **0.1.2**; `versionCode`: **3**; Android 8.0+.

## What changed

- Fullscreen landscape now has a transparent gesture layer over video, separate from video engines.
- Vertical swipes on the LEFT half adjust **window-only brightness** (restored when leaving fullscreen). No Android system-wide brightness-write permission is needed.
- Vertical swipes on the RIGHT half adjust the **phone's media volume** (`AudioManager.STREAM_MUSIC`). This is distinct from the in-app volume slider / mute. The Android permission `MODIFY_AUDIO_SETTINGS` is declared.
- Short taps inside the CENTER zone toggle pause/play only when a channel has started. Taps no longer exit fullscreen; an overlay HUD shows the state. Some live streams resume at the current live edge, not at the previously displayed frame.
- Android's native system Back gesture and Back button are retained to leave landscape playback and return to the portrait user interface without resetting the tuned channel.
- Brighter automatic **NEW UPDATE AVAILABLE** banner appears near the top of portrait UI, with UPDATE NOW / LATER / DETAILS for optional updates. Top-right Updates button is removed; tapping version text in the footer runs an optional manual check.
- Verified official release **mandatory-update** mode uses the GitHub Release description marker `<!-- NODETV_UPDATE_MODE: MANDATORY -->`, with a blocking update card, UPDATE NOW + DETAILS (no LATER). This applies to clients that contain v0.1.2+ mandatory-update parsing, and only after a confirmed new official release with the expected versioned APK + SHA-256 asset exists. Android still requires the user to approve installation.
- Check at launch, on foreground after a throttle, and every 30 minutes while open. No new hosting, subscription or API key.

## How to publish mandatory vs optional updates

The GitHub Release body can contain the following hidden comment (exact literal string):

    <!-- NODETV_UPDATE_MODE: MANDATORY -->

Include that line if you want installed NODE TV v0.1.2+ clients to block playback while a newer version is available. Omit it for an optional release; optional versions offer LATER. This is configured when you publish each GitHub Release; no backend is needed.

**Limitations and protections:** Old v0.1.1 and v0.1.0 clients cannot be retroactively changed to enforce this new rule. Network/Release API failures do not invent a version or newly lock an offline client. If the official APK and corresponding checksum are missing or invalid, the update checker ignores the release. A user can still exit the Android application or refuse installation; sideload installs cannot be silent.

## Exact source files to upload to GitHub (overwrite those already present)

1. `app/build.gradle`
2. `app/src/main/AndroidManifest.xml`
3. `app/src/main/java/net/nodedevelopment/nodetv/MainActivity.java`
4. `app/src/main/java/net/nodedevelopment/nodetv/UpdateClient.java`
5. `app/src/main/java/net/nodedevelopment/nodetv/FullscreenGestureView.java`  **(NEW)**
6. `README.md`
7. `GITHUB-RELEASE-v0.1.2.txt` **(NEW)**
8. `START-HERE-v0.1.2-FULLSCREEN-GESTURES-AND-GITHUB-UPDATES.md` **(NEW)**
9. `START-HERE-NEW-CHAT-HANDOFF.md` (historical-document heading clarified)

Preserve the existing `.github/workflows/build-apk.yml` file; this release changes no GitHub Actions steps. No SQL migrations, database, keystore or paid service is required.

## GitHub Actions — APK without Android Studio

1. Replace the listed files in the repository root using the matching paths, commit to `main`.
2. Visit **Actions → NODE TV Android APK → Run workflow** (or let your push start it).
3. Wait for the workflow result and open the artifact named **NODE-TV-Android-APK**.
4. Without all four permanent signing secrets, it contains **NODE-TV-Android-v0.1.2-DEBUG-TEST-ONLY.apk**. This is for phone feature testing ONLY and must NOT be added to official Releases. A debug APK may need uninstalling older builds due to different signing certificates.
5. With your existing private release keystore correctly configured in the four GitHub Actions secrets, it contains **NODE-TV-Android-v0.1.2.apk** and **NODE-TV-Android-v0.1.2-SHA256.txt**.
6. Only after a successful signed build and device tests, publish the GitHub Release with tag **v0.1.2**, title and complete description from `GITHUB-RELEASE-v0.1.2.txt`.
7. Attach the TWO exact files from step 5 to that Release. Do not upload `.jks`, passwords, Actions debug APKs or private signing data. Both assets must use precisely the filenames shown, or the in-app update checker will not find the release.
8. Test update by installing an older *release-signed* version, publishing the new release, starting the old app, and confirming Android's installer updates it in place; the signing certificate must match and versionCode must increase.

## Phone acceptance tests

- v0.1.2 installs and launches; worldwide / Philippines, search, categories, themes, favorite/recent work.
- Both ExoPlayer and VLC fallback tune streams as in v0.1.1. Retest known channels side by side with Windows.
- Fullscreen landscape: right vertical swipe controls media volume; left vertical swipe controls brightness; brief HUD disappears; leaving fullscreen restores prior per-window brightness.
- Center tap pauses; center tap resumes without leaving fullscreen. Swiping doesn't pause and tapping outside center doesn't exit.
- Built-in Android Back gesture leaves fullscreen to portrait with same channel selected.
- When a newer *official signed* GitHub Release is present, banner appears without manual interaction. Mandatory release marker blocks underlying interface and hides LATER; optional releases show LATER.
- Update downloads, validates expected SHA-256 and bytes, requests Android installation permission if needed and invokes package installer. Do not advertise background/silent installation.

## GitHub release fields (ready to copy)

See `GITHUB-RELEASE-v0.1.2.txt` in this ZIP. The public release should only be published after building and testing the correct signed APK.
