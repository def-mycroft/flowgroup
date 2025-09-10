# Telemetry Span Entity

Id: telemetry-span-entity
Updated: 2025-09-10T19:17:49Z

Model
- spanId:string, adapter:string, startNanos:long, endNanos:long,
  envelopeId:long?, envelopeSha256:string?

Lifecycle
- begin(adapter) → insert start; end(span) → update end; bindEnvelope(spanId, id, sha).

Lineage
- Use spanId to correlate receipts and envelope persistence.
