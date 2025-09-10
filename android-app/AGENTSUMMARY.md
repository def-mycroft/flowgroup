# Android App Design Brief (for Agents)

This module contains a minimal Android app scaffold focused on capturing, normalizing, and persisting “envelopes” of shared content and emitting telemetry receipts for operations. It favors small, testable units and Room-backed persistence with tracked schemas.

## Purpose
- Collect inputs from multiple adapters (share sheet, camera, mic, file, location, sensors) into a unified Envelope model.
- Persist envelopes idempotently by content hash (sha256) and surface basic history UI.
- Emit structured telemetry receipts and spans for success/failure auditing.

## Architecture
- UI: Jetpack Compose. Key screen: `HistoryScreen` shows receipts and envelopes with simple filtering.
- ViewModel: `KernelViewModel` exposes flows (`observeEnvelopes`, `observeReceipts`) and save entry points for each adapter pathway.
- Data: Room database `KernelDatabase` with entities `Envelope`, `ReceiptEntity`, `SpanEntity`; DAOs provide paging and observation. TypeConverters handle time types.
- Repository: `KernelRepository`/`KernelRepositoryImpl` coordinates persistence and telemetry emission. Saves are idempotent by sha256.
- Adapters: Platform-facing helpers convert platform intents/data into domain models.
  - `ShareAdapter` extracts `SharePayload` (stream/text) from `ACTION_SEND` intents, resolving display name/size/MIME and a `sourceRef`.
- Telemetry: Emitters (`ReceiptEmitter`, `ErrorEmitter`, NDJSON sink) record outcomes and spans; `SpanDao.insert` uses `OnConflictStrategy.REPLACE` to avoid duplicate spans.

## Data Model (Room)
- `Envelope`
  - Fields: `id PK`, `sha256 UNIQUE`, `mime?`, `text?`, `filename?`, `sourcePkgRef`, `receivedAtUtc`, `metaJson?`.
  - Unique index on `sha256` enforces idempotent saves.
- `ReceiptEntity`
  - Fields: `id PK`, `ok`, `code`, `adapter`, `tsUtcIso`, `envelopeId?`, `envelopeSha256?`, `message?`, `spanId`, `receiptSha256`.
- `SpanEntity`
  - Fields: `spanId PK`, `adapter`, `startNanos`, `endNanos`, `envelopeId?`, `envelopeSha256?`.
- Schemas tracked under `app/schemas/...` and must be updated when entities or versions change.

## Key Flows
- Share → `ShareAdapter.fromIntent` → `SharePayload` → `KernelRepository.save*` → `Envelope` insert (idempotent) → telemetry receipts/spans → `HistoryScreen` reflects via flows.
- Direct capture (camera/mic/file/location/sensors) → `KernelViewModel.saveFrom*` → repository pathways with telemetry.

## Build & Test
- Module path: `app/` (within this directory).
- Tooling: Kotlin, Compose BOM, Material3, WorkManager, Coroutines.
- Min/Target SDK: `minSdk=26`, `targetSdk=34`. Java/Kotlin target 17.
- Room via KSP; schema directory configured at `app/schemas` and committed.
- Tests: Robolectric unit tests (Android resources enabled). Coverage includes Room round‑trips, ordering, and repository idempotency.

## Recent Changes (context for designs)
- Raised `minSdk` to 26; Material3 theme fixes.
- Room configuration stabilized (KSP, schema export) and schemas added.
- Legacy `app.zero.inlet` sources removed; consolidated under `com.mfme.kernel`.
- Telemetry taxonomy and receipt infrastructure implemented; span insert made idempotent.
- `HistoryScreen` string safety and simple filters (All/Errors/Ok).

## Design Guardrails
- Preserve idempotency: any new save pathway must dedupe by `sha256` and avoid duplicate spans/receipts.
- Update Room schemas: when changing entities/DAOs/migrations, bump DB `version`, add migrations, and commit new JSON to `app/schemas/...`.
- Keep adapters narrow: platform translation stays in adapter layer; domain/persistence in repository.
- UI simplicity: Compose UIs should consume flows from the repository via ViewModel; avoid business logic in Composables.
- Telemetry first: all success/error paths should emit a `ReceiptEntity` with a meaningful `code`, and bind to spans where applicable.

## Helpful Entrypoints (paths)
- UI: `app/src/main/java/com/mfme/kernel/ui/HistoryScreen.kt`, `KernelViewModel.kt`
- Adapters: `app/src/main/java/com/mfme/kernel/adapters/share/ShareAdapter.kt`
- Data: `app/src/main/java/com/mfme/kernel/data/KernelDatabase.kt`, DAOs under `.../data/telemetry`
- Repository: `app/src/main/java/com/mfme/kernel/data/KernelRepository*.kt`
- Schemas: `app/schemas/...`
- Tests: `app/src/test/java/com/mfme/kernel/*Test.kt`

## Open Opportunities
- Add DB migrations tests across schema versions.
- Expand History UI (filtering, details, export) driven by receipts taxonomy.
- Extend adapters for multi‑share (`ACTION_SEND_MULTIPLE`) and richer metadata.
