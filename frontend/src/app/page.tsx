'use client';

import { useMonitor } from '@/hooks/useMonitor';
import { UnifiedEvent } from '@/lib/types';

// ─── Sub-components ──────────────────────────────────────────────────────────

function InfraCard({ event }: { event: UnifiedEvent }) {
  return (
    <div className="infra-card">
      <span className="infra-card__source">{event.source}</span>
      <span className={`infra-card__badge badge--${event.severity}`}>{event.severity}</span>
    </div>
  );
}

function LogEntry({ event }: { event: UnifiedEvent }) {
  const ts = new Date(event.timestamp).toLocaleTimeString('es-ES', { hour12: false });
  return (
    <div className={`log-entry log-entry--${event.severity}`}>
      <div className="log-entry__meta">
        <span>{ts}</span>
        <span>{event.source}</span>
        <span className={`infra-card__badge badge--${event.severity}`}>{event.severity}</span>
      </div>
      <div className="log-entry__message">{event.message}</div>
    </div>
  );
}

// ─── Main page ───────────────────────────────────────────────────────────────

export default function DashboardPage() {
  const { state, active, start, stop } = useMonitor();

  return (
    <>
      {/* Start overlay – required to initialise AudioContext from a user gesture */}
      {!active && (
        <div className="start-overlay">
          <h1 className="start-overlay__title">Monitor Dashboard</h1>
          <p className="start-overlay__subtitle">
            Panel de monitorización de operaciones en tiempo real
          </p>
          <button className="btn btn--start btn--large" onClick={start}>
            ▶ Iniciar Monitorización
          </button>
        </div>
      )}

      <div className="dashboard">
        {/* ── Header ── */}
        <header className="header">
          <span className="header__title">Monitor · Operaciones en Tiempo Real</span>
          <div className="header__status">
            <span
              className={`status-dot ${state.connected ? 'status-dot--connected' : ''}`}
              title={state.connected ? 'Conectado' : 'Desconectado'}
            />
            <span>{state.connected ? 'CONECTADO' : 'DESCONECTADO'}</span>
            {active && (
              <button className="btn btn--stop" onClick={stop}>
                ■ Detener
              </button>
            )}
          </div>
        </header>

        {/* ── Infrastructure panel ── */}
        <section className="panel">
          <div className="panel__header">Estado de Infraestructura</div>
          <div className="panel__content">
            {state.infrastructure.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', textAlign: 'center', marginTop: '2rem' }}>
                Esperando datos de infraestructura…
              </p>
            ) : (
              state.infrastructure.map((e) => <InfraCard key={e.source} event={e} />)
            )}
          </div>
        </section>

        {/* ── Event log panel ── */}
        <section className="panel">
          <div className="panel__header">Log de Eventos en Tiempo Real</div>
          <div className="panel__content">
            {state.logs.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', textAlign: 'center', marginTop: '2rem' }}>
                Esperando eventos de datos…
              </p>
            ) : (
              state.logs.map((e, i) => <LogEntry key={`${e.timestamp}-${i}`} event={e} />)
            )}
          </div>
        </section>
      </div>
    </>
  );
}
