  
  Expanded User Story

  - Title: Share live location via periodic SMS pings while hiking
  - Actors: Hiker (sender), Friend (recipient), App system (UI, worker, adapters)
  - Goal: While hiking, automatically send a Google Maps link with current GPS location to a friend every N
  minutes while enabled; provide a one-tap “Ping self now” for testing.
  - Preconditions:
      - App installed and configured with a valid destination phone number (E.164).
      - Permissions granted: SEND_SMS; location (foreground for one-shot; background for periodic).
      - Device has intermittent GPS and SMS coverage; app should behave gracefully when coverage is missing.
  - Triggering:
      - User enables “Location Ping” and sets interval minutes (default 15).
      - Optional “Test now” triggers an immediate one-shot send.
  - Main Flow:
      - User enters a valid E.164 number and saves. -- note: just make sure that i don't have to specify in detail what the number is. need to validate nubmer on input 
      - User sets interval minutes; values under OS minimum are coerced to allowed min (15).
      - User enables pings; app schedules a periodic worker.
      - On each run, the worker acquires a fresh one-shot location, renders message (Google Maps pin +
  timestamp), persists an Envelope, sends via SMS, and emits a typed Receipt.
      - Recipient receives a message containing a Google Maps link and a timestamp.
  - Test Flows:
      - “Ping self now” sends an immediate one-shot using current config; result is shown inline (success/
  error).
      - A short-interval test mode is desired for feasibility (e.g., 1-minute) while acknowledging production
  minimum is 15 minutes.
  - Constraints and Invariants (aligned with receding-bamboo brief):
      - E.164 validation required; never enable without valid destination.
      - Periodic scheduling minimum 15 minutes (Android/WorkManager floor) in production.
      - UTC discipline for all persisted timestamps and Receipts; no main-thread I/O; no cloud egress on
  capture paths.
      - Idempotent collapse within a UTC day: identical body to same destination on same UTC day suppresses
  transport and emits OkDuplicateSuppressed.
      - Privacy-minimal: receipts don’t store raw message text; UI masks destination.
  - Message Content:
      - Google Maps link of the form https://maps.google.com/?q=<lat>,<lon>&z=16.
      - Timestamp included (format TBD; see questions on UTC vs local-of-sender).
      - Optional short label, if configured, may prefix the message (per brief).
  - Failure Handling (typed receipts):
      - permission_denied (SMS/location).
      - device_unavailable (no fix within budget or radio down).
      - empty_input, storage_quota, digest_mismatch.
      - Each attempt yields exactly one Receipt with stable fingerprint.
  - Edge Cases:
      - Out of coverage: skip send and emit device_unavailable; next run tries again.
      - Device reboots: boot receiver re-schedules if still enabled.
      - Timezone changes while traveling: stored times remain UTC; SMS body timestamp choice needs
  confirmation.
      - Duplicates: same body within the same UTC day suppresses transport.
  - Acceptance (high level):
      - With valid number and permissions, enabling pings schedules periodic sends at configured interval
  (coerced to ≥15).
      - Friend receives messages containing current location link and a timestamp while enabled.
      - “Test now” sends immediately and surfaces a typed result in UI.
      - Receipts and last ping status persist across process death and follow UTC discipline.
      - Invalid number or missing permissions keeps toggle off with clear inline guidance.

note: make sure that messages aren't send more than a couple of times within the window e..g if n=15 mins then no more htan 3 messages durinmg 15 min window 

  Key Observations from Briefs

  - The core PingNow template specifies an ISO-8601 UTC timestamp in the message body and UTC-only Receipts.
  - Frontend/Backend prompts enforce E.164 validation, min interval 15, background location permission for
  periodic worker, and “Test now” behavior.
  - Your user story requests recipient timestamps “in my local time wherever I am,” which conflicts with the
  brief’s UTC body template.

  Clarifying Questions

  1. Timestamp in SMS body: The brief mandates UTC in the message body, but your story asks for “timestamp in
  my local time wherever I am.” Should the SMS body:
      - Use UTC only (per brief),
      - Use sender’s local time only -- sender localtime 
  2. Minimum interval for periodic pings: Android’s WorkManager enforces a 15-minute minimum. For your “every
  1 minute” feasibility test, do you want: 
      - Or are manual “Test now” taps sufficient for bench testing? <- good enough
  3. Background location: Are you comfortable requiring background location permission for periodic pings,
  while keeping the one-shot “Test now” to foreground only? -- sure 
  4. Single vs multiple recipients: Should this be limited to one destination E.164 number for now, or
  support multiple recipients? -- def support multiple 
  5. Delivery receipts: Do you want us to attempt carrier delivery receipts (requires READ_SMS in some
  cases), or skip and rely on our internal Receipts only? -- no, not rn  
  provided label)? Keep to one SMS segment or okay if it spills to two? --
  7. Retry policy: If a run fails (no fix or radio down), should the worker:
      - Just log and wait for the next interval (simple),<- this for now 
  8. Auto-stop behavior: Should periodic pings auto-stop after a duration (e.g., after 24 hours) unless re-
  confirmed, or remain on until toggled off? - remain until toggled
  9. Accuracy thresholds: If the GPS fix has poor accuracy (e.g., > 100 m), still send, or fail the attempt? - still end
  10. Content privacy: Confirm that we never display or store full SMS body in-app history—only masked
  destination, UTC timestamp, and typed result. <- yes 
  11. Internationalization: Is E.164-only input with no localized formatting acceptable for v1? <- idk 
  12. “Ping self now”: Should the “self” target be the same configured number, or do you want a separate
  field for a self-test number? <- separate field for self test number. default app infers own nubmer 
  13. Distance/recency UI: The brief mentions a “History chip” with distance delta vs last fix. Do you want
  that in v1, or defer to later? <- defer
  14. Boot behavior: On device restart, re-enable scheduling if ping_enabled=true—okay to ship in v1? <- yes 
  15. Data retention: Any retention policy for Receipts/Envelopes, or keep indefinitely until user clears? <- keep indef 


