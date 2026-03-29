'use client';

import { useMonitor } from '@/hooks/useMonitor';
import { ConnectionStatus, CriticalAlert } from '@/lib/types';

/**
 * EN: Banner that reflects SSE connection health for operators.
 * ES: Banner que refleja la salud de la conexion SSE para operadores.
 */
function ConnectionBadge({ status }: { status: ConnectionStatus }) {
  const visual =
    status === 'OPEN'
      ? {
          dot: 'bg-emerald-500',
          panel: 'border-emerald-400/60 bg-emerald-900/25',
          label: 'Online',
          subtitle: 'Conectado / Connected',
        }
      : {
          dot: 'bg-amber-500',
          panel: 'border-amber-400/60 bg-amber-900/25',
          label: 'Reconnecting',
          subtitle: 'Reconectando / Reconnecting',
        };

  return (
    <div className={`mb-4 rounded-xl border px-4 py-3 text-sm text-slate-100 ${visual.panel}`}>
      <div className="flex items-center gap-3">
        <span className={`inline-block h-2.5 w-2.5 rounded-full ${visual.dot}`} aria-hidden />
        <strong>{visual.label}</strong>
        <span className="text-slate-300">{visual.subtitle}</span>
      </div>
    </div>
  );
}

/**
 * EN: Card for a single critical alert from SSE stream.
 * ES: Tarjeta para una alerta critica individual del stream SSE.
 */
function AlertCard({ alert }: { alert: CriticalAlert }) {
  const ts = new Date(alert.event.timestamp).toLocaleString('es-ES', { hour12: false });

  return (
    <article className="rounded-xl border border-rose-500/50 bg-rose-950/30 p-4 shadow-sm">
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-semibold text-rose-100">{alert.event.source}</h3>
        <span className="text-xs text-rose-200">{ts}</span>
      </div>
      <p className="text-sm text-rose-50">{alert.event.message}</p>
      <p className="mt-2 text-xs text-rose-200">
        eventId: <code>{alert.id}</code>
      </p>
    </article>
  );
}

/**
 * EN: Main dashboard consuming useMonitor resilient SSE hook.
 * ES: Dashboard principal consumiendo el hook SSE resiliente useMonitor.
 */
export function MonitorDashboard() {
  const { status, alerts, lastEventId, active, start, stop, clearAlerts } = useMonitor();

  return (
    <main className="mx-auto flex w-full max-w-5xl flex-col gap-4 p-4 text-slate-100">
      {!active && (
        <section className="rounded-2xl border border-slate-700 bg-slate-900/70 p-6">
          <h1 className="mb-2 text-2xl font-bold">Monitor Dashboard</h1>
          <p className="mb-4 text-slate-300">
            EN: Start the stream to receive critical alerts in real time.
            <br />
            ES: Inicia el stream para recibir alertas criticas en tiempo real.
          </p>
          <button
            type="button"
            onClick={start}
            className="rounded-lg bg-indigo-500 px-4 py-2 font-semibold text-white hover:bg-indigo-400"
          >
            Start Stream / Iniciar Stream
          </button>
        </section>
      )}

      <section className="rounded-2xl border border-slate-700 bg-slate-900/70 p-4">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">Critical Alerts / Alertas Criticas</h2>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={clearAlerts}
              className="rounded-md border border-slate-600 px-3 py-1.5 text-sm hover:bg-slate-800"
            >
              Clear / Limpiar
            </button>
            <button
              type="button"
              onClick={stop}
              className="rounded-md border border-slate-600 px-3 py-1.5 text-sm hover:bg-slate-800"
            >
              Stop / Detener
            </button>
          </div>
        </div>

        <ConnectionBadge status={status} />

        <div className="mb-4 rounded-lg border border-slate-700 bg-slate-950/50 p-3 text-xs text-slate-300">
          Last-Event-ID: <code>{lastEventId ?? 'n/a'}</code>
        </div>

        <div className="grid gap-3">
          {alerts.length === 0 ? (
            <p className="rounded-lg border border-dashed border-slate-700 p-4 text-sm text-slate-400">
              EN: Waiting for critical SSE events...
              <br />
              ES: Esperando eventos criticos SSE...
            </p>
          ) : (
            alerts.map((alert) => <AlertCard key={alert.id} alert={alert} />)
          )}
        </div>
      </section>
    </main>
  );
}

