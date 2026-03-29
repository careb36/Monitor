'use client';

import { ConnectionStatus } from '@/lib/types';

/**
 * EN: Props for ConnectionBadge component showing SSE stream health.
 * ES: Props para componente ConnectionBadge mostrando salud del stream SSE.
 */
interface ConnectionBadgeProps {
  /** EN: Current connection state. ES: Estado actual de la conexion. */
  status: ConnectionStatus;
}

/**
 * EN: Visual badge reflecting SSE connection health for operators.
 * ES: Badge visual que refleja la salud SSE para operadores.
 */
export function ConnectionBadge({ status }: ConnectionBadgeProps) {
  const visual =
    status === 'OPEN'
      ? {
          dot: 'bg-emerald-500',
          panel: 'border-emerald-400/60 bg-emerald-900/25',
          label: 'Online',
          subtitle: 'Conectado / Connected',
        }
      : status === 'CONNECTING'
        ? {
            dot: 'bg-amber-500 animate-pulse',
            panel: 'border-amber-400/60 bg-amber-900/25',
            label: 'Reconnecting',
            subtitle: 'Reconectando / Reconnecting',
          }
        : {
            dot: 'bg-rose-500',
            panel: 'border-rose-400/60 bg-rose-900/25',
            label: 'Offline',
            subtitle: 'Desconectado / Disconnected',
          };

  return (
    <div
      className={`mb-4 rounded-xl border px-4 py-3 text-sm text-slate-100 ${visual.panel}`}
      role="status"
      aria-live="polite"
    >
      <div className="flex items-center gap-3">
        <span
          className={`inline-block h-2.5 w-2.5 rounded-full ${visual.dot}`}
          aria-hidden
        />
        <strong>{visual.label}</strong>
        <span className="text-slate-300">{visual.subtitle}</span>
      </div>
    </div>
  );
}

