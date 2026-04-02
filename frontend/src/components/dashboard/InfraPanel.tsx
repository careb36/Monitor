'use client';

import { useEffect, useState } from 'react';
import { UnifiedEvent } from '@/lib/types';

const OFFLINE_THRESHOLD_MS = 60000; // 60 seconds

function formatTimeSince(receivedAt: number | undefined, now: number): string {
  if (!receivedAt) return 'Never';
  const elapsed = now - receivedAt;
  const seconds = Math.floor(elapsed / 1000);
  if (seconds < 60) {
    return `${seconds}s ago`;
  }
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    return `${minutes}m ago`;
  }
  const hours = Math.floor(minutes / 60);
  return `${hours}h ago`;
}

function isOffline(receivedAt: number | undefined, now: number): boolean {
  if (!receivedAt) return true;
  return now - receivedAt > OFFLINE_THRESHOLD_MS;
}

export function InfraPanel({ infrastructure }: { infrastructure: UnifiedEvent[] }) {
  // Local state to force re-render every few seconds for relative time updates
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 5000);
    return () => clearInterval(timer);
  }, []);

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
          infrastructure.map((event) => <InfraCard key={event.source} event={event} now={now} />)
        )}
      </div>
    </section>
  );
}

function InfraCard({ event, now }: { event: UnifiedEvent; now: number }) {
  const offline = isOffline(event.receivedAt, now);
  const timeSince = formatTimeSince(event.receivedAt, now);

  const getBadgeClass = (severity: string) => {
    // Si está offline, mostrar badge gris
    if (offline) {
      return 'bg-[#1a1a1a] text-[#666] border-[#333]';
    }

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

  const severityText = offline ? 'OFFLINE' : event.severity;

  return (
    <div className={`flex flex-col p-3 rounded-lg border shadow-sm ${offline ? 'bg-[#0a0a0a] border-[#222]' : 'border-border-subtle bg-bg-card'}`}>
      <div className="flex items-center justify-between mb-2">
        <span className="font-semibold text-sm truncate mr-2" title={event.source}>
          {event.source}
        </span>
        <span
          className={`px-2 py-0.5 rounded border text-[0.7rem] font-bold tracking-widest shrink-0 ${getBadgeClass(event.severity)}`}
        >
          {severityText}
        </span>
      </div>
      <span className={`text-xs ${offline ? 'text-[#555]' : 'text-text-muted'}`}>
        Last seen: {timeSince}
      </span>
    </div>
  );
}
