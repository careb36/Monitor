'use client';

import { ConnectionStatus } from '@/lib/types';

interface HeaderProps {
  connected: ConnectionStatus;
  active: boolean;
  onStop: () => void;
}

const STATUS_CONFIG: Record<ConnectionStatus, { dot: string; label: string; text: string }> = {
  CONNECTED:    { dot: 'bg-status-ok shadow-[0_0_8px_var(--color-status-ok)] animate-pulse', label: 'LIVE',         text: 'text-status-ok' },
  CONNECTING:   { dot: 'bg-status-warn animate-pulse',                                        label: 'CONNECTING…',  text: 'text-status-warn' },
  STALE:        { dot: 'bg-status-warn',                                                      label: 'STALE',        text: 'text-status-warn' },
  DISCONNECTED: { dot: 'bg-text-muted',                                                       label: 'OFFLINE',      text: 'text-text-muted' },
};

export function Header({ connected, active, onStop }: HeaderProps) {
  const { dot, label, text } = STATUS_CONFIG[connected] ?? STATUS_CONFIG.DISCONNECTED;
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
          <div className={`w-2.5 h-2.5 rounded-full ${dot}`} />
          <span className={text}>{label}</span>
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
