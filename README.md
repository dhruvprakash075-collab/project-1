# OpenFiles

A privacy-first, offline-first Android file manager. No internet permission until the (future)
Ring 2 cloud feature; browsing, viewing, and organizing files never leaves the device.

## Modules

- `:app` — application shell, navigation, settings, trash.
- `:core:common` — shared models (`FileItem`, `Route`) and the `UiState` state machine.
- `:core:data` — repositories: filesystem (NIO2 + SAF), MediaStore gallery, Room (recents/trash),
  DataStore settings, Apache POI (Excel/Word/Slides, read-only), archives (zip4j / Commons Compress),
  Samba cloud browsing (smbj), BLAKE3 hashing (Rust via uniffi), ML Kit GenAI tagging, duplicate
  detection, document search annotations.
- `:core:ui` — Material 3 theme (Light/Dark/True Black/System), shared Loading/Empty/Error
  composables, storage permission helpers, and the MIME-based `FileOpener` router.
- `:feature:browser` — the file browser (list, multi-select, create/rename/copy/move/delete),
  breadcrumbs, sort options, in-folder search.
- `:feature:gallery` — photo/video grid backed by MediaStore.
- `:feature:viewer` — PDF (native `PdfRenderer`), image (Coil), video/audio (Media3), Office
  documents (Apache POI, read-only), plain text, and archive browsing.

## Stack (locked versions — see `gradle/libs.versions.toml`)

Kotlin 2.4.0 · AGP 9.2.1 · Gradle 9.4.1+ · JDK 17 · compileSdk 36 · targetSdk 35 · minSdk 26 ·
Compose BOM 2026.06.00 · Room 2.8.4 · KSP 2.3.9 · Hilt 2.60 · Coil 3.5.0 · Media3 1.10.1 ·
zip4j 2.11.6 · ACRA 5.13.1 · Navigation 3 1.1.4 · JNA 5.16.0 · coroutines-guava ·
smbj 0.14.0 · ML Kit GenAI 1.0.0-beta1.

## Build

```bash
./gradlew assembleDebug     # debug APK
./gradlew lintDebug testDebugUnitTest assembleDebug   # what CI runs
```

Requires Android SDK (compileSdk 36) and JDK 17 installed locally. Ring 3 features include Rust
native libraries (built via `cargo-ndk` + uniffi) — prebuilt `.so` files are vendored in
`core/data/src/main/jniLibs/` so no local NDK build is needed for development.

## License

GPL-3.0-or-later. See `LICENSE`.

## Architecture notes

- **Permissions**: defaults to Storage Access Framework; "All files access" (`MANAGE_EXTERNAL_STORAGE`)
  is requested only for full file-manager browsing, matching Play's approved use case for file managers.
- **PDF**: zero-dependency native `PdfRenderer`, one page rendered at a time — memory use never
  scales with document length.
- **Office documents**: Apache POI is intentionally **read-only** in v1 (no in-app editing) to avoid
  round-tripping corruption; this is a locked decision, not a stub.
- **POI dependency**: `third_party/poishadow/poishadow-all-5.2.5-4.jar` is vendored from the official
  `centic9/poi-on-android` GitHub release because the release asset is not published as a Maven artifact.
  This jar uses Java method handles, so the app minSdk is 26.
- **Open-file routing**: `FileOpener` maps MIME types to an in-app viewer or a neutral system
  chooser — it never names or favors a specific third-party app.
- **Trash**: soft-delete only; items move to an app-private trash folder and are restored or purged
  explicitly (no silent auto-expiry).
- **Native hashing**: BLAKE3 via Rust crate (`native/rust-core`) + uniffi FFI. Prebuilt `.so` files
  vendored in `core/data/src/main/jniLibs/`. Falls back to Kotlin SHA-256 if native unavailable.
- **On-device AI**: ML Kit GenAI for image description + text summarization. Narrow device support
  (Pixel/Samsung S24+/vivo X200+ only). Ships dark until UI entry points are wired.

## Status

All phases from the build plan (browser, gallery, PDF, Office viewers, archives, text viewer,
settings, trash) are scaffolded with real implementations, not placeholders. Ring 3 features are
in progress:

- **F1** — BLAKE3 file hashing core (Rust via uniffi + cargo-ndk, Kotlin SHA-256 fallback) ✅
- **F2** — Duplicate finder (uses F1 for whole-file hashing) ✅
- **F5** — On-device AI tagging/summarization (ML Kit GenAI, ships dark until UI wired) ✅
- **F4** — Document search annotations (in progress)
- **F3** — Shizuku app manager integration (in progress)
- **F6** — LibreOfficeKit (descoped — no embeddable Android artifact available)

CI runs `./gradlew lintDebug testDebugUnitTest assembleDebug` on every feature branch.
