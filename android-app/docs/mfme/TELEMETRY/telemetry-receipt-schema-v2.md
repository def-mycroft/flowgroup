# Telemetry Receipt Schema V2

Id: telemetry-receipt-schema-v2
Updated: 2025-09-10T19:17:49Z

Schema (v2)
- version:int (2)
- ok:boolean
- code:string (TelemetryCode wire)
- ts_utc:string (ISO-8601)
- adapter:string
- span_id:string
- envelope_id:int?  envelope_sha256:string?  message:string?

Notes
- Produced by CanonicalReceiptJson.encodeV2; one NDJSON line per receipt.
