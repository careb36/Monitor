'use client';

import { useState } from 'react';
import { UnifiedEvent } from '@/lib/types';

export function LogViewer({ logs }: { logs: UnifiedEvent[] }) {
  const [filter, setFilter] = useState<string | null>(null);

  const filteredLogs = filter ? logs.filter(l => l.severity === filter) : logs;

  return (
    <section className="flex flex-col h-full bg-bg-base flex-1 overflow-hidden">
      <div className="flex items-center justify-between py-3 px-6 border-b border-border-subtle bg-bg-panel shrink-0">
        <h2 className="text-xs font-bold tracking-[0.15em] text-text-muted uppercase">
          Live Event Stream
        </h2>
        <div className="flex gap-2">
          <FilterButton current={filter} target={null} label="ALL" onClick={setFilter} />
          <FilterButton current={filter} target="INFO" label="INFO" onClick={setFilter} />
          <FilterButton current={filter} target="WARNING" label="WARN" onClick={setFilter} />
          <FilterButton current={filter} target="CRITICAL" label="CRIT" onClick={setFilter} />
        </div>
      </div>
      
      <div className="flex-1 overflow-y-auto p-6 space-y-2 font-mono">
        {filteredLogs.length === 0 ? (
          <p className="text-center text-sm text-text-muted italic mt-20">
            {logs.length === 0 ? 'Awaiting data stream...' : 'No logs match the current filter.'}
          </p>
        ) : (
          filteredLogs.map((event, idx) => (
            <LogEntry key={`${event.timestamp}-${idx}`} event={event} />
          ))
        )}
      </div>
    </section>
  );
}

function FilterButton({ current, target, label, onClick }: { current: string | null, target: string | null, label: string, onClick: (v: string | null) => void }) {
  const isActive = current === target;
  
  let activeClass = 'bg-border-subtle text-white';
  if (isActive) {
    if (target === 'INFO') activeClass = 'bg-status-info text-black';
    else if (target === 'WARNING') activeClass = 'bg-status-warn text-black';
    else if (target === 'CRITICAL') activeClass = 'bg-status-crit text-white';
    else activeClass = 'bg-white text-black'; // ALL
  }

  return (
    <button 
      onClick={() => onClick(target)}
      className={`px-3 py-1 rounded text-xs font-bold tracking-wider border border-border-subtle transition-colors hover:brightness-110 ${isActive ? activeClass : 'text-text-muted hover:text-white hover:bg-border-subtle'}`}
    >
      {label}
    </button>
  );
}

function LogEntry({ event }: { event: UnifiedEvent }) {
  const ts = new Date(event.timestamp).toLocaleTimeString('es-ES', { hour12: false });
  
  const getBorderClass = (severity: string) => {
    switch (severity) {
      case 'INFO': return 'border-status-info';
      case 'WARNING': return 'border-status-warn';
      case 'CRITICAL': return 'border-status-crit';
      default: return 'border-border-subtle';
    }
  };

  const getBadgeClass = (severity: string) => {
    switch (severity) {
      case 'INFO': return 'bg-[#0a2a3a] text-status-info';
      case 'WARNING': return 'bg-[#3a2a00] text-status-warn';
      case 'CRITICAL': return 'bg-[#3a0000] text-status-crit';
      default: return 'bg-border-subtle text-text-primary';
    }
  };

  return (
    <div className={`p-3 bg-bg-card border-l-[3px] rounded-r-md flex flex-col gap-1 text-sm shadow-sm transition-all animate-in fade-in slide-in-from-top-2 duration-300 ${getBorderClass(event.severity)}`}>
      <div className="flex items-center gap-3 text-xs text-text-muted font-bold">
        <span className="text-white/60">{ts}</span>
        <span className="truncate max-w-[200px] text-white/80">{event.source}</span>
        <span className={`px-1.5 py-0.5 rounded text-[0.65rem] tracking-widest uppercase ${getBadgeClass(event.severity)}`}>
          {event.severity}
        </span>
      </div>
      <div className="text-text-primary whitespace-pre-wrap break-words leading-relaxed">
        {event.message}
      </div>
    </div>
  );
}
