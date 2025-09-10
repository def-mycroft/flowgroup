morph brief mfme TroubleshootFlow imbued-sycamore b5e49dc5

imbued-sycamore
b5e49dc5-b13d-4a60-891d-2c5716a238cc
2025-09-10T13:04:41.146608-06:00

***

# TroubleshootFlow — Morphism Brief (id: bug-hunt 7f2e2a1c)

**Input → Output**  
User story + observed defect + repo context → minimized repro + failing property bound to a brief → trace-linked root cause → verified fix + typed Receipt.

**Invariants**

- Every round yields a Receipt (`OkFixed|OkExplained|ErrNonRepro`) tied to `brief_id` and `property_id`.
    
- Arrow contracts govern the investigation: inputs/outputs, invariants, failure modes are explicit before patching.
    
- Properties, not anecdotes, define “broken” and “fixed”; red→green proves correctness.
    
- Core stays pure; hypotheses isolate via adapters (IO at the edges only).
    
- Telemetry provides lineage from story → repro → fix; no silent arrows.
    

**Failure Modes**  
`empty_story` (ambiguous intent), `non_reproducible`, `missing_property` (no falsifiable claim), `missing_telemetry`, `adapter_shadowing` (bug in edge, not core), `flaky_test`, `drift_from_brief` (patch not mapped to contract).

**Adapters (seams)**

- **RepoAdapter**: Git/branch/CI context; emits `case_id`, seed, revision.
    
- **ReproAdapter**: deterministic runner (seeded clocks/PRNG/fs sandbox).
    
- **TelemetryAdapter**: OpenTelemetry spans/logs linking `brief_id`, `property_set`, `trace_id`.
    
- **FixtureAdapter**: stubs/fakes for services (db/net/fs/device) to localize defect.
    
- **ReceiptEmitter**: writes typed outcomes to the doc/log surface.
    

**Properties (what must hold)**

- **Falsifiability**: A single property captures the user story; failing run reproduces it with same seed (idempotent repro).
    
- **Localization by composition**: swapping adapters narrows the fault set monotonically (core vs edge).
    
- **Fix proof**: the failing property turns green; sibling invariants remain green (no regression).
    
- **Lineage**: each run attaches spans that reference the governing brief and property digest.
    

**Telemetry (evidence trail)**  
Span schema: `{trace_id, case_id, brief_id, property_id, adapter_ids[], seed, rev, outcome, duration_ms}`; emit `start`, `invariant_fail|pass`, `patch_applied`, `receipt_emitted`. These spans are the live link between docs and behavior.

**Process (MFME loop for bugs)**

1. **Describe**: translate the story into/under an existing morphism brief or draft a new one (inputs→outputs, invariants, failures).
    
2. **Brief→Property**: encode the story as a property (what always should hold) and make it fail on current main.
    
3. **Scaffold/Adapters**: introduce fakes/stubs to reproduce deterministically and bisect core vs edge.
    
4. **Instrument**: wire spans/logs to the brief/property so runs are observable.
    
5. **Implement**: patch core (or adapter) under the contract; keep the core pure.
    
6. **Test**: property flips green locally and in CI; sibling properties stay green.
    
7. **Emit**: produce a Receipt with links to trace and doc page; merge.
    

**Composition (where it lives in the manifesto)**  
This section binds **Brief → Properties → Adapters → Telemetry → Loop** so a user story traverses a disciplined path to a fix, with documentation that is promptable and observable in production.

**Notes on “soothed-hemlock” (docs generation access)**  
Coding/design agents surface and update the MFME briefs, attach runnable properties, mark adapter seams, and link spans to briefs—so every troubleshooting pass strengthens the doc lineage and keeps specs in lock-step with code.

---