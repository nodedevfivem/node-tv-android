# NODE TV Android — v0.1.5

**Developed by NODE Development.** Free IPTV television player for Android phones (Android 8.0+).

Dedicated Android application, not a port of the Windows WPF binary. Source code is written in Java with AndroidX Media3 ExoPlayer (HLS support).

## First version features

- Live Worldwide and Philippines channel directories, loaded from their public M3U playlists.
- Retro wooden TV frame with the official NODE TV logo; immersive landscape video-only fullscreen.
- Search, category filter, saved **Favorite TV Channels**, 30 recently watched, last successful channel.
- Basic playback controls, volume/mute, current video dimensions, visual black-and-white CRT noise and optional low-volume hiss on stream errors / timeout.
- Eight named color preferences; accent swatch on restart.
- GitHub Release update checks at launch, on foreground and periodically; prominent portrait **NEW UPDATE AVAILABLE** banner and release-controlled mandatory-update gate.
- Verified APK download (SHA-256, size) and user-approved Android package installer handoff.
- No account, monthly hosting, background analytics or custom streaming server.

## Important limits and compatibility

- Android uses **Media3 ExoPlayer first**, then **VideoLAN LibVLC fallback**; not all codecs or transport protocols work. HLS is supported; geo-blocked and dead streams can still fail.
- Program guide (EPG) and remote channel logos are **future Android work**; initial focus is stable basic TV playback and updating.
- Android never silently installs APKs. The user must approve installation and may need to allow installs from NODE TV.
- App updates require **a stable release signing key** and an incremented versionCode on each release; temporary Actions debug APKs cannot update one another reliably.
- Sources of streams are public links with varied availability. Use authorized stream sources, and retain third-party acknowledgments.

## Building without Android Studio

1. Create public GitHub repository `nodedevfivem/NODE-TV-Android`.
2. Upload this project's contents to repository root, including `.github/workflows/build-apk.yml`.
3. **Actions → NODE TV Android APK → Run workflow** (or push to main).
4. After successful Actions build, download the **individual** signed APK/checksum artifacts, or the `NODE-TV-ALL-RELEASE-FILES` bundle.
5. Without signing secrets, output is **NODE-TV-TEST-ONLY.apk** for device testing; do not publish it.
6. For a real distributable and future updates, configure signing secrets; see `START-HERE-GITHUB-APK-AND-UPDATES.md`.

The Android GitHub repository must match `UpdateClient.java` repository constants. GitHub APK file naming matters to the updater.

v0.1.5 is a SOURCE-ONLY fix and has NOT been compiled or phone-tested in this environment. User reports v0.1.4 produces audio-only black video after exiting fullscreen; the v0.1.5 stable root-level video surface removes reparenting that can trigger this. Run GitHub Actions and test phone playback before publishing.


Latest v0.1.1: dual Media3/LibVLC playback compatibility and faster channel retries. See `START-HERE-v0.1.1-ANDROID-PLAYBACK.md`.

Latest **v0.1.2**: fullscreen left brightness/right media-volume vertical swipes, center tap pause/play, native system Back to portrait, and GitHub Release banner/mandatory update marker. See `START-HERE-v0.1.2-FULLSCREEN-GESTURES-AND-GITHUB-UPDATES.md`. The user has since provided phone screenshots of v0.1.2 playing VLC channels, but with a resizing bug.

Latest **v0.1.3**: VLC automatic video-fit/centering and viewport synchronization on portrait/fullscreen moves. Official signed installer is now **`NODE-TV.apk`**, with a versioned compatibility copy for older clients. See `START-HERE-v0.1.3-VIDEO-SCALING-AND-APK-NAMING.md`. The new fix still needs GitHub build + phone testing.

Latest **v0.1.4**: persistent mandatory GitHub Release update gate and an on-screen EXIT FULLSCREEN button revealed by tapping landscape video; middle tap still pauses/resumes. The official signed APK is still `NODE-TV.apk`. See `START-HERE-v0.1.4-MANDATORY-UPDATES-EXIT-CONTROL.md` and `GITHUB-RELEASE-v0.1.4.txt`.

Latest **v0.1.5**: critical portrait black-video/audio-only fix after fullscreen exit. `videoSlot` now stays under the root parent from app start to app shutdown; fullscreen/portrait only change bounds, rather than detaching VLC's TextureView. GitHub Actions now explicitly surfaces all **three official signed release assets as individually downloadable artifacts**, plus a release ZIP containing all three. Without permanent signing secrets, ONLY TEST APKs will be produced; this is intentional. See `START-HERE-v0.1.5-VIDEO-SURFACE-AND-ARTIFACT-FIX.md`.
