morph brief PingNow Snapshot to SMS w Receipt receding-bamboo 66af03c8

receding-bamboo
66af03c8-3479-41fc-9764-003e18b55c50
2025-09-10T11:31:57.026040-06:00

***

2025-09-10 12:32:30 -0600
https://chatgpt.com/g/g-p-68c19e25848c81919ffd61ec8142f288-morph-update/c/68c1c432-91dc-8320-97dc-32cc08ea08ac

***

# 道 morph / brief — **PingNow: one-tap snapshot to SMS (with Receipt)**

> [!note] Morphism  
> **Intent**  
> A single tap sends your current location via SMS to a pre-configured recipient, and always leaves an auditable Receipt. The arrow is local-first: it captures a one-shot GPS fix, renders a compact body (Google Maps link + UTC timestamp), persists an `Envelope{source:"sms_out"}` with privacy-minimal facts, delegates transport to `SmsSendAdapter`, and emits a typed `ok|error` Receipt with a stable fingerprint.

---

## 投 (tóu) — input / casting

- **User action**: tap “PingNow”.
    
- **Config**: `DestinationStore.to_e164` (required), optionally `DestinationStore.label`.
    
- **Adapters present**: `LocationAdapter` (one-shot fix), `SmsSendAdapter` (send single text), `ReceiptEmitter`.
    
- **Clock/Digest**: UTC `Instant.now()`, `sha256(UTF-8 bytes)` of SMS body.
    
- **Permissions**: foreground location; `SEND_SMS` (and `READ_SMS` only if delivery receipts are consumed).
    
- **Time budget**: location fix target ≤ 6 s; SMS hand-off ≤ 1 s after persist.
    

---

## 果 (guǒ) — output / fruit (essence)

- **Arrow (explicit)**:  
    `Tap + Destination → LocationAdapter.oneShotFix() → BodyTemplate.render(fix, ts_utc) → Envelope{source:"sms_out", body_sha256, to_e164, ts_utc} persisted (tmp→fsync→atomic rename) → SmsSendAdapter.send(to, body) → Receipt{ok|error, code, ts_utc, fingerprint}`.
    
- **User-visible**: brief Snackbar/Chip “Ping sent” or “Ping failed (reason)”; last-ping preview (ts_utc, distance delta if available).
    
- **System-visible**:
    
    - `Envelope` row with `source="sms_out"`, `body_sha256`, `to_e164`, `ts_utc`, and a minimal `meta.location` snapshot (lat, lon, accuracy).
        
    - `Receipt` row + NDJSON append with deterministic key order and a **stable fingerprint** `receipt_sha256` (computed over canonical JSON excluding the fingerprint itself).
        
- **Idempotent collapse**: duplicates within a day collapse by `{to_e164, body_sha256, day(ts_utc)}`; transport is suppressed on perfect duplicates, while still emitting a Receipt (`OkDuplicateSuppressed`).
    

---

## 經 (jīng) — invariants / properties

1. **UTC discipline** — every persisted timestamp is UTC (no device-local time anywhere).
    
2. **Duplicate collapse** — `{to_e164, body_sha256, day}` is a unique key; repeating the same ping on the same day does not re-send.
    
3. **Durability** — all file writes use `tmp → fsync → atomic rename`; DB + FS appear all-or-nothing.
    
4. **Receipt on every attempt** — success or failure, a typed Receipt is emitted with a stable fingerprint.
    
5. **Privacy-minimal** — zero cloud egress on capture paths; receipts do not store raw message text or precise PII beyond `to_e164` (masking permitted in exported/visual views).
    
6. **Radio-only** — no network dependency except the SMS radio; sending works offline from data/Wi-Fi.
    

---

## 絡 (luò) — failure modes (typed, deterministic)

- `permission_denied` — user declined location or SMS permission.
    
- `empty_input` — destination missing/blank, or template resolved to empty body.
    
- `device_unavailable` — no GPS fix within budget or telephony stack not ready.
    
