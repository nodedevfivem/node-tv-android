# NODE TV Android v0.1.1 — GitHub Actions compiler hotfix #2

The failed GitHub Actions run `39763480559` points to `MainActivity.java` line 153: `cannot find symbol`.

## Exact correction

Media3 `PlaybackException` exposes the **method** `getErrorCodeName()`, not a public `errorCodeName` field.

**Old:** `error.errorCodeName`

**New:** `error.getErrorCodeName()`

## Apply on GitHub

1. Open `app/src/main/java/net/nodedevelopment/nodetv/MainActivity.java` in `nodedevfivem/node-tv-android`.
2. Click the pencil icon and replace line 153 with:

   `Log.w("NodeTV", "Media3 playback error: "+error.getErrorCodeName());`

3. Commit to `main`. Existing GitHub Actions workflow runs automatically on push.

Or extract UPDATE ONLY ZIP and replace the corresponding file in GitHub, preserving the path. The update archive also has this note.

## What changed

Only `MainActivity.java` changes in compiled application source. No player logic, LibVLC fallback, app ID, signing, version number, assets, or update notification changes. Keep `versionName` at `0.1.1` and `versionCode` at `2`.

## Verification

This exact API name is documented in AndroidX Media3 `PlaybackException`: `public final String getErrorCodeName()`. Local text inspection and ZIP-integrity checks pass. GitHub Actions and phone behavior still need user verification.
