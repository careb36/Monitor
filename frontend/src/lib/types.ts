export type Severity = 'INFO' | 'WARNING' | 'CRITICAL';
export type EventType = 'DATA' | 'INFRASTRUCTURE';
export type ConnectionStatus = 'CONNECTED' | 'CONNECTING' | 'STALE' | 'DISCONNECTED';

export interface UnifiedEvent {
  type: EventType;
  severity: Severity;
  source: string;
  message: string;
  timestamp: string;
  receivedAt?: number; // Local timestamp when event was received by frontend
}

export interface MonitorState {
  infrastructure: UnifiedEvent[];
  logs: UnifiedEvent[];
  connected: ConnectionStatus;
  lastEventTimestamp: number;
}

