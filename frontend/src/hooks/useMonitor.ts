'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { MonitorState, UnifiedEvent } from '@/lib/types';

const SSE_URL = '/api/events/stream';
const MAX_LOG_ENTRIES = 100;

/**
 * Custom hook that manages the EventSource (SSE) connection to the Spring Boot
 * backend.  It separates incoming events into infrastructure status and data
 * log entries, updating the shared monitor state accordingly.
 *
 * The connection is only opened after the user clicks "Start Monitoring" so
 * that the AudioContext can be created in a user-gesture callback.
 */
export function useMonitor() {
  const [state, setState] = useState<MonitorState>({
    infrastructure: [],
    logs: [],
    connected: false,
  });
  const [active, setActive] = useState(false);

  const esRef = useRef<EventSource | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);

  // ── Audio helpers ────────────────────────────────────────────────────────
  const initAudio = useCallback(() => {
    if (!audioCtxRef.current) {
      audioCtxRef.current = new AudioContext();
    }
  }, []);

  const playCriticalAlert = useCallback(() => {
    const ctx = audioCtxRef.current;
    if (!ctx) return;

    // Generate a short 880 Hz beep sequence using the Web Audio API
    const playBeep = (startTime: number) => {
      const oscillator = ctx.createOscillator();
      const gainNode = ctx.createGain();
      oscillator.connect(gainNode);
      gainNode.connect(ctx.destination);

      oscillator.type = 'square';
      oscillator.frequency.setValueAtTime(880, startTime);
      gainNode.gain.setValueAtTime(0.3, startTime);
      gainNode.gain.exponentialRampToValueAtTime(0.001, startTime + 0.25);

      oscillator.start(startTime);
      oscillator.stop(startTime + 0.25);
    };

    const now = ctx.currentTime;
    playBeep(now);
    playBeep(now + 0.35);
    playBeep(now + 0.7);
  }, []);

  // ── Event handling ───────────────────────────────────────────────────────
  const handleEvent = useCallback(
    (event: UnifiedEvent) => {
      if (event.severity === 'CRITICAL') {
        playCriticalAlert();
      }

      setState((prev) => {
        if (event.type === 'INFRASTRUCTURE') {
          // Replace the status entry for this source (keep latest per source)
          const others = prev.infrastructure.filter((e) => e.source !== event.source);
          return {
            ...prev,
            infrastructure: [event, ...others],
          };
        } else {
          // Prepend to the log, capped at MAX_LOG_ENTRIES
          const updated = [event, ...prev.logs].slice(0, MAX_LOG_ENTRIES);
          return { ...prev, logs: updated };
        }
      });
    },
    [playCriticalAlert],
  );

  // ── SSE connection lifecycle ─────────────────────────────────────────────
  useEffect(() => {
    if (!active) return;

    initAudio();

    const es = new EventSource(SSE_URL);
    esRef.current = es;

    es.onopen = () => setState((s) => ({ ...s, connected: true }));
    es.onerror = () => setState((s) => ({ ...s, connected: false }));

    // The backend sends events with name "infrastructure" or "data"
    es.addEventListener('infrastructure', (e: MessageEvent) => {
      try {
        handleEvent(JSON.parse(e.data) as UnifiedEvent);
      } catch (err) {
        console.warn('[useMonitor] Malformed infrastructure event payload:', err, e.data);
      }
    });

    es.addEventListener('data', (e: MessageEvent) => {
      try {
        handleEvent(JSON.parse(e.data) as UnifiedEvent);
      } catch (err) {
        console.warn('[useMonitor] Malformed data event payload:', err, e.data);
      }
    });

    return () => {
      es.close();
      setState((s) => ({ ...s, connected: false }));
    };
  }, [active, handleEvent, initAudio]);

  const start = useCallback(() => setActive(true), []);
  const stop = useCallback(() => {
    setActive(false);
    esRef.current?.close();
    esRef.current = null;
  }, []);

  return { state, active, start, stop };
}
