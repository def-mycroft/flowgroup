# Android App — Agent Summary

This repo is a compact, test‑oriented Android app. It captures heterogeneous inputs into normalized Envelopes, persists them idempotently, and emits structured telemetry (Receipts/Spans). It uses Room, Jetpack Compose, WorkManager, Coroutines, and OkHttp. The code favors seams so agents can instrument, test, and patch safely.

## Modules
- app: Android application (UI, repository, Room, telemetry, Google Drive integration).
- core: JVM library with pure interfaces and fakes (e.g., `app.zero.core.cloud.DriveAdapter`). No Android deps.
- vaultlogsurface: JVM experiments for vault mirroring/log surfacing (not wired by default).
- src: Integration sketches and examples.

## Core Concepts
- Envelope: content record (sha256, mime, meta, filename) with idempotent storage.
- Receipt: typed telemetry (ok, code, adapter, tsUtcIso, spanId, …) stored in Room and mirrored to NDJSON.
- Span: timing/lineage record linked to Receipts.
- CloudBinding: local↔cloud binding (envelopeId, driveFileId, md5, bytes, uploadedAtUtc).

## Architecture
- UI (Compose)
  - Screens: Capture, History, Cloud, About. `KernelActivity` hosts; `KernelViewModel` exposes `envelopes`/`receipts` StateFlows.
  - History chip shows Drive connection using a single source of truth (see Cloud/Drive).
- DI / Wiring
  - `AppModule` provides `KernelDatabase`, `ReceiptEmitter`, `ErrorEmitter`, `EnvelopeChainer`, `KernelRepositoryImpl`.
  - `ServiceLocator` exposes `repository(context)` and `receiptEmitter(context)` singletons.
- Repository
  - `KernelRepositoryImpl`: span → build envelope → idempotent insert → emit receipt → chain/export.
  - Save surfaces: share, camera, mic, files, location, sensors, SMS in/out.
- Telemetry
  - `ReceiptEmitter.emitV2` writes Room + NDJSON via `CanonicalReceiptJson`; `ErrorEmitter` maps exceptions to codes.
  - `TelemetryCode` provides compact codes for success/errors.
- Background Work
  - `UploadWorker` and `ReconcilerWorker` (POC) operate over Envelopes/CloudBinding and emit receipts.

## Cloud / Drive Integration
- UI: `CloudScreen` provides Google Sign‑In (connect/disconnect) and a “Verify now” action.
- Factory: `DriveServiceFactory.getAdapter(context)` returns a `DriveAdapter` when a signed‑in account with Drive scope exists.
  - Uses `GoogleSignInTokenProvider` and an OkHttp‑based `GoogleDriveAdapter` for Drive REST calls.
  - Test seam: `DriveServiceFactory.setTokenProviderOverrideForTests(fake)`; `invalidate()` clears cached adapter.
- Single Source of Truth
  - “Connected” is `DriveServiceFactory.getAdapter(context) != null`. History reads this; Cloud updates it by invalidating after sign‑in/out.
- Connect / Verify Telemetry
  - Connect result (`CloudScreen`): emits `ok_drive_connected` (success), `err_auth_cancelled` (cancel), `err_auth_no_scope` (no scope).
  - Verify (`CloudActions.verifyNow`): always emits; `err_no_account` when disconnected; `ok_verify_queued` then async probe → `ok_verified` or `err_verify_failed`.
  - Receipts include `message` tags for doc lineage: `brief:imbued-sycamore property:property-drive-connect-visible`.

## Data Model (Room)
- Envelope: `id PK`, `sha256 UNIQUE`, `mime?`, `text?`, `filename?`, `sourcePkgRef`, `receivedAtUtc`, `metaJson?`.
- ReceiptEntity: `id PK`, `ok`, `code`, `adapter`, `tsUtcIso`, `envelopeId?`, `envelopeSha256?`, `message?`, `spanId`, `receiptSha256`.
- SpanEntity: `spanId PK`, `adapter`, `startNanos`, `endNanos`, `envelopeId?`, `envelopeSha256?`.
- CloudBinding: `envelopeId PK`, `driveFileId UNIQUE`, `uploadedAtUtc`, `md5?`, `bytes?`.
- Migrations: 1→2→3→4; schemas exported under `app/schemas`.

## Tests
- Run: `./gradlew :app:testDebugUnitTest` (and `:core:testDebugUnitTest`). Robolectric + Compose debugUnitTests are enabled.
- Examples:
  - Repository idempotency, canonical JSON hashing, History no‑crash property.
  - Cloud verify receipts: `CloudVerifyReceiptsTest` (emits `err_no_account` vs `ok_verified`).
  - History chip UI: `HistoryCloudChipTest` (connected vs not connected via TokenProvider seam).
- Fakes: `core/src/test/.../FakeDriveAdapter` for pure JVM cloud tests.

## Build & Run
- Toolchain: Kotlin 2.x, AGP 8.x, Compose BOM, Material3, WorkManager, Coroutines, OkHttp, Room (KSP). Java/Kotlin 17.
- Android: minSdk 26, targetSdk 34.
- Commands:
  - Unit tests: `./gradlew :app:testDebugUnitTest`.
  - Build: `./gradlew :app:assembleDebug`.

## TroubleshootFlow (MFME)
- `tsflow/*.md` documents the TroubleshootFlow for brief `imbued-sycamore (b5e49dc5)`.
  - Property: `property-drive-connect-visible` — connect/verify must be observable and reflected in History.
  - Describe/Scaffold/Instrument/Implement/Test/Emit updated to this user story.
- Related design brief: `devcontext/morph brief Drive Uploader Connector … twisted-hazel (ea6f0c28)`.

## Agent Seams & Conventions
- Keep core pure; put effects behind adapters/factories.
- Emit receipts for meaningful actions; avoid silent paths.
- Use `ServiceLocator.receiptEmitter(context)` to emit receipts from UI.
- In tests, use `DriveServiceFactory.setTokenProviderOverrideForTests(fake)` to control “connected”.

## TODO / Next
- Complete `UploadWorker` to stream, verify (md5/bytes), and bind with receipts, including `OkDuplicateUpload`.
- Reconciler: search/bind by sha256 for duplicates; robust error taxonomy for auth/network.
- UI: basic receipt viewer and filters; optional snackbar for verify outcomes.

