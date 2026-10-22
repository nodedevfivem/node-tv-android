# NODE TV Android v0.1.0 — APK Build Hotfix 1

## Why this hotfix exists

The initial GitHub Actions run #39759355924 was marked **Failure** after 43 seconds with no APK. The summary screenshot shows Node.js 20 and setup-java v4 **deprecation warnings**; it does not show the underlying error from the job's first failed step. Therefore, do not treat those warnings as the proven root cause.

The initial source also combined Android Gradle Plugin 8.9.2 (max API 35) and compileSdk 35 with a Media3 1.11.1 dependency that may require a newer compileSdk. This patch aligns the Android build toolchain to API 36:

- `build.gradle`: Android Gradle Plugin **8.11.1** (supports API 36)
- `app/build.gradle`: `compileSdk 36` (targetSdk remains 35, minSdk remains 26, version remains 0.1.0)
- `.github/workflows/build-apk.yml`: GitHub Actions updated for Node 24 (checkout v6, setup-java v6, Gradle setup v6, setup-android v4, upload-artifact v7), Gradle **8.13**, Android SDK **36**, and actionable compiler diagnostics.

No NODE TV Java application code, app ID, versionCode, assets, settings, favorites, or updater code was modified.

## GitHub website: apply the 3-file hotfix

1. Open `https://github.com/nodedevfivem/node-tv-android` and sign in.
2. Extract the **UPDATE ONLY** ZIP locally.
3. Replace these **three** repository files with their patched copies, keeping the same paths:
   - `.github/workflows/build-apk.yml`
   - `build.gradle`
   - `app/build.gradle`
4. You can either Git-upload the three files preserving the exact folder paths, or open each file in GitHub > pencil (**Edit**) > replace its contents > **Commit changes**. Do not place `build-apk.yml` in the repository root; it must be inside `.github/workflows/`.
5. Open **Actions > NODE TV Android APK**. The commit should trigger a new build; otherwise click **Run workflow** on `main`.
6. If the green build succeeds, open its **Artifacts > NODE-TV-Android-APK**, download the zip, extract and install the **DEBUG-TEST-ONLY** APK on your Android phone.
7. If the build fails, click the failing run > **build > first red step**. This hotfix includes a build-log artifact and GitHub summary if Gradle actually compiled, making a subsequent exact fix possible.

## Important

This is a **source-only, uncompiled hotfix**. It has not been executed against a GitHub runner or Android device. The debug APK has a temporary signing identity. Do not publish it as an official update; first set up and safeguard a stable Android release signing key.
