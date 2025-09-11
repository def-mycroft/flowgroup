morph brief Drive Uploader Connector for Android App twisted-hazel ea6f0c28

twisted-hazel
ea6f0c28-6200-44d1-b07c-e52520ab30db
2025-09-10T08:18:15.759067-06:00

***


> [!note] Morphism
> 
> ---
> 
> ## Input
> 
> - Kernel: `ShareActivity`→`ShareAdapter`→`Envelope` (Room/FS/WAL), Receipts/Spans, failure taxonomy
>     
> - Android context: WorkManager; intermittent connectivity; OAuth Drive client via Google Sign-In
>     
> - Boundary: `DriveServiceFactory` + `DriveAdapter`; core logic remains pure
>     
> 
> ## Output (Essence)
> 
> - Background `UploadWorker` per `sha256`; survives reboots/process death
>     
> - Drive artifact at `mfme/ingest/YYYY/MM/<sha256>[.<ext>]` with `appProperties{sha256,bytes,mime,receivedAtUtc,idempotencyKey}`
>     
> - Verified binding in `cloud_binding(envelopeId,driveFileId,uploadedAtUtc,md5,bytes)` + typed Receipt
>     
> - History cloud chip: queued ▸ uploading ▸ uploaded ▸ failed; NDJSON telemetry ≤1 KB
>     
> 
> ## Invariants
> 
> - Exactly-once by `(sha256, folderId)`; retries collapse to same binding
>     
> - Verify remote `md5/size` before binding; no assumption of success
>     
> - UTC timestamps; lineage spans/receipts include `sha256` and `driveFileId`
>     
> - Privacy-minimal: no content in logs; only hashes/sizes/mime
>     
> - Repairable: daily reconciler proves/binds; no silent loss
>     
> 
> ## Failure Modes
> 
> - No account/revoked scope → queue pause; single sign-in prompt; backlog drains after
>     
> - Offline/flaky network → exponential backoff; default unmetered Wi-Fi constraint
>     
> - Resolver/stream error → `ResolverError`; envelope retained
>     
> - 409/412 conflict → search by `appProperties.sha256`; bind existing; `OkDuplicateUpload`
>     
> - Token stale → silent refresh; fatal → clear + prompt once
>     
> - Mid-upload death → WM resumes; idempotency prevents dupes
>     
> 
> ## Next Arrows
> 
> - Auth & adapter port (scopes, UX, `DriveServiceFactory`)
>     
> - `UploadWorker` & `CloudBinding` (stream, verify, bind, typed receipts)
>     
> - Reconciler/verify loop; conflict search by `sha256`
>     
> - UI cloud chips + receipt viewer in History
>     
> - CI property hammer: offline/auth/kill-switch chaos tests
>     
> 
> ---


---

# 道 morph / brief — **Morph X: Drive Uploader Connector (MFME-aligned)**

> **Heuristic**: States are shadows; arrows are real.  
> **Aim**: Bind every local `Envelope` to a verified Drive artifact via a pure contract + edge adapters.  
> **Values**: Determinism • Observability • Idempotency • Minimal surface • Repairability.  
> **Constraints**: No hollow outputs (no code without receipts/tests). No hidden effects (effects only via adapters).  
> **Kernel**: If the upload arrow upholds its invariants, the archive is trustworthy in motion, not just at rest.

---

## 投 (tóu) — input / casting

- Kernel in place: `ShareActivity` → `ShareAdapter` → `Envelope` (Room, FS, WAL); Receipts/Spans; failure taxonomy.
    
- Context: WorkManager available; user often offline; OAuth Drive client.
    
- Boundary: **DriveAdapter** is the sole egress; core logic remains pure.
    

---

## 果 (guǒ) — output / fruit (essence)

- **Background Uploader**: 1 WM job per new `sha256` (unique by `(sha256, folderId)`), survives reboots.
    
- **Drive artifact**: `mfme/ingest/YYYY/MM/<sha256>[.<ext>]` with `appProperties{sha256, bytes, mime, receivedAtUtc, idempotencyKey}`.
    
- **Verified binding**: On success, record `cloud_binding(envelopeId, driveFileId, uploadedAtUtc, md5, bytes)` and emit a **typed Receipt**.
    
- **Receipts**: `OkUploaded` (new), `OkDuplicateUpload` (collapsed), `PermissionDeniedAuth`, `NetworkBackoff`, `ResolverError`, `UnknownDriveError(code)`.
    
- **UI**: History chip = queued ▸ uploading ▸ uploaded ▸ failed (tap shows the Receipt trail).
    

