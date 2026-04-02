'use client';

import { useMemo, useState } from 'react';
import { UnifiedEvent } from '@/lib/types';

export function LogViewer({ logs }: { logs: UnifiedEvent[] }) {
  const [severityFilter, setSeverityFilter] = useState<string | null>(null);
  const [textSearch, setTextSearch] = useState('');
  const [paused, setPaused] = useState(false);
  const [pausedSnapshot, setPausedSnapshot] = useState<UnifiedEvent[] | null>(null);
  const [clearCutoffMs, setClearCutoffMs] = useState<number | null>(null);

  // Hide all entries up to the moment the operator pressed CLEAR.
  const visibleLogs = useMemo(() => {
    if (!clearCutoffMs) return logs;
    return logs.filter((event) => Date.parse(event.timestamp) > clearCutoffMs);
  }, [logs, clearCutoffMs]);

  // Freeze list when paused so incoming events do not update current view.
  const displayLogs = paused ? pausedSnapshot ?? visibleLogs : visibleLogs;

  const filteredLogs = useMemo(() => {
    return displayLogs
      .filter((l) => (severityFilter ? l.severity === severityFilter : true))
      .filter((l) => {
        if (!textSearch.trim()) return true;
        const searchLower = textSearch.toLowerCase();
        return (
          l.message.toLowerCase().includes(searchLower) ||
          l.source.toLowerCase().includes(searchLower)
        );
      });
  }, [displayLogs, severityFilter, textSearch]);

  const handleClear = () => {
    setClearCutoffMs(Date.now());
    if (paused) {
      setPausedSnapshot([]);
    }
  };

  const handleTogglePaused = () => {
    if (!paused) {
      setPausedSnapshot(visibleLogs);
      setPaused(true);
      return;
    }

    setPaused(false);
    setPausedSnapshot(null);
  };

  const bufferedCount = paused ? Math.max(visibleLogs.length - (pausedSnapshot?.length ?? 0), 0) : 0;

  return (
    <section className="flex flex-col h-full bg-bg-base flex-1 overflow-hidden" aria-labelledby="log-viewer-title">
      {/* Header with filters + pause/search/clear */}
      <div className="flex flex-col gap-3 py-3 px-6 border-b border-border-subtle bg-bg-panel shrink-0">
        <div className="flex items-center justify-between">
          <h2 id="log-viewer-title" className="text-xs font-bold tracking-[0.15em] text-text-muted uppercase">
            Live Event Stream
          </h2>
          <div className="flex gap-2">
            <button
              onClick={handleTogglePaused}
              aria-label={paused ? 'Resume live stream' : 'Pause live stream'}
              aria-pressed={paused}
              className={`px-3 py-1 rounded text-xs font-bold tracking-wider border transition-colors ${
                paused
                  ? 'bg-status-warn text-black border-status-warn'
                  : 'bg-border-subtle text-text-muted hover:text-white hover:bg-border-subtle border-border-subtle'
              }`}
            >
              {paused ? '⏸ PAUSED' : '▶ LIVE'}
            </button>
            <button
              onClick={handleClear}
              aria-label="Clear all current logs"
              className="px-3 py-1 rounded text-xs font-bold tracking-wider border border-border-subtle text-text-muted hover:text-white hover:bg-border-subtle transition-colors"
            >
              CLEAR
            </button>
          </div>
        </div>

        {/* Severity filters */}
        <div className="flex gap-2">
          <FilterButton current={severityFilter} target={null} label="ALL" onClick={setSeverityFilter} />
          <FilterButton current={severityFilter} target="INFO" label="INFO" onClick={setSeverityFilter} />
          <FilterButton current={severityFilter} target="WARNING" label="WARN" onClick={setSeverityFilter} />
          <FilterButton current={severityFilter} target="CRITICAL" label="CRIT" onClick={setSeverityFilter} />

          {/* Text search input */}
          <input
            type="text"
            placeholder="Search message or source..."
            value={textSearch}
            onChange={(e) => setTextSearch(e.target.value)}
            className="ml-auto px-3 py-1 rounded text-xs bg-bg-card text-text-primary border border-border-subtle placeholder-text-muted focus:outline-none focus:ring-1 focus:ring-status-info"
          />
        </div>
      </div>

      {/* Log entries */}
      <div className="flex-1 overflow-y-auto p-6 space-y-2 font-mono">
        {filteredLogs.length === 0 ? (
          <p className="text-center text-sm text-text-muted italic mt-20">
            {logs.length === 0
              ? 'Awaiting data stream...'
              : textSearch.trim()
              ? 'No logs match the search.'
              : severityFilter
              ? 'No logs match the current severity filter.'
              : 'No logs yet.'}
          </p>
        ) : (
          filteredLogs.map((event, idx) => <LogEntry key={`${event.timestamp}-${idx}`} event={event} />)
        )}
      </div>

      {/* Status indicator when paused */}
      {paused && (
        <div className="px-6 py-2 bg-status-warn bg-opacity-10 border-t border-status-warn text-xs text-status-warn font-bold uppercase tracking-wider">
          ⏸ Paused - {bufferedCount} new events buffered. Press LIVE to resume.
        </div>
      )}
    </section>
  );
}

function FilterButton({
  current,
  target,
  label,
  onClick,
}: {
  current: string | null;
  target: string | null;
  label: string;
  onClick: (v: string | null) => void;
}) {
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
      className={`px-3 py-1 rounded text-xs font-bold tracking-wider border border-border-subtle transition-colors hover:brightness-110 ${
        isActive ? activeClass : 'text-text-muted hover:text-white hover:bg-border-subtle'
      }`}
    >
      {label}
    </button>
  );
}

function LogEntry({ event }: { event: UnifiedEvent }) {
  const ts = new Date(event.timestamp).toLocaleTimeString('es-ES', { hour12: false });

  const getBorderClass = (severity: string) => {
    switch (severity) {
      case 'INFO':
        return 'border-status-info';
      case 'WARNING':
        return 'border-status-warn';
      case 'CRITICAL':
        return 'border-status-crit';
      default:
        return 'border-border-subtle';
    }
  };

  const getBadgeClass = (severity: string) => {
    switch (severity) {
      case 'INFO':
        return 'bg-[#0a2a3a] text-status-info';
      case 'WARNING':
        return 'bg-[#3a2a00] text-status-warn';
      case 'CRITICAL':
        return 'bg-[#3a0000] text-status-crit';
      default:
        return 'bg-border-subtle text-text-primary';
    }
  };

  return (
    <div
      className={`p-3 bg-bg-card border-l-[3px] rounded-r-md flex flex-col gap-1 text-sm shadow-sm transition-all animate-in fade-in slide-in-from-top-2 duration-300 ${getBorderClass(event.severity)}`}
    >
      <div className="flex items-center gap-3 text-xs text-text-muted font-bold">
        <span className="text-white/60">{ts}</span>
        <span className="truncate max-w-[200px] text-white/80">{event.source}</span>
        <span className={`px-1.5 py-0.5 rounded text-[0.65rem] tracking-widest uppercase ${getBadgeClass(event.severity)}`}>
          {event.severity}
        </span>
      </div>
      <div className="text-text-primary whitespace-pre-wrap break-words leading-relaxed">{event.message}</div>
    </div>
  );
}
