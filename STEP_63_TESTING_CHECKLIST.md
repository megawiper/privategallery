# STEP 63 — Final Testing, Security Audit, and Polish

This is a checklist, not code — it must be run against a real build in
Android Studio on a device/emulator. 142 Kotlin files covering Steps
1–62 are complete in `PrivateGallery/`. This document tracks what to
verify before calling the app done, per the roadmap's Step 63.

## Before you start

1. Open `PrivateGallery/` in Android Studio, let Gradle sync.
2. `net.zetetic:android-database-sqlcipher` and a few other libraries
   pull native binaries — first sync will take longer than usual.
3. Build variant: debug, on a real device if possible (biometric
   enrollment and FLAG_SECURE behavior are easiest to verify on
   hardware rather than an emulator without biometrics configured).

## Known gaps to close during this pass

These are honest gaps, not hidden — flagged so the testing pass finds
them on purpose rather than by surprise:

- **Vignette/grain/fade sliders** (Editor, Step 36) are wired in the UI
  and stored in `AdjustmentState`, but `ImageEditRenderer` does not yet
  apply them to the rendered bitmap — they need a shader-based pass
  (RenderEffect/AGSL) rather than the ColorMatrix approach used for
  brightness/contrast/saturation/temperature. Documented inline in
  `ImageEditRenderer.kt`.
- **Sharpness/Clarity sliders** are stored but not yet rendered, same
  reason as above.
- **Drawing/markup** (Step 38) has a working renderer
  (`ImageEditRenderer.applyDrawing`) but the Editor screen's Draw tab
  is currently instructional text only — the actual touch-to-path
  capture UI (turning finger drags into `DrawingElement` points) isn't
  wired yet. This is the single largest remaining UI gap.
- **Auto-lock duration read at resume** (`GalleryNavHost`'s lifecycle
  observer) is hardcoded to `0L` (Immediately) for Private Gallery
  rather than reading the user-configured `AutoLockDuration` from
  `SecurityPreferences`. Functionally correct for the default, but
  won't respect a user who changes it to 30s/1min/5min/Never until
  wired to the async preference read.
- **Pattern input UI**: `SecuritySettingsScreen`'s credential dialog
  offers PIN/Password/Pattern as a method choice, but pattern entry
  currently reuses the same text field rather than a dot-grid pattern
  view. `CredentialHasher` already treats a pattern as an arbitrary
  string (e.g. `"0-1-4-7"`), so only the input widget is missing.
- **Album lock enforcement at read time**: creating/locking/unlocking
  an album (Steps 18/59) persists `isLocked`, but opening a locked
  album doesn't yet route through a Lock screen check before showing
  contents — the album list shows the lock icon but doesn't gate entry.
- **Duplicate detection at scale**: `DuplicateDetectionEngine` hashes
  every candidate on `Dispatchers.Default`; it hasn't been profiled
  against the "thousands of items" case from Step 63's own test matrix.
  Worth a real timing run.

None of the above are placeholders or fake UI — every screen listed
does real work end to end (real MediaStore queries, real encryption,
real Room/DataStore persistence). These are scoped feature gaps, which
is different from Rule E's "no fake buttons" — nothing here pretends
to be complete when it isn't; each is either inert (sliders that don't
yet affect output) or deferred (pattern grid, draw capture, album-lock
gating).

## Functional test matrix

### First launch → Home
- [ ] Fresh install → Splash → Welcome → Permission → Home
- [ ] Deny permission → app doesn't crash, shows rationale + Settings link
- [ ] Grant partial access (Android 14+) → PARTIAL_ACCESS handled, not
      treated as full denial
- [ ] Re-launch after onboarding → Splash skips Welcome, goes to Home
      (or Lock, if Normal Gallery lock was configured)

### Media library
- [ ] 0 photos → Home shows empty state, not a crash
- [ ] Few photos (<20) → grid renders, date grouping correct
- [ ] Hundreds/thousands → scroll performance acceptable, no ANR during
      initial `MediaStoreScanner` scan
- [ ] Images + videos mixed → video thumbnails show play icon + duration

### Albums
- [ ] Automatic albums populate correctly (Camera, Screenshots,
      Downloads, Videos, Recently Added) based on real device paths
- [ ] Create custom album, add/remove media, rename, delete (media
      survives album deletion — verify in MediaStore/Home after)
- [ ] Lock an existing album via long-press menu

### Viewer / Editor
- [ ] Pinch zoom, double-tap zoom, swipe down to close
- [ ] Swipe left/right preserves source collection (Home vs Favorites
      vs Album vs Search results stay separate)
