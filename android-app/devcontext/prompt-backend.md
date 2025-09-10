# CODEX SETUP PROMPT — 道 morph / brief: **SmsLocationPinger (backend worker)**

Implement the backend worker that emits continuous GPS→SMS pings on a schedule. Reuse the **one-shot message shape, body template discipline, idempotent collapse, and typed Receipts** from the codename document **“morph brief PingNow Snapshot to SMS w Receipt — receding-bamboo 66af03c8.”** Treat this worker as “PingNow on a timer” with identical invariants, differing only in its periodic trigger and config source.

**約 (yuē) — constraint.** The app exposes a config phone number and an interval; schedule a Google-Maps-formatted GPS ping **every N minutes (default 15)** to that number. It repeats until the user disables it or clears the number.

**投 (tóu) — input / casting.** Read `ping_enabled`, `ping_to_e164`, and `ping_interval_min` from `DataStore`. When enabled and a valid number exists, obtain a **one-shot** location fix via `LocationAdapter` (no silent cache), render the **same** body template as in **receding-bamboo 66af03c8** (Google Maps `https://maps.google.com/?q=<lat>,<lon>&z=16` + ISO-8601 UTC stamp), compute `body_sha256` over the final UTF-8 bytes, persist an `Envelope{source:"sms_out"}` with `meta.location`, then send with `SmsSendAdapter`. Always emit a canonical Receipt.

**果 (guǒ) — output / essence.** The arrow is: `Config → oneShotFix → Template.render (receding-bamboo) → Envelope(sms_out) persist → SmsSendAdapter.send → Receipt`. Schedule with `WorkManager PeriodicWorkRequest` using the user’s interval, coerce values below 15 to 15 to satisfy OS minimums, and re-enqueue on `BOOT_COMPLETED` if still enabled.

**經 (jīng) — invariants.** Mirror **receding-bamboo 66af03c8**: UTC discipline for all timestamps and day rollovers; durability `tmp→fsync→atomic rename` for file writes; **idempotent collapse** within a UTC day keyed by `{to_e164, body_sha256, day}` so identical bodies suppress transport but still log a deterministic Receipt; privacy-minimal telemetry (mask destination where displayed, never store raw SMS body in Receipts); **no cloud egress** on capture paths—SMS radio only.

**絡 (luò) — failure modes.** Use the same typed taxonomy as **receding-bamboo 66af03c8**: `OkSent`, `OkDuplicateSuppressed`, `permission_denied` (SMS or background location), `device_unavailable` (no fix in time or radio down), `storage_quota`, `digest_mismatch`. Always produce exactly one Receipt per attempt with a stable fingerprint.

**器 (qì) — adapters & harness.** Implement `SmsLocationPingerWorker` (Kotlin) invoked by `WorkManager` with a reasonable flex window; if the platform requires it, supply `ForegroundInfo` during location acquisition with `serviceType="location"`. Provide a small `BootReceiver` that re-schedules based on `DataStore`. Reuse existing `LocationAdapter`, `SmsSendAdapter`, `ReceiptEmitter`, and `EnvelopeRepository`. Ensure `PingNow`’s body template and hashing rules from **receding-bamboo 66af03c8** are referenced in code to prevent drift.

**Acceptance (tight).** With valid config and permissions, one run creates exactly one `Envelope(sms_out)` and an `OkSent` Receipt; a second run producing an identical body in the same UTC day suppresses transport and emits `OkDuplicateSuppressed`; missing permission yields `permission_denied` without partials; location timeout yields `device_unavailable`; killing mid-write never leaves partial artifacts; all timestamps and day keys are UTC.

**Delivery notes for Codex.** Name files `work/SmsLocationPingerWorker.kt`, `boot/BootReceiver.kt`, and `data/datastore/LocationPingPreferences.kt`. Do not invent new fields or Receipt codes; **conform to receding-bamboo 66af03c8** for schema, template, hashing, and codes. Keep all I/O off the main thread, and keep the worker self-healing: if config is missing or disabled, return `Result.success()` without side effects but still log a no-op span.

---

Notes appended from notes.md (decisions and constraints)

- Timestamp in SMS body: render using sender’s local time in the message body; database/Receipt timestamps remain UTC.
- Testing cadence: manual “Test now” is sufficient; no special 1-minute periodic periodicity is required beyond OS minimums.
- Permissions: background location is acceptable for periodic pings (supply ForegroundInfo during fix acquisition if required by platform).
- Recipients: support multiple destination E.164 numbers; send to all configured valid recipients per run.
- Delivery receipts: do not integrate carrier delivery receipts (no READ_SMS); rely on internal Receipts pathway only.
- Retry policy: if a run fails, do not chain immediate retries; allow the next scheduled interval to handle it.
- Auto-stop: service remains enabled until explicitly toggled off (no auto-expiry window).
- Poor accuracy: proceed with send even if accuracy is poor (>100 m).
- Privacy: never store or emit raw SMS body in Receipts; mask destinations where displayed.
- Self-test number: support a separate self-test number, optionally defaulting to the device’s own number if resolvable.
- Boot behavior: on device restart, re-schedule if `ping_enabled=true` (BootReceiver reads DataStore and re-enqueues worker).
- Data retention: keep Receipts/Envelopes indefinitely until user clears.
- Rate limit guard: cap sends to a maximum of 3 messages within any rolling 15-minute window across all recipients to avoid over-sending.
