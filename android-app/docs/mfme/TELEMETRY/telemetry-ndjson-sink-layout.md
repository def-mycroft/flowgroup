# Telemetry Ndjson Sink Layout

Id: telemetry-ndjson-sink-layout
Updated: 2025-09-10T19:17:49Z

Layout
- Path: files/telemetry/YYYY-MM-DD/
- Files: <epoch>-<receiptSha>.ndjson (one JSON per line)
- Fallback: receipts.ndjson append-only if rename fails.

Atomicity
- Write to tmp, fsync, rename; fallback append preserves durability.