- `storage_quota` — DB/FS write fails; must leave a Receipt and no partials.
    
- `digest_mismatch` — body hash drift detected between pre- and post-persist canonicalization.
    

_All failures emit `{ok:false, code, message, ts_utc, receipt_sha256}`; messages are non-PII and ≤ 120 chars._

---

## 約 (yuē) — constraints

- **No background location**; one-shot foreground only.
    
- **No main-thread I/O**; hashing/persist on `Dispatchers.IO`.
    
- **Receipts ≤ 1 KB**, canonical JSON key order; fingerprint is `sha256` over canonical bytes.
    
- **Template**: `"$label — https://maps.google.com/?q=<lat>,<lon>&z=16 @ <ISO8601_UTC>"` (customizable, but hash is computed on the final rendered body).
    
- **Throughput guard**: minimum 10 s between attempts to prevent accidental tap-spamming.
    

---

## 器 (qì) — adapters & ports

- **LocationAdapter**
    
    - `suspend fun oneShotFix(timeoutMs=6000): Result<LocationFix>`
        
    - Returns `{lat, lon, accuracy_m, provider, ts_utc}`; never caches silently.
        
- **SmsSendAdapter**
    
    - `suspend fun send(toE164: String, bodyUtf8: ByteArray): Result<SmsSendAck>`
        
    - Must not mutate body bytes; returns `MessageId?` if the platform exposes one.
        
- **ReceiptEmitter**
    
    - `fun emit(morph: "PingNow", code: "ok"|"error"|"...", facts: Map) : Receipt` (stable, canonical).
        
- **DigestAdapter**: `sha256(UTF-8 bytes)` over the _exact_ SMS body after template render.
    
- **ClockAdapter**: monotonic span + `Instant.now()` (UTC).
    
- **DestinationStore**: DataStore-backed `{to_e164:String, label:String?}`.
    

---

## 形 (xíng) — schema (Room + content-addressed FS)

**Envelope (sms_out)**

```json
{
  "id": "<uuid>",
  "source": "sms_out",
  "created_at_utc": "2025-09-10T00:00:00Z",
  "body_sha256": "hex-64",
  "to_e164": "+13035551234",
  "meta": {
    "location": {"lat": 39.7392, "lon": -104.9903, "accuracy_m": 12.0},
    "template_ver": "pingnow@1"
  }
}
```

**Receipt (canonical, ≤1 KB)**

```json
{
  "morph": "PingNow",
  "code": "OkSent|OkDuplicateSuppressed|permission_denied|empty_input|device_unavailable|storage_quota|digest_mismatch",
  "ok": true,
  "ts_utc": "2025-09-10T00:00:01Z",
  "facts": {
    "to_masked": "+1•••1234",
    "body_sha256": "hex-64",
    "idempotency_key": "to:hex-64:2025-09-10"
  },
  "receipt_sha256": "hex-64"
}
```

---

## 面 (miàn) — surfaces (UI/UX)

- **PingNow Button/Card**: prominent, thumb-reachable.
    
- **First-run flow**: destination + permissions in a single compact sheet; validates E.164.
    
- **Send affordance**: tap → spinner ≤ 6 s → brief result.
    
- **History chip**: “Last sent 12:03:14Z · 0.8 km ago” (distance computed vs last `meta.location`).
    
- **Privacy toggle**: show/hide masked destination in UI; never display full SMS body.
    

---

## 工 (gōng) — sequence & transactional guards

1. Resolve destination; guard `empty_input`.
    
2. `LocationAdapter.oneShotFix()` with timeout; on timeout → `device_unavailable`.
    
3. Render `body = Template(fix, ts_utc)`; compute `body_sha256`.
    
4. Construct `Envelope(sms_out)`; **persist** (DB/FS) using `tmp→fsync→atomic rename`.
    
5. Compute `idempotency_key = {to_e164, body_sha256, day}`; if duplicate → emit `OkDuplicateSuppressed`, **skip send**.
    
