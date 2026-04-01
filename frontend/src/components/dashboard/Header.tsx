'use client';

interface HeaderProps {
  connected: boolean;
  active: boolean;
  onStop: () => void;
}

export function Header({ connected, active, onStop }: HeaderProps) {
  return (
    <header className="flex items-center justify-between px-6 py-3 bg-[#111111] border-b border-border-subtle shrink-0">
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 rounded bg-bg-card border border-border-subtle flex items-center justify-center text-status-info font-bold">
          M
        </div>
        <h1 className="text-lg font-bold tracking-widest text-status-info uppercase">
          Command Center
        </h1>
      </div>

      <div className="flex items-center gap-4 text-sm font-semibold">
        <div className="flex items-center gap-2">
          <div
            className={`w-2.5 h-2.5 rounded-full ${
              connected ? 'bg-status-ok shadow-[0_0_8px_var(--color-status-ok)] animate-pulse' : 'bg-text-muted'
            }`}
          />
          <span className={connected ? 'text-status-ok' : 'text-text-muted'}>
            {connected ? 'LIVE' : 'OFFLINE'}
          </span>
        </div>

        {active && (
          <button
            onClick={onStop}
            className="px-3 py-1.5 border border-status-crit text-status-crit rounded hover:bg-status-crit hover:text-white transition-colors tracking-wide"
          >
            ⏹ STOP
          </button>
        )}
      </div>
    </header>
  );
}
