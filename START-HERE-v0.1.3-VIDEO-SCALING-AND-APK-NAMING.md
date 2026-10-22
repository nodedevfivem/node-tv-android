# NODE TV Android v0.1.3 — VLC Video Scaling + Friendly APK Name

## Canonical version and verification

- **Base**: Uploaded v0.1.2 complete full source ZIP (which derives from the user's original v0.1.1 compiler-hotfix-#2 handoff). All original assets/features preserved.
- **Android ID**: `net.nodedevelopment.nodetv` (unchanged). **versionName** `0.1.3`, **versionCode** `4` (increased from 3).
- **Status**: Source changes and archive integrity validated only. This environment has neither Android SDK/Gradle nor internet to fetch them. NO new v0.1.3 APK compiled or phone-tested. Use GitHub Actions to compile; only install/publish after confirming results.
- **Prior device evidence**: user supplied five genuine v0.1.2 screenshots: VLC played Cinemax/HBO channels, but portrait sometimes cropped/zoomed the image and fullscreen sometimes showed a small lower-left video. They do NOT prove v0.1.3 fixed it; those behaviors are the target of this patch.

## Technical change

- `VlcFallbackPlayer.java`: explicitly reset VLC's aspect ratio to the source's native ratio (`setAspectRatio(null)`) and select automatic fit (`setScale(0f)`), rather than zoom/crop. After the native TextureView is attached, signal its *actual current* drawable dimensions to `IVLCVout.setWindowSize(w,h)`.
- The old empty `onSurfaceTextureSizeChanged` now updates the native window; surface availability and TextureView layout updates also synchronize it, and on VLC's `Playing` event it is refreshed again.
- `MainActivity.java`: listen to the actual retro-TV/fullscreen viewport layout size, and synchronize VLC even if the TextureView's surface ID stays the same. Resync after both moves between fullscreen and portrait. ExoPlayer retains `AspectRatioFrameLayout.RESIZE_MODE_FIT`.
- These code paths do not change channel URLs, decoder fallback logic, media/network headers, gestures, preferences, nor GitHub mandatory-update policy.

## Official APK naming and legacy compatibility

- Official signed APK downloaded by humans: **`NODE-TV.apk`** (the simple filename requested by the developer).
- Existing v0.1.2 apps only recognize the versioned legacy filename `NODE-TV-Android-v0.1.3.apk`; therefore the workflow publishes **both** names with identical release-signed bytes. DO NOT delete the versioned copy until no older clients need it.
- The third release asset `NODE-TV-Android-v0.1.3-SHA256.txt` includes TWO SHA-256 lines, one for each APK filename. It is required by older and newer update checkers. v0.1.3 prefers `NODE-TV.apk` but still recognizes the versioned filename for older release compatibility.
- With permanent signing secrets set, GitHub Actions outputs those three files. Without them it outputs `NODE-TV-TEST-ONLY.apk` plus warning; test builds are not valid GitHub Release assets and cannot reliably replace a release-signed app due to Android signature checks.
- User approval remains mandatory for sideloaded APK installation. Do not provide/expose keystore or passwords.

## Exact files changed/new in v0.1.3

1. `app/src/main/java/net/nodedevelopment/nodetv/VlcFallbackPlayer.java` — VLC sizing/fit
2. `app/src/main/java/net/nodedevelopment/nodetv/MainActivity.java` — viewport synchronization and version constant
3. `app/src/main/java/net/nodedevelopment/nodetv/UpdateClient.java` — recognize friendly official APK name with legacy fallback
4. `app/build.gradle` — versionCode 4, versionName 0.1.3
5. `.github/workflows/build-apk.yml` — signed output `NODE-TV.apk`, compatibility APK/checksum, test-only naming
6. `README.md` — v0.1.3 references and instructions
7. `START-HERE-NEW-CHAT-HANDOFF.md` — current source banner
8. `START-HERE-GITHUB-APK-AND-UPDATES.md` — current filename banner
9. `GITHUB-RELEASE-v0.1.3.txt` — NEW copyable release fields
10. `START-HERE-v0.1.3-VIDEO-SCALING-AND-APK-NAMING.md` — NEW this document
11. `CURRENT-SOURCE-v0.1.3-SHA256.txt` — NEW source integrity manifest

## No Android Studio: GitHub build sequence

1. Unzip `NODE-TV-Android-v0.1.3-UPDATE-ONLY.zip` into the checked-out GitHub repository root while keeping folder hierarchy. Overwrite 8 existing files and add 3 new files (two docs and the source manifest). Keep all other original files unchanged. Commit/push to `main`.
2. Open GitHub repo **Actions → NODE TV Android APK**. Let push trigger the workflow or select **Run workflow**.
3. Download successful Actions artifact named `NODE-TV-Android-APK` and inspect contents.
4. Test install/playback on your Android phone. With no signing secrets, `NODE-TV-TEST-ONLY.apk` is built for local testing ONLY, and its debug certificate may differ from your previous installation.
5. For the **official APK named `NODE-TV.apk`**, use existing four permanent GitHub Actions signing secrets: `NODETV_KEYSTORE_B64`, `NODETV_STORE_PASSWORD`, `NODETV_KEY_ALIAS`, `NODETV_KEY_PASSWORD` (never put their values in commits or releases). Download the signed artifact.
6. Verify that `NODE-TV.apk` and `NODE-TV-Android-v0.1.3.apk` have identical SHA-256 hashes and that the SHA-256 text lists both.
7. **After successful build and testing** create GitHub Release tag `v0.1.3`; title and entire description are in `GITHUB-RELEASE-v0.1.3.txt`. Upload all THREE official signed files. This makes v0.1.3 discoverable by legacy v0.1.2 mandatory-update code.

## Device acceptance checks

- Tune working VLC fallback channels (e.g. channels previously observed with correct picture but incorrect scaling). Verify centered contained image in the CRT bezel.
- Enter fullscreen landscape: image must be centered and FIT the screen as much as native aspect allows, no tiny video in lower left.
- Exit with Android right-edge Back gesture: picture must re-fit the retro TV aperture; no zoom/crop. Repeat 5+ times; retry channel switching afterwards.
- Verify genuine source letterboxing is maintained (4:3 and widescreen) without distortion; provider-embedded black bars cannot be removed without cropping.
- Verify left/right brightness/media volume gestures, center tap pause/play, favorites, search, categories, Philippines channels, themes, CRT noise, update banner.
- Test in-place update from an older officially signed APK to a new release: same signing identity, same package, versionCode increased. Confirm the old v0.1.2 app sees v0.1.3 when both versioned compatibility files are attached.

## Current limits

- This is a targeted source-level fix, not proof of success on particular GPU/VLC renderer. Android devices/codecs may vary; only phone testing can confirm.
- A permanently release-signed installer cannot be produced locally without the private signing keystore; the existing GitHub Actions workflow handles this privately.
- Do not publish a mandatory release until the update download + install path is verified. You may remove the hidden mandatory marker from the proposed description for initial test/optional rollout.
