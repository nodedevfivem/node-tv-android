# START HERE — NODE TV Android v0.1.5 fullscreen video and GitHub artifact fix

Source basis: official user-provided v0.1.4 FULL SOURCE ZIP. Development is separate from NODE TV Windows.

## 1. What was wrong with fullscreen Exit

v0.1.4 physically detached `videoSlot` from `retroFrame` and attached it to `fullscreenLayer` on every fullscreen change. `videoSlot` contains a VLC `TextureView`. Android can destroy and recreate its SurfaceTexture when its parent changes, even though LibVLC audio continues to play; the reattached video surface can remain black. Resizing alone does not restore frames to a disconnected native decoder surface. The reported symptom is therefore consistent with an Android surface lifecycle problem.

## 2. How this build addresses it

- `videoSlot` now has a PERMANENT `root` FrameLayout parent, from startup through finish.
- The retro TV frame is still drawn by `mainColumn`. The root-level video slot overlays ONLY the calibrated inner black screen rectangle in portrait.
- In fullscreen the root video slot fills the available window, and the transparent fullscreen gesture/exit HUD is placed above it.
- `layoutVideoSlot()` responds to global layout changes (orientation, status bar/nav insets, update banner) and updates `LayoutParams` only when rectangle size or position actually changes, preventing relayout loops.
- VLC receives updated view dimensions but its TextureView is NEVER detached by fullscreen transitions.
- ExoPlayer/VLC playback engines, channel state, pause/play, brightness/volume and GitHub updater remain unchanged.

## 3. Why you previously saw only ONE APK artifact

GitHub **Actions artifact entries are containers**, so a single entry (`NODE-TV-Android-APK`) may actually contain multiple files after download/unzip. HOWEVER, the v0.1.4 workflow also had a second condition: if ANY of the four permanent signing secrets were missing, it built ONE `NODE-TV-TEST-ONLY.apk` rather than the THREE official files, by design. Neither an APK rename nor manually typing checksums turns that test APK into a signed official release.

v0.1.5 workflow has clearly separate downloadable entries for all THREE official files, plus ONE bundle ZIP. **They appear only when permanent signing is configured and `assembleRelease` succeeds**. Otherwise there is a clearly labelled TEST-ONLY APK and test bundle only.

The third required file is **`NODE-TV-Android-v0.1.5-SHA256.txt`**, which is a TEXT file with APK checksums — NOT `.exe`.

### Four private GitHub Actions secrets needed

Go to your repository → Settings → Secrets and variables → Actions. Use **New repository secret** for each:

- `NODETV_KEYSTORE_B64` — Base64 representation of your PRIVATE permanent signing keystore.
- `NODETV_STORE_PASSWORD` — Keystore password.
- `NODETV_KEY_ALIAS` — Key alias in that keystore.
- `NODETV_KEY_PASSWORD` — Key password.

Keep your existing **official** signing certificate; changing certificates prevents in-place updates. Never put these secrets or the keystore into the GitHub repository, this chat, or a ZIP shared publicly. If you have no permanent certificate yet, create a new key with local JDK `keytool` and secure offline backups; understand that already installed debug-signed builds will NOT update over the newly signed release. See `START-HERE-GITHUB-APK-AND-UPDATES.md` for the private PowerShell keystore creation example.

## 4. Build APK without Android Studio

1. Extract `NODE-TV-Android-v0.1.5-UPDATE-ONLY.zip` into your existing GitHub source root, preserving `.github/workflows/build-apk.yml`, `app/src/main/java/...`, `app/build.gradle`, etc.
2. Commit to the `main` branch of `nodedevfivem/node-tv-android` (the app checks that repository).
3. Open **Actions → NODE TV Android APK → Run workflow** and open the most recent successful run.
4. If release signing is correct, at the BOTTOM of the workflow page under Artifacts you will see:
   - `NODE-TV.apk` — download this to install.
   - `NODE-TV-Android-v0.1.5.apk` — duplicate binary needed by older update clients.
   - `NODE-TV-Android-v0.1.5-SHA256.txt` — checksums, not app.
   - `NODE-TV-ALL-RELEASE-FILES` — ZIP containing all three, for convenient Release attachment.
5. Otherwise, look for `NODE-TV-TEST-ONLY.apk` and `NODE-TV-TEST-ONLY-BUNDLE`; this means at least one signing secret is missing. The single test APK is normal and MUST NOT be published as an official update.
6. When an OFFICIAL signed build and physical phone test succeed, go to **Releases → Draft a new release** with tag `v0.1.5`, paste `GITHUB-RELEASE-v0.1.5.txt`, attach the THREE official files. Publish as normal (NOT prerelease).

## 5. Testing and safety

This release is NOT confirmed built or fixed on phone yet. After test installation, tune a VLC-backed station, enter fullscreen, press EXIT FULLSCREEN, confirm picture AND audio in portrait. Repeat at least 5 times. Test ExoPlayer and VLC, other aspect-ratio formats and recovery. Test older signed app detecting the NEW published release; signed apps need the same publisher key to update. For a TEST-only APK you might have to uninstall prior differently signed builds first, which loses app-local favorites/settings.

## 6. Exact release files (ONLY for signed builds)

`NODE-TV.apk`

`NODE-TV-Android-v0.1.5.apk`

`NODE-TV-Android-v0.1.5-SHA256.txt`

## Version and status

- versionName: 0.1.5
- versionCode: 6
- applicationId: net.nodedevelopment.nodetv
- Code/packaging: DONE
- Actual GitHub Actions compile: NOT performed here
- Physical Android TV/fullscreen behavior: NOT verified here
- Official APK signing and release publishing: requires your existing private signing secrets and successful CI run
