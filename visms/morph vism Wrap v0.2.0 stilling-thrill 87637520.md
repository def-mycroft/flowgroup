morph vism Wrap v0.2.0 stilling-thrill 87637520

stilling-thrill
87637520-4484-4c6a-b458-bad529e0106b
2025-08-31T08:59:07.783729-06:00

***

# morph/brief 


# 冊 docs 
## Wrap Vism — Morph Brief 冊 documentation

### Using the Wrap Vism

Wrap is a CLI morphism with a **pure core**. You stream JSON on `stdin`
and receive an `**Outcome<Envelope>**` JSON on `stdout`. Side-effects
(storage, logging) live behind adapters; the core is deterministic
**given fixed ports** (Clock, Crypto).

**Input format**

```json
{
  "vism": "wrap",
  "input": {
    "bytes_": "BASE64_ENCODED",
    "media_type": "text/plain",
    "source": "cli"
  }
}
````

Notes: `media_type` defaults to `"application/octet-stream"` if omitted.

**Run example**

```bash
echo '{"vism":"wrap","input":{"bytes_":"YWJj","media_type":"text/plain","source":"cli"}}' \
  | python vism_wrap.py
```

**Output shape**

```json
{
  "ok": true,
  "value": {
    "id": "UUID",
    "content_hash_sha256": "sha256...",
    "created_at": "2025-08-28T19:12:34.567890+00:00",
    "media_type": "text/plain",
    "source": "cli"
  },
  "error": null,
  "receipts": {
    "wrap": "ok",
    "ports": {"clock": "utc", "crypto": "sha256"}
  }
}
```

On error, `ok` is `false`, `value` is `null`, and `error` is one of:  
`"empty_payload"`, `"invalid_base64"`, or `"input_malformed"` (reserved  
for non-conforming JSON).

---

### Morph Discipline

- **Arrows > states**: explicit arrow **Payload → Envelope**; states are logs.
    
- **Purity**: with identical `input` and fixed **ports** (Clock, Crypto),  
    `id`/`hash`/`created_at` are deterministic.
    
- **Properties**: `hash_is_deterministic`, `rejects_empty`, `idempotent_on_hash`  
    are provable without I/O.
    
- **Ports**: Crypto (`sha256`), Clock (UTC). All side-effects are adapters.
    
- **Telemetry**: every run yields receipts (at minimum `{wrap:"ok"}`) plus a  
    ports echo for audit/lineage.
    

---

### Generating Docs

The Wrap vism can emit its own documentation (doc-first doctrine).

```bash
python vism_wrap.py --document -o ./docs/
```

This produces a morph-brief block: inputs, outputs, invariants, failure modes.

---

### Working Example

Minimal example wrapping `"hello world"` (base64: `aGVsbG8gd29ybGQ=`):

**Input JSON (stdin):**

```json
{
  "vism": "wrap",
  "input": {
    "bytes_": "aGVsbG8gd29ybGQ=",
    "media_type": "text/plain",
    "source": "example"
  }
}
```

**Run command:**

```bash
echo '{"vism":"wrap","input":{"bytes_":"aGVsbG8gd29ybGQ=","media_type":"text/plain","source":"example"}}' \
  | python vism_wrap.py
```

**Formatted output (example):**

```json
{
  "ok": true,
  "value": {
    "id": "e2a7c1d0-3f4b-4d2a-8a77-91bb1a0c64e2",
    "content_hash_sha256": "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
    "created_at": "2025-08-28T21:01:33.847019+00:00",
    "media_type": "text/plain",
    "source": "example"
  },
  "error": null,
  "receipts": {
    "wrap": "ok",
    "ports": {"clock": "utc", "crypto": "sha256"}
  }
}
```

This shows `"hello world"` captured as an immutable **Envelope** with a unique  
`id`, deterministic `content_hash_sha256`, and UTC `created_at`.


***
# Python vism Code 



***