6. Else `SmsSendAdapter.send(to, body)`; map platform result to `OkSent` or typed `error`.
    
7. Emit deterministic `Receipt`; append NDJSON; update UI.
    

Rollback/compensation:

- FS ok / DB fail ⇒ delete FS; emit `storage_quota`.
    
- DB ok / FS fail ⇒ tombstone DB row; emit `storage_quota`.
    
- Persist ok / send fail ⇒ keep `Envelope`; emit error Receipt; user can retry (idempotency key prevents duplicate sends if body unchanged).
    

---

## 檢 (jiǎn) — verification (properties over anecdotes)

- **Idempotency hammer**: N identical taps within a day ⇒ one transport; N Receipts with `OkDuplicateSuppressed` after the first.
    
- **UTC round-trip**: all stored and rendered times are UTC; no locale leakage.
    
- **Deterministic fingerprints**: same inputs ⇒ identical `receipt_sha256` across runs/devices.
    
- **Atomic writes under fault**: kill process between tmp/write/rename; no partials observable; Receipts explain outcome.
    
- **Privacy audit**: Receipts and logs contain no raw message text; only `body_sha256` and masked destination.
    

_Instrumentation tests_:

- Deny SMS permission ⇒ `permission_denied` Snackbar + Receipt.
    
- Deny location ⇒ `permission_denied` without calling `SmsSendAdapter`.
    
- No fix within 6 s ⇒ `device_unavailable`.
    
- Duplicate within a day ⇒ transport suppressed, Receipt `OkDuplicateSuppressed`.
    
- Happy path ⇒ real SMS visible in emulator/device outbox; Receipt `OkSent`.
    

---

## 次箭 (cì jiàn) — next arrows

- **Delivery Acks (optional)**: add `SmsReceiveAdapter` to correlate carrier delivery reports to existing Envelopes (new Receipt `OkDelivered`).
    
- **Obsidian log hook (optional)**: stream PingNow Receipts into the daily vault log (`logs/YYYY-MM-DD.md`) with masked destination and map link.
    
- **Rate policy**: per-recipient cool-downs and daily caps; surface typed `rate_limited` without breaking invariants.
    
- **Multi-recipient migration**: expand `DestinationStore` to a small roster; idempotency key lifts to `{to[], body_sha256, day}` with per-recipient Receipts.
    

---

## Codex-ready acceptance (3 truths)

1. Every tap yields a deterministic Receipt and a durable `Envelope(sms_out)`; no silent paths.
    
2. Duplicate pings within a day collapse by `{to_e164, body_sha256, day}`; transports are suppressed without losing lineage.
    
3. No cloud calls occur on this arrow; only the SMS radio is used, and all timestamps are UTC.



*** 

Here’s a single, self-contained Codex prompt that implements “continuous GPS → Google Maps link → SMS every N minutes,” plus the two MFME morph/brief stubs you asked for (Settings UI + backend worker). I’ve aligned contracts with your existing PingNow/Receipts/Adapters doctrine so you get canonical Envelopes and typed Receipts out of the box. Citations point to the relevant briefs we’ve already locked.

---

# CODEx IMPLEMENTATION PROMPT — **Continuous GPS→SMS Pinger**

You are adding a “Continuous Location Ping” feature to the existing Android kernel app. The user configures a destination phone number and an interval (default 15 minutes). When enabled, a background worker obtains a one-shot location fix, renders a **Google Maps** link + UTC timestamp into the SMS body, persists an `Envelope{source:"sms_out"}` with `body_sha256`, and sends the SMS. Each attempt emits a deterministic **Receipt**. The send repeats every N minutes until the user disables it (or removes the config). Body shape and invariants mirror **PingNow** (one-tap) but run on a schedule. Template reference: `"$label — https://maps.google.com/?q=<lat>,<lon>&z=16 @ <ISO8601_UTC>"`.

## Scope & modules

