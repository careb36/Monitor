'use client';

import { useMemo } from 'react';
import { MonitorState } from '@/lib/types';

interface SidebarProps {
  state: MonitorState;
}

export function Sidebar({ state }: SidebarProps) {
  const infraCount = state.infrastructure.length;
  const logCount = state.logs.length;
  
  const critCount = useMemo(() => 
    state.logs.filter((l) => l.severity === 'CRITICAL').length 
    + state.infrastructure.filter((i) => i.severity === 'CRITICAL').length,
    [state.logs, state.infrastructure]
  );

  const warnCount = useMemo(() => 
    state.logs.filter((l) => l.severity === 'WARNING').length 
    + state.infrastructure.filter((i) => i.severity === 'WARNING').length,
    [state.logs, state.infrastructure]
  );

  return (
    <aside className="w-64 bg-bg-panel border-r border-border-subtle flex flex-col p-4 gap-6 shrink-0 h-full overflow-y-auto">
      <div className="space-y-2">
        <h2 className="text-xs font-bold text-text-muted tracking-[0.15em] uppercase">
          System Overview
        </h2>
        <div className="grid grid-cols-2 gap-2">
          <StatBox label="Infra" value={infraCount} />
          <StatBox label="Logs" value={logCount} />
        </div>
      </div>

      <div className="space-y-2">
        <h2 className="text-xs font-bold text-text-muted tracking-[0.15em] uppercase">
          Active Alerts
        </h2>
        <div className="flex flex-col gap-2">
          <AlertBox label="Critical" value={critCount} colorClass="text-status-crit border-status-crit" />
          <AlertBox label="Warning" value={warnCount} colorClass="text-status-warn border-status-warn" />
        </div>
      </div>

      <div className="mt-auto pt-4 border-t border-border-subtle text-xs text-text-muted font-mono text-center">
        v2.0 Command Center
      </div>
    </aside>
  );
}

function StatBox({ label, value }: { label: string; value: number }) {
  return (
    <div className="bg-bg-card p-3 rounded border border-border-subtle flex flex-col items-center">
      <span className="text-2xl font-mono font-bold text-text-primary">{value}</span>
      <span className="text-xs text-text-muted uppercase tracking-wider mt-1">{label}</span>
    </div>
  );
}

function AlertBox({ label, value, colorClass }: { label: string; value: number; colorClass: string }) {
  const isActive = value > 0;
  return (
    <div className={`p-3 rounded border flex justify-between items-center bg-bg-card ${isActive ? colorClass : 'border-border-subtle text-text-muted'}`}>
      <span className="text-sm font-bold uppercase tracking-wider">{label}</span>
      <span className="text-xl font-mono font-bold">{value}</span>
    </div>
  );
}
