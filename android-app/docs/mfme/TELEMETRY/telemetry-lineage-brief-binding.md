# Telemetry Lineage Brief Binding

Id: telemetry-lineage-brief-binding
Updated: 2025-09-10T19:17:49Z

Current Binding
- SpanDao.bindEnvelope(spanId, envelopeId, envelopeSha256) links execution evidence to data.

Future Work
- Add brief_id/property_set fields in spans/receipts to complete lineage.

Checks
- For each arrow, ensure a span exists and binds envelope (or null on failure).