- **UI (Settings subpage)**: “Location Ping” screen under Settings for:
    
    - `to_e164` (validated E.164), `interval_minutes` (int, default 15), `enabled` (toggle).
        
    - “Test now” (fires a one-shot **PingNow** send) and “Stop pings”.
        
    - Surface last-ping status (ts_utc + last result code), masked destination.
        
- **Config storage**: `DataStore` keys: `ping_to_e164`, `ping_interval_min`, `ping_enabled`.
    
- **Worker**: `SmsLocationPingerWorker` scheduled via `WorkManager`:
    
    - Default `PeriodicWorkRequest` = 15 min (Android minimum), flex 5 min.
        
    - Reads config; if `enabled && to_e164` present → run arrow; else `Result.success()` no-op.
        
    - Uses **LocationAdapter** (`FusedLocationProviderClient`) one-shot snapshot; no silent caching.
        
    - Renders body per **PingNow** template; computes `body_sha256`; persists `Envelope(sms_out)`; idempotency on `{to_e164, body_sha256, day}`; then `SmsSendAdapter.send(...)`. Duplicate bodies within the same UTC day suppress actual transport but still emit a typed Receipt (`OkDuplicateSuppressed`).
        
- **Receipts**: Emit canonical Receipts (Room row + NDJSON) with stable fingerprints; all timestamps UTC; append-only sink.
    
- **Boot**: `BOOT_COMPLETED` receiver re-enqueues the periodic work if `ping_enabled=true`.
    
- **Optional**: If the Obsidian vault surface is enabled in this build, stream these Receipts into the daily vault log as masked entries (no raw SMS body), per your Log Surface brief.
    

## Permissions & policy

