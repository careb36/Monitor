'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { ConnectionStatus, MonitorState, UnifiedEvent } from '@/lib/types';

const SSE_URL = '/api/events/stream';
const MAX_LOG_ENTRIES = 100;
const STALE_THRESHOLD_MS = 15_000;
const RECONNECT_THRESHOLD_MS = 30_000;

export function useMonitor() {
  const [state, setState] = useState<MonitorState>({
    infrastructure: [],
    logs: [],
    connected: 'DISCONNECTED',
  });
  const [active, setActive] = useState(false);
  const [reconnectKey, setReconnectKey] = useState(0);

  const esRef = useRef<EventSource | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const lastEventRef = useRef<number>(0);
  const staleTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const reconnectingRef = useRef(false);
  const lastReconnectAttemptRef = useRef(0);

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

      lastEventRef.current = Date.now();
      setState((prev) => {
        const base: MonitorState = {
          ...prev,
          connected: 'CONNECTED' as ConnectionStatus,
        };
        if (event.type === 'INFRASTRUCTURE') {
          const others = base.infrastructure.filter((e) => e.source !== event.source);
          return { ...base, infrastructure: [{ ...event, receivedAt: Date.now() }, ...others] };
        } else {
          const updated = [event, ...base.logs].slice(0, MAX_LOG_ENTRIES);
          return { ...base, logs: updated };
        }
      });
    },
    [playCriticalAlert],
  );

  // ── Heartbeat checker — STALE at 15s, reconnect at 30s ──────────────────
  useEffect(() => {
    if (!active) return;

    staleTimerRef.current = setInterval(() => {
      if (lastEventRef.current === 0) return;
      const elapsed = Date.now() - lastEventRef.current;
      if (elapsed >= RECONNECT_THRESHOLD_MS) {
        const now = Date.now();
        const shouldRetry =
          !reconnectingRef.current || now - lastReconnectAttemptRef.current >= RECONNECT_THRESHOLD_MS;
        if (shouldRetry) {
          reconnectingRef.current = true;
          lastReconnectAttemptRef.current = now;
          setReconnectKey((k) => k + 1);
          setState((s) => ({ ...s, connected: 'CONNECTING' }));
        }
      } else if (elapsed >= STALE_THRESHOLD_MS) {
        setState((s) => (s.connected === 'CONNECTED' ? { ...s, connected: 'STALE' } : s));
      }
    }, 1_000);

    return () => {
      if (staleTimerRef.current) {
        clearInterval(staleTimerRef.current);
        staleTimerRef.current = null;
      }
    };
  }, [active]);

  // ── SSE connection lifecycle ─────────────────────────────────────────────
  useEffect(() => {
    if (!active) return;

    initAudio();

    const es = new EventSource(SSE_URL);
    esRef.current = es;

    es.onopen = () => {
      lastEventRef.current = Date.now();
      reconnectingRef.current = false;
      setState((s) => ({ ...s, connected: 'CONNECTED' }));
    };
    es.onerror = () =>
      setState((s) => (s.connected === 'CONNECTING' ? s : { ...s, connected: 'DISCONNECTED' }));

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
    };
  }, [active, reconnectKey, handleEvent, initAudio]);

  const start = useCallback(() => setActive(true), []);
  const stop = useCallback(() => {
    setActive(false);
    if (staleTimerRef.current) {
      clearInterval(staleTimerRef.current);
      staleTimerRef.current = null;
    }
    esRef.current?.close();
    esRef.current = null;
    lastEventRef.current = 0;
    setState((s) => ({ ...s, connected: 'DISCONNECTED' }));
  }, []);

  return { state, active, start, stop };
}
