# Inlet Archiver

## Input → Output Flow
- **Input:** The archiver receives an `Envelope` describing a shared file and the file's raw bytes.
- **Planning:** `planArchive` computes deterministic paths and a sidecar JSON from the envelope and bytes.
- **Commit:** `ArchiveFsAdapter.commit` writes the data and sidecar to temporary files, atomically moves them into the archive tree, then publishes an `ArchivePlan` on the commit bus.
- **Upload trigger:** `CommitListener` consumes commit events and enqueues unique work items in `WorkManagerStub` for downstream upload.

## Invariants
- **Idempotency:** Archive paths and work names derive from the SHA‑256 of the bytes; retrying with the same content results in the same paths and does not enqueue duplicate work.
- **UTC timestamps:** `Envelope.createdAtUtc` and the sidecar's `created_at` field use UTC, and archive shards are derived from these values.
- **Atomic commits:** Data and sidecar files are moved into place only after successful writes, ensuring that either both are present or neither.

## Failure Modes
- Hash mismatch between the provided bytes and `Envelope.contentHashSha256` aborts planning.
- Read or write failures while copying data or sidecar files prevent commit emission.
- Downstream consumers may ignore commit events if the channel is closed or unavailable, leaving work items unqueued.
- Unsupported media types fall back to a generic extension, which may affect downstream processing.