---

## 經 (jīng) — invariants (universal properties)

1. **Exactly-once by content**: For a fixed `(sha256, folderId)`, at most one Drive object is bound; retries are observationally no-ops.
    
2. **Verification, not assumption**: Postcondition includes remote metadata check (`md5Checksum`/`size`) consistent with local bytes; binding only after verify.
    
3. **UTC fidelity & lineage**: All timestamps in UTC; every attempt yields a span+receipt with `sha256` and `driveFileId` (when known).
    
4. **Privacy minimalism**: No content in logs; only hashes/sizes/mime in telemetry.
    
5. **Deterministic paths**: Canonical foldering by month; names are content-addressed.
    
6. **Repairable flow**: Daily reconciler proves bindings (local↔remote), re-queues gaps; no silent loss.
    

---

## 絡 (luò) — failure modes (typed, repairable)

- **No account / revoked scope** → queue pauses; single prompt to sign-in; backlog drains after.
    
- **Offline / flaky network** → exponential backoff + required constraints (default unmetered Wi-Fi); spans mark `NetworkBackoff`.
    
- **Resolver/stream errors** → `ResolverError`; envelope retained.
    
- **409/412 conflicts** → treat as duplicate; search by `appProperties.sha256`, bind, emit `OkDuplicateUpload`.
    
- **Token stale** → silent refresh; on fatal, clear + prompt once.
    
- **Process death mid-upload** → WM resumes; idempotency key prevents dupes.
    

---

## 約 (yuē) — constraints

- **Adapters only at edges**: Drive access behind `DriveServiceFactory` + `DriveAdapter`; core never imports network libs.
    
- **Unique WM keys**: Enqueue keyed by `sha256`; periodic verifier job is distinct and idempotent.
    
- **Receipts ≤1 KB** and structured (arrow name, code, sha256, outcome, timings).
    
- **Scale**: History pagination tolerates ≥10k envelopes without N+1 joins.
    

---

## 器 (qì) — adapters & harness

- **DriveServiceFactory** → Drive client for selected account/scope (`Drive.SCOPE_FILE` or `appDataFolder`).
    
- **UploadWorker** → streams text/binary; sets `appProperties`; verifies metadata before binding.
    
- **CloudBindingDao** → envelopeId ↔ driveFileId + md5/bytes.
    
- **UploadScheduler** → enqueues on `SaveResult.Success`.
    
- **Reconciler** → daily, lists unbound/invalid bindings; re-attempts or re-binds by `sha256`.
    

---

## 檢 (jiǎn) — verification (properties over anecdotes)

- **Property tests** (hammer with randomness & fault injection):
    
    - _Idempotency_: repeated enqueue for same `sha256` yields ≤1 binding and stable receipts.
        
    - _Offline persistence_: cut connectivity arbitrarily; eventually `OkUploaded` without data loss.
        
    - _Auth stall/drain_: revoke scope → queue pauses; re-auth → backlog drains; ordering preserved.
        
    - _Conflict collapse_: pre-seed remote with same `sha256`; worker binds (no re-upload).
        
    - _Process death_: kill mid-stream; resume leads to single verified binding.
        
- **Chaos runs**: 1k envelope storm; device reboots; metered↔unmetered flips.
    
- **Artifacts**: NDJSON receipts; verifier logs (local vs remote deltas) checked into CI artifacts.
    

---

## Implementation notes (precise deltas)

- **Surface/auth**: “Connect Drive” card; Google Sign-In requesting minimal scope; persist chosen account only (tokens managed by Google).
    
- **Schema**: `cloud_binding(envelopeId PK, driveFileId TEXT UNIQUE, uploadedAtUtc TEXT, md5 TEXT, bytes INTEGER)`.
    
- **Triggering**: enqueue on save; periodic verifier (daily) and a manual “Verify now” action in Debug/About.
    
- **Telemetry**: span per upload attempt; invariant-violation logs are structured and receipt-linked.
    
- **Security/config**: no API key required for private uploads; keep non-secret hints in `local.properties`.
    

---

## Next arrows (split plan, 3–5 morphs)

1. **Auth & Adapter Port** — Sign-in surface, DriveServiceFactory, scopes, error UX.
    
2. **UploadWorker & Binding** — stream, verify, bind, typed receipts, WM policies.
    
3. **Reconciler** — daily verify/repair loop; conflict search by `sha256`.
    
4. **UI Cloud Chips** — history adornments + receipt viewer.
    
5. **CI Property Hammering** — offline/auth/kill-switch chaos tests + NDJSON assertions.
    

---

