'use client';

import { UnifiedEvent } from '@/lib/types';

export function InfraPanel({ infrastructure }: { infrastructure: UnifiedEvent[] }) {
  return (
    <section className="w-80 border-l border-border-subtle bg-[#111] flex flex-col h-full shrink-0">
      <div className="py-3 px-4 border-b border-border-subtle shrink-0">
        <h2 className="text-xs font-bold tracking-[0.15em] text-text-muted uppercase">
          Infrastructure
        </h2>
      </div>
      
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {infrastructure.length === 0 ? (
          <p className="text-center text-sm text-text-muted italic mt-10">
            Awaiting infra telemetry...
          </p>
        ) : (
          infrastructure.map((event) => <InfraCard key={event.source} event={event} />)
        )}
      </div>
    </section>
  );
}

function InfraCard({ event }: { event: UnifiedEvent }) {
  const getBadgeClass = (severity: string) => {
    switch (severity) {
      case 'INFO':
        return 'bg-[#1a3a1a] text-status-ok border-status-ok';
      case 'WARNING':
        return 'bg-[#3a2a00] text-status-warn border-status-warn';
      case 'CRITICAL':
        return 'bg-[#3a0000] text-status-crit border-status-crit animate-pulse';
      default:
        return 'bg-border-subtle text-text-primary border-transparent';
    }
  };

  return (
    <div className="flex items-center justify-between p-3 rounded-lg border border-border-subtle bg-bg-card shadow-sm">
      <span className="font-semibold text-sm truncate mr-2" title={event.source}>
        {event.source}
      </span>
      <span
        className={`px-2 py-0.5 rounded border text-[0.7rem] font-bold tracking-widest shrink-0 ${getBadgeClass(
          event.severity
        )}`}
      >
        {event.severity}
      </span>
    </div>
  );
}
