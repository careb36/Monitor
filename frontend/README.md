# Monitor Frontend SSE

## EN

This frontend consumes `GET /api/events/stream` using native `EventSource`.

### Resilience behavior

- Automatic browser reconnect is enabled by default.
- When backend emits SSE event IDs, browser reconnect sends `Last-Event-ID` automatically.
- UI deduplicates alerts by `eventId` (and falls back to payload fingerprint when ID is missing).
- Hook cleans all listeners on unmount to avoid memory leaks.

### Main files

- `src/hooks/useMonitor.ts` - resilient SSE hook.
- `src/lib/types.ts` - stream and alert types.
- `src/app/page.tsx` - monitor dashboard UI.

## ES

Este frontend consume `GET /api/events/stream` usando `EventSource` nativo.

### Comportamiento resiliente

- La reconexion automatica del navegador esta habilitada por defecto.
- Cuando el backend emite IDs SSE, la reconexion envia `Last-Event-ID` automaticamente.
- La UI deduplica alertas por `eventId` (y usa huella de payload si falta ID).
- El hook limpia listeners en unmount para evitar memory leaks.

### Archivos principales

- `src/hooks/useMonitor.ts` - hook SSE resiliente.
- `src/lib/types.ts` - tipos de stream y alertas.
- `src/app/page.tsx` - UI del dashboard.

