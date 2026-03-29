/**
 * EN: Mock for native browser EventSource API to use in Jest/Node tests.
 * ES: Mock para la API nativa EventSource del navegador en tests Jest/Node.
 */

type EventSourceHandler = (event: Event) => void;

interface EventSourceMockOptions {
  url: string;
  withCredentials?: boolean;
}

export class EventSourceMock {
  private url: string;
  private listeners: Map<string, Set<EventSourceHandler>> = new Map();
  public readyState: number = 0; // CONNECTING
  public onopen: ((event: Event) => void) | null = null;
  public onerror: ((event: Event) => void) | null = null;
  public onmessage: ((event: MessageEvent) => void) | null = null;

  constructor(url: string, _options?: EventSourceMockOptions) {
    this.url = url;
    // EN: Simulate immediate connection after creation.
    // ES: Simular conexion inmediata despues de la creacion.
    setTimeout(() => this._simulateOpen(), 0);
  }

  /**
   * EN: Simulate opening the EventSource connection.
   * ES: Simular apertura de la conexion EventSource.
   */
  private _simulateOpen() {
    this.readyState = 1; // OPEN
    if (this.onopen) {
      this.onopen(new Event('open'));
    }
  }

  /**
   * EN: Simulate an error event.
   * ES: Simular un evento de error.
   */
  public _simulateError() {
    this.readyState = 0; // CONNECTING (attempt to reconnect)
    if (this.onerror) {
      this.onerror(new Event('error'));
    }
  }

  /**
   * EN: Simulate receiving a named event from server.
   * ES: Simular recepcion de un evento nombrado del servidor.
   */
  public _simulateEvent(eventName: string, data: string, lastEventId?: string) {
    const event = new MessageEvent('message', {
      data,
      lastEventId: lastEventId || undefined,
    });
    
    // Manually dispatch to addEventListener handlers
    const handlers = this.listeners.get(eventName);
    if (handlers) {
      handlers.forEach((handler) => {
        handler(event);
      });
    }
  }

  /**
   * EN: Register event listener (addEventListener pattern).
   * ES: Registrar escuchador de eventos (patron addEventListener).
   */
  public addEventListener(eventName: string, handler: EventSourceHandler) {
    if (!this.listeners.has(eventName)) {
      this.listeners.set(eventName, new Set());
    }
    this.listeners.get(eventName)!.add(handler);
  }

  /**
   * EN: Unregister event listener.
   * ES: Desregistrar escuchador de eventos.
   */
  public removeEventListener(eventName: string, handler: EventSourceHandler) {
    const handlers = this.listeners.get(eventName);
    if (handlers) {
      handlers.delete(handler);
    }
  }

  /**
   * EN: Close the connection.
   * ES: Cerrar la conexion.
   */
  public close() {
    this.readyState = 2; // CLOSED
    this.listeners.clear();
  }
}