- Manifest: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` (for periodic background fixes), `SEND_SMS`, `RECEIVE_BOOT_COMPLETED`.
    
- Foreground constraints: If the OS requires a foreground service for background location during worker execution, provide `ForegroundInfo` with `serviceType="location"`.
    
- Degrade politely: If background location is not granted, fall back to `getCurrentLocation` with foreground requirement or to last-known location; map to `permission_denied` if neither is permitted. Always emit a Receipt.
    

## Data & invariants (reuse existing doctrine)

- **Envelope(sms_out)** fields and meta: include `to_e164`, `body_sha256`, `created_at_utc`, and minimal `meta.location {lat,lon,accuracy}`. Persist with `tmp→fsync→atomic rename`.
    
- **Idempotency**: Key = `{to_e164, body_sha256, day(ts_utc)}`; if duplicate, suppress send and emit `OkDuplicateSuppressed`.
    
- **Template**: Google Maps link + ISO-8601 UTC timestamp; hash over final rendered bytes (UTF-8).
    
- **Receipts**: Deterministic canonical JSON; UTC; one per attempt; codes include `OkSent`, `OkDuplicateSuppressed`, `permission_denied`, `device_unavailable`, `storage_quota`, `digest_mismatch`.
    

## Acceptance tests (unit + instrumentation)

1. **Happy path**: With valid config + permissions, worker run creates one `Envelope(sms_out)`, one `OkSent` Receipt, real SMS visible in device/emulator outbox.
    
2. **Duplicate day suppression**: Two runs with same coordinates and rendered body within same UTC day → second run yields `OkDuplicateSuppressed`; no second SMS.
    
3. **Permission denied**: Missing SMS or background location → `permission_denied` Receipt; no DB partials.
    
4. **Device unavailable**: No fix within timeout (e.g., 6s) → `device_unavailable` Receipt.
    
5. **Durability**: Kill between tmp/write/rename → no partials; Receipts explain outcome.
    
6. **UTC discipline**: All stored/rendered timestamps are UTC; day-keying honors UTC midnight.
    

## File plan (suggested)

- `ui/settings/LocationPingSettingsScreen.kt`
    
- `data/datastore/LocationPingPreferences.kt`
    
- `work/SmsLocationPingerWorker.kt` (+ `ForegroundInfo` helper)
    
- `adapters/location/LocationAdapter.kt` (reuse one-shot contract)
    
- `adapters/sms/SmsSendAdapter.kt` (existing)
    
- `telemetry/ReceiptEmitter.kt` (existing) + new `pinger` codes
    
- `boot/BootReceiver.kt` to re-schedule work
    

---

# 道 morph / brief — **Settings › Location Ping (UI subpage)**

> [!note] Morphism  
> **Intent**  
> A Settings subpage that lets the user enable/disable continuous GPS→SMS pings, set a destination phone number and an interval (default 15 min), and run a one-tap test send. It writes DataStore config and reflects last-ping status and masked recipient.

## 投 (tóu) — input / casting

- User gestures on Settings subpage (toggle, text fields, test button).
    
- Existing `DestinationStore`/DataStore for E.164 + label; extend with interval + enabled.
    
- Permissions launcher for SMS + background location.
    

## 果 (guǒ) — output / essence

- Persisted `ping_to_e164`, `ping_interval_min=15` default, `ping_enabled`.
    
- Schedules/cancels `SmsLocationPingerWorker` according to toggle.
    
- “Test now” triggers **PingNow** (one-shot) using current config.
    

## 經 (jīng) — invariants

- E.164 validation; masked UI display of destination.
    
- UTC timestamps in status display; no local-time leakage.
    
- No main-thread I/O; DataStore usage off main.
    
- No raw SMS body shown in UI; privacy-minimal.
    

## 絡 (luò) — failure modes

- `empty_input` (no number) → disable toggle + error hint.
    
- `permission_denied` on SMS/background location → surface inline and leave disabled.
    
- Invalid interval (<15) coerced to 15 (OS minimum for periodic).
    

## 器 (qì) — adapters & ports

- `DataStore<LocationPingPrefs>`; `WorkScheduler.schedulePinger(...)`.
    
- `ActivityResultLauncher` for permissions.
    
- Reuses **PingNow** for “Test now.”
    

---

# 道 morph / brief — **SmsLocationPinger (backend worker)**

> [!note] Morphism  
> **Intent**  
> Every N minutes (default 15), obtain a one-shot GPS fix, render a Google Maps link + UTC timestamp, persist `Envelope(sms_out)` with `body_sha256`, send SMS to `to_e164`, and emit a deterministic Receipt. Repeat until disabled by user.

## 投 (tóu) — input / casting

- `ping_enabled`, `ping_to_e164`, `ping_interval_min` from DataStore.
    
- **LocationAdapter**: one-shot fix (no silent cache).
    
- **SmsSendAdapter**; **ReceiptEmitter**.
    

## 果 (guǒ) — output / essence

- Arrow: `Config → oneShotFix → Template.render → Envelope(sms_out) persist → SmsSendAdapter.send → Receipt`.
    
- Idempotent collapse within a UTC day by `{to_e164, body_sha256, day}`; duplicates suppress transport with `OkDuplicateSuppressed`.
    
- Append Receipt row + NDJSON with stable fingerprint.
    

## 經 (jīng) — invariants

- **UTC discipline** for all times; day-keying by UTC.
    
- **Durability**: `tmp→fsync→rename`; DB+FS appear all-or-nothing.
    
- **Privacy-minimal**: no raw body in Receipts; mask destination where displayed.
    
- **No cloud egress**; radio-only SMS path.
    

## 絡 (luò) — failure modes (typed)

- `permission_denied` (SMS or background location not granted).
    
- `device_unavailable` (no fix in budget / telephony stack down).
    
- `storage_quota` (DB/FS error), `digest_mismatch` (post-persist hash drift).
    

## 器 (qì) — adapters & harness

- `WorkManager PeriodicWorkRequest(15..interval)`; `BootReceiver` re-schedules.
    
- `LocationAdapter.oneShotFix(timeoutMs=6000)`; `SmsSendAdapter.send(to, body)`.
    
- `BodyTemplate`: `"$label — https://maps.google.com/?q=<lat>,<lon>&z=16 @ <ISO8601_UTC>"`.
    

---



