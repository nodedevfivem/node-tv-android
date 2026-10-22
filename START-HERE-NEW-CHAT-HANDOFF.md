# CURRENT CANONICAL SOURCE — NODE TV Android v0.1.6 (versionCode 7)

Based directly on the COMPLETE NODE TV Android **v0.1.5** full-source ZIP. Read `START-HERE-v0.1.6-FLOATING-CHANNEL-BROWSER.md`. This is SOURCE ONLY: Android compilation, GitHub Actions, phone playback, and update installation are NOT yet verified. Preserve the v0.1.5 root-owned video surface fix and dual engine (Media3 ExoPlayer then LibVLC). The floating translucent Browse Channels sheet must not reparent the live video view or interrupt playback. The official signed APK remains `NODE-TV.apk` plus versioned `NODE-TV-Android-v0.1.6.apk` and `NODE-TV-Android-v0.1.6-SHA256.txt`.

---

# Current milestone: NODE TV Android v0.1.5 source-only candidate

The latest canonical source in this archive is **v0.1.5 (versionCode 6)**, based on v0.1.4, with a stable root-owned videoSlot instead of reparenting the live TextureView on fullscreen/portrait transitions. Fixes reported audio-only black picture after EXIT FULLSCREEN. GitHub Actions uploads 3 individually downloadable signed release files, plus bundle, only when the four permanent signing secrets are configured. Read `START-HERE-v0.1.5-VIDEO-SURFACE-AND-ARTIFACT-FIX.md` before any GitHub Release. Compiling and phone behavior are UNVERIFIED. Older version sections below are history.

> **CURRENT SOURCE v0.1.4 (2026-10-22).** This old handoff records the original v0.1.1 source for provenance only. Current canonical source is v0.1.4 / versionCode 5 (derived from the complete v0.1.3 archive). See `START-HERE-v0.1.4-MANDATORY-UPDATES-EXIT-CONTROL.md` for latest GitHub mandatory-update flow, fullscreen exit button and honest unverified build status.

> **CURRENT SOURCE v0.1.3** — This is the historical v0.1.1 handoff retained for provenance. The canonical source in this ZIP is v0.1.3 (versionCode 4), based on the user-tested v0.1.2 source. See `START-HERE-v0.1.3-VIDEO-SCALING-AND-APK-NAMING.md` for changes, official NODE-TV.apk naming, build/release instructions and unverified v0.1.3 status.

> **HISTORICAL HANDOFF — v0.1.1 hotfix #2.** This document is retained unchanged in substance as provenance from the uploaded canonical v0.1.1 source. The CURRENT source in this ZIP is **v0.1.2**, based on that original v0.1.1. Read `START-HERE-v0.1.2-FULLSCREEN-GESTURES-AND-GITHUB-UPDATES.md` for current code changes, release details and build/test status.

# NODE TV ANDROID — NEW CHAT HANDOFF (canonical source: v0.1.1 hotfix #2)

This ZIP is the COMPLETE ANDROID DEVELOPMENT SOURCE, not a compiled APK. Every original file from `NODE-TV-Android-v0.1.1-CS-COMPILER-HOTFIX2-FULL-SOURCE.zip` is preserved without modification. This handoff documentation is the only added content.

## What is verified, and what is not

- **Last user-confirmed phone build**: Android v0.1.0 test APK compiled and ran, but channels playable on NODE TV Windows frequently showed NO RECEPTION.
- **Latest source to continue from**: Android v0.1.1 `versionCode=2`; adds Media3 first, LibVLC 3.7.5 fallback on playback error/timeouts; proper M3U stream options; cleaner switching and retry.
- **Last observed GitHub run**: 39763480559 failed; compiler fix #2 replaces `error.errorCodeName` with `error.getErrorCodeName()` in MainActivity.java. This corrected build has not been shown green and no phone test of v0.1.1 is verified. Source structure/ZIP checks are not a substitute for an APK build.

## Project and architecture

- GitHub repository: `https://github.com/nodedevfivem/node-tv-android` (public; GitHub Actions CI build).
- Source: Java Android `app/src/main/java/net/nodedevelopment/nodetv/`, minSdk 26, targetSdk 35, compileSdk 36, Gradle 8.13/Android Gradle Plugin 8.11.1, Java 17, package `net.nodedevelopment.nodetv`.
- Dual playback engines: Media3 ExoPlayer (`MainActivity.java`) then `VlcFallbackPlayer.java` powered by `org.videolan.android:libvlc-all:3.7.5`.
- Core files: `PlaylistRepository.java`, `Channel.java`, `UserStore.java`, `RetroNoiseView.java`, `StaticAudio.java`, `UpdateClient.java`.
- Official assets: `app/src/main/res/drawable/node_tv_logo.png`, `retro_frame.png`, icon mipmap variants.
- GitHub Actions build: `.github/workflows/build-apk.yml` (debug test without signing secrets; official release with stable signing secrets).

## Signing / updates warning

