# wrap vism core — envelope variant
import json, base64, hashlib, uuid, datetime

# morph vism Wrap v0.3.0 lowering-burl ee79ff46
VISM_CODE = "wrap"
__version__ = "0.3.1"

DOC = """Wrapper vs. Wrap (failure guard)
- Wrapper: the vism launcher/runner that handles install/dispatch.
- Wrap: the pure core here that transforms Payload → Envelope.

Input JSON (stdin)
{
  "vism": "wrap",
  "input": {
    "bytes_": BASE64,
    "media_type": "text/plain"?,
    "source": "cli"?,
    "meta": { ... }?
  }
}

Output JSON
{
  "ok": true,
  "value": {
    "id": UUID5(sha256(bytes_)),
    "content_hash_sha256": HEX,
    "created_at": ISO8601_UTC,
    "media_type": STR,
    "source": STR,
    "meta": { ... }?
  },
  "error": null,
  "receipts": {
    "wrap": "ok",
    "ports": {"clock":"utc","crypto":"sha256"}
  }
}

Failures: empty_payload, invalid_base64, input_malformed.
Determinism: identical input with fixed ports yields identical output.
CLI expectation: installed launcher should accept --input PATH --output PATH.
"""

NAMESPACE_UUID = uuid.UUID("6b7dbf3d-9a54-454a-9a0e-2b1a9d7f3a21")

def _iso_now_utc():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()

def _sha256_hex(b: bytes) -> str:
    h = hashlib.sha256()
    h.update(b)
    return h.hexdigest()

def _uuid5_from_hex(hex_digest: str) -> str:
    return str(uuid.uuid5(NAMESPACE_UUID, hex_digest))

class _WrapEnvelope:
    def apply(self, payload: dict) -> dict:
        try:
            if not isinstance(payload, dict):
                return {"ok": False, "value": None, "error": "input_malformed", "receipts": {"wrap":"error","ports":{"clock":"utc","crypto":"sha256"}}}
            inp = payload.get("input", {})
            b64 = inp.get("bytes_")
            if b64 is None:
                return {"ok": False, "value": None, "error": "input_malformed", "receipts": {"wrap":"error","ports":{"clock":"utc","crypto":"sha256"}}}
            try:
                raw = base64.b64decode(b64, validate=True)
            except Exception:
                return {"ok": False, "value": None, "error": "invalid_base64", "receipts": {"wrap":"error","ports":{"clock":"utc","crypto":"sha256"}}}
            if len(raw) == 0:
                return {"ok": False, "value": None, "error": "empty_payload", "receipts": {"wrap":"error","ports":{"clock":"utc","crypto":"sha256"}}}
            media_type = inp.get("media_type") or "application/octet-stream"
            source = inp.get("source") or "cli"
            meta = inp.get("meta")
            hex_digest = _sha256_hex(raw)
            eid = _uuid5_from_hex(hex_digest)
            created_at = _iso_now_utc()
            value = {
                "id": eid,
                "content_hash_sha256": hex_digest,
                "created_at": created_at,
                "media_type": media_type,
                "source": source,
            }
            if isinstance(meta, dict) and meta:
                value["meta"] = meta
            return {"ok": True, "value": value, "error": None, "receipts": {"wrap":"ok","ports":{"clock":"utc","crypto":"sha256"}}}
        except Exception:
            return {"ok": False, "value": None, "error": "input_malformed", "receipts": {"wrap":"error","ports":{"clock":"utc","crypto":"sha256"}}}

def factory():
    return _WrapEnvelope()