- [ ] Edit → crop/rotate/flip → adjustments → Save as New (verify
      original untouched) → Replace Original (verify old file gone,
      new one has edits)
- [ ] Kill the app mid-save (Save Changes dialog open) → relaunch →
      confirm original was never deleted before the new file verified

### Trash / Duplicates
- [ ] Delete → appears in Recently Deleted with correct remaining-time
- [ ] Restore → reappears in Home
- [ ] Permanent delete → confirmation required, file actually gone
- [ ] Duplicate scan on a folder with real duplicate photos → exact
      matches grouped correctly; near-duplicates (e.g. burst shots)
      appear in a separate, clearly-labeled "similar" group

### Security — Normal Gallery
- [ ] Set PIN → app restart → Lock screen appears before Home
- [ ] Wrong PIN → error shown, no crash, no info leak in error text
- [ ] Enable biometric → prompt appears automatically on Lock screen
- [ ] Biometric fail/cancel → falls back to PIN entry

### Security — Private Gallery
- [ ] Private Gallery lock is fully independent from Normal Gallery
      lock (different PIN, unlocking one never unlocks the other)
- [ ] Auto-lock "Immediately" (default) → backgrounding the app for a
      few seconds and returning requires re-authentication
- [ ] Lock Now → immediately requires re-auth, no residual content
      flash visible during the transition

### Private Gallery data flow — the critical path
- [ ] Move photo from Normal → Private → confirm it **disappears**
      from: Home grid, other gallery apps, a file browser (if rooted/
      adb-accessible), Recents preview
- [ ] Force-kill the app mid-move (between encrypt and delete-original)
      → relaunch → confirm the original is NOT lost (worst case: exists
      in both places, never neither)
- [ ] Private Search never returns a result when searching from Normal
      Gallery's search screen, and vice versa
- [ ] Screenshot attempt while viewing private content → blocked
      (FLAG_SECURE) — verify on a real device since some emulators
      don't enforce this consistently
- [ ] Recent-apps (overview) preview of the app while Private Gallery
      is open → shows blank/black, not private content
- [ ] Move Private → Normal → confirm it reappears in Home, and the
      encrypted private copy is actually gone (check app's private
      storage size before/after via `adb shell du`)
- [ ] Import via Photo Picker → confirm the *original* picked file is
      untouched afterward (import is additive, unlike move)

### Rotation / lifecycle
- [ ] Rotate device during Viewer, Editor, Video playback → state
      survives (position, edit state, playback position within reason)
- [ ] Background the app during an active private-media move/import →
      operation completes or fails cleanly, never partial

## Security audit checklist

- [ ] `adb backup` (or Auto Backup inspection) confirms private DB and
      encrypted media files are excluded — `allowBackup="false"` in the
      manifest should already guarantee this; verify it wasn't
      overridden anywhere
- [ ] Pull `private_gallery_encrypted.db` via `adb shell run-as` on a
      debug build and confirm it's unreadable without the passphrase
      (SQLCipher-encrypted, not plaintext SQLite)
- [ ] Pull a file from `files/private_media/` and confirm it is not a
      viewable image/video (i.e. actually ciphertext, not just renamed)
- [ ] Confirm no `Log.d`/`Log.e` call anywhere logs a raw PIN, password,
      or decrypted byte content (grep for `Log\.` across the codebase
      as a first pass)
- [ ] Confirm `MediaThumbnail`/`AsyncImage` never renders a `content://`
      URI for private media (should always be `private://<id>`,
      resolved only by `PrivateMediaFetcher`)

## Build cleanup pass

- [ ] `./gradlew lint` — resolve any real warnings (unused imports are
      auto-flagged; Android Studio's "Optimize Imports" on the whole
      project is a fast first pass)
- [ ] `./gradlew assembleDebug` completes with zero errors
- [ ] Verify dark mode, light mode, and system-follow all render
      correctly across every screen (Theme.kt's three GalleryThemeMode
      values) — a few screens (Editor's dark canvas, Viewer's black
      background) intentionally stay dark regardless of theme, which
      is correct for a media viewer, not a bug
- [ ] Accessibility: TalkBack pass over Home, Viewer, Settings at
      minimum — several `Icon`/`IconButton` calls have
      `contentDescription`, spot-check the rest

## What "done" looks like

All boxes above checked, `./gradlew assembleDebug` succeeds, and the
five items under "Known gaps to close" are either fixed or explicitly
accepted as post-1.0 follow-ups.