- Keep the same permanent private release keystore and applicationId across official versions; monotonically increase versionCode.
- Never upload signing keys or passwords to a public GitHub repository, handoff ZIP or release. Use GitHub Actions Secrets `NODETV_KEYSTORE_B64`, `NODETV_STORE_PASSWORD`, `NODETV_KEY_ALIAS`, `NODETV_KEY_PASSWORD` and preserve original private keystore securely.
- Debug-TEST-ONLY APK artifacts may use incompatible ephemeral debug certs, requiring uninstall; they should not be described as reliable in-place auto-updates.
- Official release assets expected: `NODE-TV-Android-v0.1.1.apk` (signed) and checksum. Android's package installer confirmation is required.

## Next tasks

1. Ensure the source in GitHub matches THIS ZIP (especially `MainActivity.java` hotfix + new `VlcFallbackPlayer.java` and Gradle dependency) and run GitHub Actions.
2. If build fails inspect earliest actual compiler error, not only generic Gradle stack trace. Supply modified files and full source as separate ZIPs for each fix.
3. Test target streams side-by-side with NODE TV Windows. Do not promise geo-blocked, DRM or offline streams will play on Android.
4. Configure permanent private release signing when debug tests pass, then test actual GitHub Releases update from one signed APK to newer signed APK.
5. Future releases: full source, update-only archive, paste-ready tag/title/full release description code block, and accurate validation status.

## Copy-paste prompt

We are CONTINUING development of my NODE TV ANDROID application by NODE Development. I am uploading the COMPLETE FULL-SOURCE ZIP. Treat THIS ZIP as the single canonical development base; DO NOT revert to v0.1.0 or start a new project.

CURRENT SOURCE: NODE TV Android v0.1.1, including the latest MainActivity.java Media3 getErrorCodeName() COMPILER HOTFIX #2. Android v0.1.0 was successfully built/tested on a real phone, but many streams that worked on NODE TV Windows showed NO RECEPTION on Android. v0.1.1 adds auto fallback from Media3 ExoPlayer to VideoLAN LibVLC for improved compatibility and channel switching. The first v0.1.1 GitHub Actions build failed with a Java 'cannot find symbol'; the hotfix addresses that specific line, but the revised v0.1.1 build and playback are NOT YET user-confirmed working. Do not falsely claim tested results.

PUBLIC ANDROID GITHUB REPO: nodedevfivem/node-tv-android
TECH: Java Android app, Media3 ExoPlayer primary + LibVLC fallback, minSdk 26, AndroidX, Gradle / GitHub Actions APK builder. Package ID: net.nodedevelopment.nodetv. Current versionName 0.1.1 / versionCode 2. I prefer NO Android Studio; build APKs using GitHub Actions and test APKs on my phone. Keep the workflow .github/workflows/build-apk.yml.

PRESERVE ALL FUNCTIONALITY: official NODE TV gold logo and retro wooden TV identity; Worldwide and Philippines live channel playlists; categories/search; Favorite TV Channels; Recently Watched; eight themes; landscape/fullscreen video-only mode; CRT snow/static and sound for truly unavailable streams; channel resolution info; GitHub Releases NEW UPDATE banner with UPDATE NOW / LATER / DETAILS; saved preferences. Never revert the dual-engine fallback or treat no reception prematurely. Some channels may genuinely be offline, geoblocked, DRM protected or unsupported by Android device hardware.

NEXT PRIORITY: validate the new v0.1.1 build through GitHub Actions; fix any ACTUAL compiler errors from build logs, keeping version 0.1.1 until ready for feature bump. Then test channel playback on Android versus Windows and improve channel switching, error recovery, headers and decoder compatibility where appropriate. Do not guess failure reasons from generic Java stack trace alone; get first relevant compiler error. Use update-only and full-source ZIPs for changes.

ANDROID UPDATE/SIGNING POLICY: Official APK auto-updates must keep a stable private signing key/certificate, unchanged applicationId and increasing versionCode. GitHub Actions debug artifacts signed with transient debug keys are TEST ONLY and should not be advertised as reliable in-place official updates. NEVER put keystores, key passwords or base64 secrets into public GitHub commits or chat responses. Configure GitHub Actions secrets privately to make release-signed APK; the app can download and verify official APK but Android must show system installation confirmation. Release needs correct tag, versioned APK name and checksum; no silent install.

FOR ALL FUTURE ANDROID UPDATES: give me (1) COMPLETE FULL-SOURCE ZIP and (2) MODIFIED/NEW FILES ONLY UPDATE-ONLY ZIP; instructions for uploading changed files to GitHub; (3) copyable release tag, title and FULL release description in ONE CODE BLOCK; status of GitHub build and whether the APK is user-tested. Do not claim Android Studio builds or device tests unless truly done. Keep NODE TV Windows a SEPARATE project; do not confuse their files or versioning.

Read START-HERE-NEW-CHAT-HANDOFF.md and existing project docs from the attached ZIP first. Before modifying code inspect the uploaded complete source and tell me you understand the project, its current version and build verification status. Then proceed with my next request.
