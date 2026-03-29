'use client';

import { CriticalAlert } from '@/lib/types';

/**
 * EN: Props for CriticalAlertCard component.
 * ES: Props para componente CriticalAlertCard.
 */
interface AlertCardProps {
  /** EN: Critical alert to render. ES: Alerta critica a renderizar. */
  alert: CriticalAlert;
}

/**
 * EN: Card for a single critical alert from SSE stream.
 * ES: Tarjeta para una alerta critica individual del stream SSE.
 */
export function AlertCard({ alert }: AlertCardProps) {
  const ts = new Date(alert.event.timestamp).toLocaleString('es-ES', {
    hour12: false,
  });

  return (
    <article
      className="rounded-xl border border-rose-500/50 bg-rose-950/30 p-4 shadow-sm"
      data-testid={`alert-card-${alert.id}`}
    >
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-semibold text-rose-100">{alert.event.source}</h3>
        <span className="text-xs text-rose-200">{ts}</span>
      </div>
      <p className="text-sm text-rose-50">{alert.event.message}</p>
      <p className="mt-2 text-xs text-rose-200">
        eventId: <code className="text-rose-300">{alert.id}</code>
      </p>
    </article>
  );
}

