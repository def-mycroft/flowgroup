# Android App – Agent Summary

This repo contains a minimal, test-oriented Android app that captures heterogeneous inputs, normalizes them into “envelopes”, persists them with idempotency, and emits structured telemetry. It uses Room for storage, Jetpack Compose for UI, and Coroutines throughout. The code is intentionally compact and modular to make changes easy for agents.

## Modules
- app: Main Android application (UI, adapters, repository, Room, telemetry, export integration).
- core: Pure JVM Android library with cloud interfaces (e.g., `DriveAdapter` and types). No Android dependencies.
- vaultlogsurface: JVM-only module reserved for vault mirroring/log surfacing experiments. Not currently wired into app code directly.

## Purpose
- Ingest data from share sheet, camera, mic, files, location, sensors, and SMS into a single `Envelope` model.
- Persist envelopes idempotently by `sha256` with simple history and inspection.
- Emit telemetry receipts/spans for every operation (success or error), with canonical JSON lines and stable hashes.
- Optionally chain envelopes into a file-based log and mirror to an Obsidian vault (no-op by default).

## Architecture
- UI (Compose):
  - `KernelActivity` hosts `KernelApp` with bottom navigation.
  - Screens: `CaptureScreen`, `HistoryScreen`, `AboutScreen`.
  - `KernelViewModel` exposes `envelopes` and `receipts` as StateFlows and thin save methods calling the repository.
- DI / Wiring:
  - `AppModule` provides `KernelDatabase`, `ReceiptEmitter`, `ErrorEmitter`, `EnvelopeChainer`, and `KernelRepositoryImpl`.
  - `ServiceLocator` lazily creates and caches a singleton repository per process.
- Repository:
  - `KernelRepositoryImpl` implements `KernelRepository` and `KernelRepositorySms` and owns the idempotent save logic per adapter.
  - On each save: start span → build `Envelope` → check/insert → emit telemetry → bind span → chain/export.
  - New dependency: `EnvelopeChainer` to append envelope hashes to a local NDJSON log and call `ObsidianExporter`.
- Adapters:
  - `ShareAdapter` extracts `SharePayload` (text or stream) from `ACTION_SEND` with best-effort source and metadata.
  - Additional simple adapters for Camera/Mic/Files/Location/Sensors exist at the repository API surface; UI buttons demonstrate usage.
- Telemetry:
  - `ReceiptEmitter`/`ErrorEmitter` write to Room and `NdjsonSink` using a canonical, deterministic JSON shape (`CanonicalReceiptJson`).
  - `SpanDao.insert` uses REPLACE so spans are idempotent on end.
- Background work (stubs/POC):
  - `UploadWorker` placeholder for background Drive uploads.
  - `ReconcilerWorker` scans local state vs cloud via `DriveAdapter` (implementation TODO) and emits receipts (rebound/already-bound/not-found).

## Data Model (Room)
- Envelope (table `envelopes`):
  - `id PK`, `sha256 UNIQUE`, `mime?`, `text?`, `filename?`, `sourcePkgRef`, `receivedAtUtc`, `metaJson?`.
  - Unique index on `sha256` enforces repository idempotency.
- Telemetry – ReceiptEntity (table `receipts`):
  - `id PK`, `ok`, `code`, `adapter`, `tsUtcIso`, `envelopeId?`, `envelopeSha256?`, `message?`, `spanId`, `receiptSha256`.
- Telemetry – SpanEntity (table `spans`):
  - `spanId PK`, `adapter`, `startNanos`, `endNanos`, `envelopeId?`, `envelopeSha256?`.
- CloudBinding (table `cloud_binding`):
  - `envelopeId PK`, `driveFileId UNIQUE`, `uploadedAtUtc`, `md5?`, `bytes?`.
- Migrations: versions 1→2, 2→3, 3→4 defined in `KernelMigrations.kt`. Schemas are exported under `app/schemas`.

## Key Flows
- Share: Intent → `ShareAdapter.fromIntent` → `SharePayload` → `KernelRepository.saveFromShare` → idempotent insert → receipts/spans → optional chain/export → UI updates via flows.
- Capture (camera/mic/file/location/sensors): UI → `KernelViewModel.saveFrom*` → repository → telemetry → UI.
- SMS: `saveSmsOut` and `ingestSmsIn` generate envelopes, write payload for outbox, and emit receipts.

## Telemetry Details
- Canonical JSON lines ensure consistent hashing (`receiptSha256`).
- `NdjsonSink` partitions files by UTC date (`filesDir/telemetry/YYYY-MM-DD`) with fsync-on-write for durability.
- `TelemetryCode` provides a compact taxonomy for success/error codes (e.g., `ok_new`, `ok_duplicate`, `permission_denied`, `error_not_found`).

## Build & Test
- Tooling: Kotlin 2.0.x, AGP 8.12.x, Compose BOM, Material3, WorkManager, Coroutines.
- Android: `minSdk=26`, `targetSdk=34`, Java/Kotlin target 17.
- Room via KSP with schema export at `app/schemas`.
- Run tests: `./gradlew test` (Robolectric enabled with Android resources). Current tests cover repository idempotency, Room schema, telemetry determinism, and cloud binding DAO.

## Extension Points & TODOs
- DriveAdapter: Provide a concrete cloud implementation (in `core`) and wire into `UploadWorker`/`ReconcilerWorker`.
- Vault mirroring: `ObsidianExporter` is a no-op unless constructed with a root directory; plumb configuration if needed.
- Multi-share: Add `ACTION_SEND_MULTIPLE` to `ShareAdapter` and repository.
- History UI: Filters, details, and export affordances can expand; wire to telemetry taxonomy.
- Migrations tests: Add cross-version migration tests to lock schemas.

## Guardrails for Changes
- Preserve idempotency: all save paths must dedupe by `sha256`; do not create duplicate spans/receipts.
- Update schemas: bump DB `version`, provide migrations, and commit new JSON schema files.
- Keep adapters slim: platform translation in adapters; persistence/telemetry in repository.
- Compose best practices: keep business logic in ViewModel/Repository; UI consumes flows.

## Useful Paths
- UI: `app/src/main/java/com/mfme/kernel/ui/*`
- Repository: `app/src/main/java/com/mfme/kernel/data/KernelRepository*.kt`
- Telemetry: `app/src/main/java/com/mfme/kernel/telemetry/*`
- Export: `app/src/main/java/com/mfme/kernel/export/*`
- Room: `app/src/main/java/com/mfme/kernel/data/*` and `.../data/telemetry/*`, schemas in `app/schemas`
- Adapters: `app/src/main/java/com/mfme/kernel/adapters/*`
- Workers: `app/src/main/java/com/mfme/kernel/work/*`
- Tests: `app/src/test/java/com/mfme/kernel/**`
