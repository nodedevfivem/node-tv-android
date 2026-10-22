> **v0.1.3 CURRENT APK NAMING:** Official signed installer is `NODE-TV.apk`, plus identical `NODE-TV-Android-v0.1.3.apk` for v0.1.2 client compatibility and `NODE-TV-Android-v0.1.3-SHA256.txt` for both checksums. Old naming below is historical; see `START-HERE-v0.1.3-VIDEO-SCALING-AND-APK-NAMING.md`. Do not publish `NODE-TV-TEST-ONLY.apk`.

# NODE TV Android v0.1.0 — Build and automatic updates without Android Studio

## Overview

This is a dedicated Android project (Java + AndroidX Media3). It does **not** depend on the Windows NODE TV app to run, but uses the official artwork and concept. It is a **source-only candidate, not a compiled APK**. Compile with GitHub Actions, install the resulting APK, and report build or playback errors for further refinement.

The updater is configured for:

- Owner: `nodedevfivem`
- Repository: `NODE-TV-Android`
- Latest release API: `https://api.github.com/repos/nodedevfivem/NODE-TV-Android/releases/latest`
- Current Android version: `0.1.0` / code `1`
- Minimum Android: 8.0 (API 26), 64-bit/32-bit device support depends on Android's Media3 decoders.

The APK file name is **not optional** for updates: `NODE-TV-Android-v0.1.0.apk`, and the checksum file name is `NODE-TV-Android-v0.1.0-SHA256.txt`. These are produced by the GitHub Actions workflow once release-signing secrets are configured.

## 1. Create repo

1. In GitHub create **Public** repository named `NODE-TV-Android` under `nodedevfivem`.
2. Extract this FULL SOURCE ZIP to your computer. Upload the **contents** to the repository root (not the ZIP itself), including `.github/workflows/build-apk.yml` and `app/`.
3. The first push to `main` may start a build automatically. Alternatively open **Actions > NODE TV Android APK > Run workflow**.
4. Click the finished workflow run and download artifact **NODE-TV-Android-APK**.

## 2. Testing before release signing

If no signing secrets exist, Actions builds `NODE-TV-Android-v0.1.0-DEBUG-TEST-ONLY.apk`.

- This is for testing on your own phone only. Download and install it on Android.
- It may trigger an Android warning about installing from unknown sources; approve the action only if you trust the APK from your repository.
- Debug keys are created anew by clean CI runners. An APK signed by one runner may fail to update a version signed on another runner.
- **Never publish the debug APK as the official release**. Users will not receive reliable in-place updates.
- Switching from debug to release signing generally requires uninstalling the debug app first. An uninstall deletes the locally saved favorites.

## 3. Configure a permanent Android release signing identity

Create the key on your own computer using a JDK's `keytool` (JDK 17+). Windows PowerShell example (prompts for a password):

```powershell
keytool -genkeypair -v -keystore "NODE-TV-Android-Release.jks" -storetype PKCS12 -alias nodetv -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=NODE Development"
```

Remember the password and save the keystore in a **private, offline backup**. Never post its bytes, password, or base64 representation into chat or a public repository. **You must retain the same key for every future release** or Android will reject an in-place APK update.

Encode privately in PowerShell (copies base64 to the Windows clipboard, so keep the clipboard private and clear it after use):

```powershell
[Convert]::ToBase64String([System.IO.File]::ReadAllBytes(".\NODE-TV-Android-Release.jks")) | Set-Clipboard
```

At GitHub repository **Settings > Secrets and variables > Actions > New repository secret**, configure these exact names:

| Name | Value |
| --- | --- |
| `NODETV_KEYSTORE_B64` | Base64 encoding of the private keystore (clipboard) |
| `NODETV_STORE_PASSWORD` | Keystore password |
| `NODETV_KEY_ALIAS` | `nodetv` |
| `NODETV_KEY_PASSWORD` | Private key password (normally the same as keystore password for PKCS12) |

Do not commit or upload the keystore file. `.gitignore` excludes `*.jks`.

Then re-run **Actions > NODE TV Android APK > Run workflow**. If all secrets are configured, the workflow builds a release-signed `NODE-TV-Android-v0.1.0.apk` **and** `NODE-TV-Android-v0.1.0-SHA256.txt`.

Download the Actions artifact containing those files, test the signed APK on your phone, and only then publish it.

## 4. Publish official v0.1.0 on GitHub Releases

1. Go to the Android repository's **Releases > Draft a new release**.
2. **Tag**: `v0.1.0`.
3. **Title**: `NODE TV Android v0.1.0 — First Mobile Release`.
4. Use the text in `GITHUB-RELEASE-v0.1.0.txt` for the release description.
5. Upload exactly `NODE-TV-Android-v0.1.0.apk` and `NODE-TV-Android-v0.1.0-SHA256.txt`, obtained from the **signed release** workflow, not the debug one.
6. Publish as a normal, non-prerelease release. This initial release makes the download public.

## 5. How the Android updater works

- The app checks the public GitHub latest release at startup and around every six hours while open, or from the `Updates` button.
- For a **newer** version, a visible banner offers **UPDATE NOW**, **LATER** (24-hour snooze), and **DETAILS**.
- UPDATE NOW downloads the GitHub-hosted APK, checks its declared file size and published SHA-256, then opens Android's native package installer.
- Android **requires user approval**. The OS may request "Allow from this source" (NODE TV) once before installation.
- Android checks the APK's package name, signing certificate, and version code; this cannot be overridden.
- No server, password or account is needed to look up releases. No silent update occurs.

**First live updater test:** after installing official v0.1.0, create Android version **0.1.1** by changing BOTH `versionCode` from `1` to `2` and `versionName` from `0.1.0` to `0.1.1` in `app/build.gradle`, plus the `VERSION` constant in `MainActivity.java` to `0.1.1`. Rebuild with the **same keystore**; publish `v0.1.1` with `NODE-TV-Android-v0.1.1.apk` and `NODE-TV-Android-v0.1.1-SHA256.txt`. Version 0.1.0 will then offer the new version.

## Compatibility / limitations

- The Windows version uses LibVLC; Android v0.1.0 uses the official Media3/ExoPlayer HLS engine. Some stream codecs unsupported on a phone may fail even when they work in VLC.
- The first Android version includes no EPG or channel-remote-logo fetching; those can be added after basic playback is verified.
- GitHub API / download URLs require internet access and a publicly available release.
- Updates do not work across differently signed APKs (for example, debug -> production release).
- App has no remote account sync; uninstalling deletes local preferences unless you export them separately in future versions.
- Android's permission prompt and package installer screens are under Android's control; they can't be fully custom-skinned like the Windows NSIS installer.
- Retain the legal attributions in `THIRD-PARTY-NOTICES.md` even though the interface labels just say Worldwide / Philippines.

## Troubleshooting

- If the workflow fails, copy the **first actual compiler error** from the Android GitHub Actions job logs. Don't rely on just the final `BUILD FAILED` line.
- If it says missing signing secrets, either add all four or run the debug build without any; a partial set is treated as debug only.
- If APK won't update, confirm identical `applicationId`, same signing certificate and **strictly higher** `versionCode`.
- If the release isn't seen, verify it is publicly published, not marked a prerelease, has a valid tag (`v0.1.1`), and uses the exact expected APK/checksum asset names.
- If a live station cannot play, try a different stream (availability and device codec support vary).

## Verification status

Initial Java/XML and source archive checks can be performed offline. No Gradle/Android SDK is preinstalled in the creating environment, so compilation, streaming, and real APK update installs are **not yet validated**.
