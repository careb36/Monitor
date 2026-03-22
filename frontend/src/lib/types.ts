export type Severity = 'INFO' | 'WARNING' | 'CRITICAL';
export type EventType = 'DATA' | 'INFRASTRUCTURE';

export interface UnifiedEvent {
  type: EventType;
  severity: Severity;
  source: string;
  message: string;
  timestamp: string;
}

export interface MonitorState {
  infrastructure: UnifiedEvent[];
  logs: UnifiedEvent[];
  connected: boolean;
}
