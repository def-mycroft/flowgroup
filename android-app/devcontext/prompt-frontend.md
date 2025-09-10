Here’s a self-contained **morph/brief codex setup prompt** to implement the Settings subpage. It explicitly references the PingNow brief by codename and gives Codex everything it needs to wire UI ↔ config ↔ worker without touching core arrows.

---

# CODEX SETUP PROMPT — 道 morph / brief — **Settings › Location Ping (UI subpage)**

**Reference (must align with this codename):**
*morph brief PingNow Snapshot to SMS w Receipt — receding-bamboo 66af03c8*
Use the same UTC discipline, idempotent collapse semantics, and typed Receipts as defined by **PingNow**. This UI triggers a one-shot “Test now” using the PingNow send path and controls a periodic background pinger (already specified separately).

> \[!note] Morphism
> **Intent**
> Provide a Settings subpage that lets the user 1) enable/disable continuous GPS→Google-Maps-link→SMS pings, 2) set the destination phone number (E.164), 3) set the ping interval in minutes (default 15), and 4) run a one-tap “Test now” that delegates to **PingNow (receding-bamboo 66af03c8)**. The page persists preferences via DataStore and schedules/cancels the periodic worker accordingly.

## 投 (tóu) — input / casting

* User gestures on **Settings ▸ Location Ping**: toggle `enabled`, text field `to_e164`, numeric field `interval_minutes`, button **Test now**.
* Permissions launchers for **SEND\_SMS** and background location as needed (show non-blocking rationale if missing).
* Existing app kernel with **PingNow** arrow (receding-bamboo 66af03c8) and **SmsLocationPingerWorker** backend entrypoint (do not re-implement the arrow here).

## 約 (yuē) — constraints

* Must accept and validate a destination **E.164** phone number; reject non-E.164 with inline error and keep toggle off until valid.
* Interval minimum hard floor = **15** minutes (Android periodic floor); coerce smaller input to 15 and show helper text.
* **Privacy-minimal UI**: never render the full SMS body; display destination masked (e.g., `+1•••555-12••`).
* **UTC discipline** for displayed timestamps (last ping status) and day-keyed behavior matches PingNow invariants.
* No main-thread I/O; all DataStore and scheduling off main.

## 果 (guǒ) — output / essence

* Persisted preferences (DataStore):

  * `ping_enabled: Boolean`
  * `ping_to_e164: String`
  * `ping_interval_min: Int` (default **15**)
* Side effects:

  * When `ping_enabled` turns **on** with valid `to_e164`, schedule **SmsLocationPingerWorker** at `ping_interval_min`.
  * When `ping_enabled` turns **off**, cancel the worker.
  * **Test now**: invoke the **PingNow** arrow using current `to_e164`; surface result code inline (success/typed error).
* Status area: show **Last ping** timestamp (UTC) and last result code; do not show message content.

## 經 (jīng) — invariants

* One source of truth = DataStore; UI reflects live prefs via `Flow`.
* Toggle state mirrors actual schedule: scheduling succeeds → toggle stays on; scheduling fails → toggle reverts and shows inline error.
* Never enable if `to_e164` invalid or empty.
* Strings/formatting UTF-8; no blocking dialogs for permission denials—inline, recoverable UX.

## 絡 (luò) — failure modes

* `empty_input` (missing `to_e164`): keep toggle off; red helper under number field.
* `invalid_number` (fails E.164): same as above.
* `permission_denied` (SMS or background location): show inline banner; keep toggle off.
* `schedule_failed` (WorkManager or OS constraint): revert toggle and show retry hint.
* **Test now** returns typed errors from **PingNow**; map to small status pill (e.g., `OkSent`, `permission_denied`, `device_unavailable`).

## 器 (qì) — adapters & ports

* **UI:** Jetpack Compose screen `LocationPingSettingsScreen`.
* **Prefs:** `DataStore<LocationPingPrefs>` with schema + migration from defaults.
* **Scheduling:** `WorkScheduler.schedulePinger(intervalMin)` and `cancelPinger()`.
* **Test:** `PingNowClient.send(to_e164)` that delegates to the existing PingNow arrow (receding-bamboo 66af03c8).
* **Permissions:** `ActivityResultLauncher` wrappers for SMS + background location.

## File plan (create/extend)

* `ui/settings/LocationPingSettingsScreen.kt` — Composables, state wiring, validators, permission hooks.
* `data/datastore/LocationPingPreferences.kt` — Proto/Preferences DataStore model + keys + default(15).
* `domain/ping/LocationPingController.kt` — Facade: read/write prefs; schedule/cancel worker; call **PingNow** for Test.
* `work/SmsLocationPingerWorkScheduler.kt` — Thin wrapper over WorkManager (used by controller).
* `domain/ping/PingNowClient.kt` — Small interface that invokes the **PingNow** arrow (receding-bamboo 66af03c8).

## Compose UX spec (succinct)

* Top-level `Scaffold` with title “Location Ping”.
* **Phone number** `OutlinedTextField` with E.164 filter; helper: “Use full international format (e.g., +14155551212).”
* **Interval** `NumberTextField` (mins) with helper: “Minimum 15 minutes.” Coerce <15 to 15 on blur.
* **Enable pings** `Switch`: disabled unless number valid + required permissions granted.
* **Test now** `Button`: enabled when number valid; shows transient snackbar with last result code.
* **Status** row: “Last ping: 2025-09-10T19:44:02Z — OkSent” (UTC only).

## Validation details

* E.164 regex: `^\+[1-9]\d{1,14}$` (no spaces/dashes in stored value; allow formatted input but normalize to canonical before save).
* Masking for display: keep leading country code and last two digits; replace middle digits with bullets.

## Scheduling contract

* `schedulePinger(intervalMin: Int)` enqueues/updates a `PeriodicWorkRequest` for `SmsLocationPingerWorker` with flex 5m; tag consistently (e.g., `"sms-location-pinger"`).
* `cancelPinger()` cancels by tag.
* `BOOT_COMPLETED` listener elsewhere will re-enlist when `ping_enabled=true` (UI does not implement boot logic).

## Done-when (acceptance)

* With valid number and granted permissions, enabling toggle schedules worker and persists prefs; toggling off cancels.
* Entering `<15` is stored as `15` and reflected in UI.
* “Test now” triggers PingNow send path (receding-bamboo 66af03c8) and surfaces the typed outcome.
* Status shows UTC timestamp/result of the last attempt; persists across process death.
* Invalid/empty number or missing permissions keep toggle off with clear inline guidance; no crashes; no main-thread I/O.

**Non-goals (for Codex):** Do not re-implement PingNow’s core arrow or any Receipt schemas; only invoke them. Do not introduce cloud egress. Keep strings minimal; no localization needed yet.

**Style:** Kotlin, Jetpack Compose, coroutines/Flow, DataStore (Preferences or Proto), WorkManager. Keep functions small, pure where possible, and testable with fakes (Prefs, WorkScheduler, PingNowClient).

