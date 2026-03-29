# ADR 0003: Resilient ingestion pipeline with critical outbox

## Status
Accepted

## Context
`Monitor` receives events from polling and Kafka, then delivers them to SSE clients and email.
Critical alerts must not be lost when SMTP fails or SSE clients disconnect.
The previous design coupled producers directly to `EventBus` and `EmailService`, making retries and backpressure policies hard to enforce uniformly.

## Decision
Introduce a unified ingestion port and a resilient pipeline:

1. Producers publish through `IngestionFacade` with `IngestMetadata`.
2. `ReliableEventPipelineService` performs:
   - TTL deduplication (`EventDeduplicator`)
   - priority routing (standard queue and critical queue)
   - scheduled dispatch and replay.
3. Split channel delivery with `EventNotifier` implementations:
   - `SseEventNotifier`
   - `EmailEventNotifier`.
4. Persist critical events in `CriticalOutbox` before channel delivery.
5. Retry failed critical email delivery with exponential backoff.

## Consequences

### Positive
- Pull and push sources are decoupled from delivery channels.
- Critical events survive temporary SMTP outages and are replayed.
- Alert storm noise is reduced with deduplication.
- Backpressure policy can drop low-priority events without losing critical ones.

### Negative
- More moving parts and scheduler-driven behavior.
- Current outbox implementation is in-memory and does not survive process restarts.

## Follow-up
- Replace `InMemoryCriticalOutbox` with persistent storage (RDBMS outbox table).
- Add delivery metrics (`queue depth`, `retry count`, `dedup hits`, `latency`).
- Add `Last-Event-ID` support for SSE replay windows.

