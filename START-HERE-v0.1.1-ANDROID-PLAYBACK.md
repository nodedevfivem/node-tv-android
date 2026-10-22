# NODE TV Android v0.1.1 — IPTV Playback Compatibility

This is a **source-only, uncompiled candidate** built from the working v0.1.0 Android GitHub workflow baseline.

## Changes
- Keep Android Media3 ExoPlayer for quick normal channel playback; when it errors or cannot start within 11 seconds, try **LibVLC 3.7.5** as a compatibility engine.
- LibVLC native codecs/transports align more closely with NODE TV Windows' VLC playback.
- Animated CRT NO RECEPTION only appears after both engines fail (VLC fallback times out after an additional 24 seconds).
- Better stream user-agent handling, redirects, HLS `m3u8` recognition, optional `#EXTVLCOPT` / `#EXTHTTP` referrer and user-agent metadata.
- Recognizes safe-listed IPTV network transport schemes (including RTSP/RTMP) and sends them straight to LibVLC rather than asking Media3 to decode them. Actual support depends on the stream and the underlying native engine.
- One-tap **Retry** button and reliable cleanup when rapidly switching stations.
- Playback controls/volume/mute and fullscreen work with whichever engine is active.
- Increased Android version to **0.1.1 (versionCode 2)**; same package identity, themes, favorites, logo, and GitHub updater.

## GitHub steps
1. Preserve your known-good repository; upload the UPDATE ONLY files, retaining relative directory structure (the new `VlcFallbackPlayer.java` must be inside `app/src/main/java/net/nodedevelopment/nodetv/`).
2. Commit the changed/new files to `main`; the workflow should build the APK. Because LibVLC contains native binaries, the APK artifact will be significantly larger.
3. Download `NODE-TV-Android-v0.1.1-DEBUG-TEST-ONLY.apk` and install on your phone. If Android reports a signature mismatch with v0.1.0, it is because separate debug builds may use different debug signing certificates. Back up needed local favorites; uninstalling can clear local data. For seamless future updates set up permanent release signing.
4. Test channels that work in Windows but not in Android, as well as a geo-blocked channel (which may still fail).
5. If a GitHub Actions build fails, open its first failed step and provide the detailed compiler messages.

## Limitations / transparency
- Even with LibVLC some sources may be offline, region-restricted, require credentials/DRM, or be incompatible with Android hardware. We do not bypass restrictions.
- An 11-second primary-engine timeout plus 24-second VLC attempt can still be required for slow stations.
- Do not publish an unsigned/debug APK as an official release; keep the same permanent signing key for installed-app updates.
- GitHub Actions and on-device playback have **not** been executed within this environment.
- VideoLAN third-party license terms apply to the included LibVLC binaries.
