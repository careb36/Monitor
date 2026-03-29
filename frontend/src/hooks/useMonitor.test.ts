/**
 * EN: Unit tests for useMonitor hook.
 * ES: Tests unitarios para el hook useMonitor.
 */

import { renderHook, act, waitFor } from '@testing-library/react';
import { useMonitor } from '../useMonitor';
import { EventSourceMock } from '@/__mocks__/EventSource';
import { UnifiedEvent } from '@/lib/types';

/**
 * EN: Helper to create a test critical event.
 * ES: Helper para crear un evento critico de prueba.
 */
function createCriticalEvent(source: string, message: string): UnifiedEvent {
  return {
    type: 'INFRASTRUCTURE',
    severity: 'CRITICAL',
    source,
    message,
    timestamp: new Date().toISOString(),
  };
}

describe('useMonitor hook', () => {
  /**
   * EN: Test state transitions: CLOSED -> CONNECTING -> OPEN.
   * ES: Test transiciones de estado: CLOSED -> CONNECTING -> OPEN.
   */
  it('should transition from CLOSED to CONNECTING to OPEN', async () => {
    const { result } = renderHook(() => useMonitor());

    // EN: Initially closed.
    // ES: Inicialmente cerrado.
    expect(result.current.status).toBe('CLOSED');

    // EN: Start the stream.
    // ES: Inicia el stream.
    act(() => {
      result.current.start();
    });

    // EN: Should be connecting first.
    // ES: Deberia estar reconectando primero.
    expect(result.current.status).toBe('CONNECTING');

    // EN: Wait for connection to open (mock simulates immediate open).
    // ES: Esperar a que la conexion se abra (mock simula apertura inmediata).
    await waitFor(() => {
      expect(result.current.status).toBe('OPEN');
    });
  });

  /**
   * EN: Test receiving a critical event and adding to state.
   * ES: Test recibir un evento critico y agregarlo al estado.
   */
  it('should receive and store a critical event', async () => {
    const { result } = renderHook(() => useMonitor());

    act(() => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.status).toBe('OPEN');
    });

    // EN: Simulate receiving an event from the server.
    // ES: Simular recepcion de un evento del servidor.
    const testEvent = createCriticalEvent('database-01', 'Connection timeout');
    const mockES = (global as any).eventSourceInstance as EventSourceMock;

    act(() => {
      mockES._simulateEvent(
        'critical_alert',
        JSON.stringify(testEvent),
        '123'
      );
    });

    // EN: Alert should be in the state.
    // ES: Alerta deberia estar en el estado.
    await waitFor(() => {
      expect(result.current.alerts.length).toBe(1);
      expect(result.current.alerts[0].event.source).toBe('database-01');
      expect(result.current.lastEventId).toBe('123');
    });
  });

  /**
   * EN: Test deduplication: duplicate events should be filtered.
   * ES: Test deduplicacion: eventos duplicados deberian ser filtrados.
   */
  it('should deduplicate events with the same id', async () => {
    const { result } = renderHook(() => useMonitor());

    act(() => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.status).toBe('OPEN');
    });

    const testEvent = createCriticalEvent('database-02', 'Memory full');
    const mockES = (global as any).eventSourceInstance as EventSourceMock;

    // EN: Send the same event twice with the same ID.
    // ES: Enviar el mismo evento dos veces con el mismo ID.
    act(() => {
      mockES._simulateEvent(
        'critical_alert',
        JSON.stringify(testEvent),
        '456'
      );
      mockES._simulateEvent(
        'critical_alert',
        JSON.stringify(testEvent),
        '456'
      );
    });

    // EN: Should only have one alert, not two.
    // ES: Deberia tener solo una alerta, no dos.
    await waitFor(() => {
      expect(result.current.alerts.length).toBe(1);
    });
  });

  /**
   * EN: Test that close() is called on unmount.
   * ES: Test que se llame a close() al desmontar.
   */
  it('should call close() on unmount', async () => {
    const { result, unmount } = renderHook(() => useMonitor());

    act(() => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.status).toBe('OPEN');
    });

    const mockES = (global as any).eventSourceInstance as EventSourceMock;
    const closeSpy = jest.spyOn(mockES, 'close');

    act(() => {
      unmount();
    });

    expect(closeSpy).toHaveBeenCalled();
    closeSpy.mockRestore();
  });

  /**
   * EN: Test manual stop.
   * ES: Test detencin manual.
   */
  it('should stop the stream and close EventSource', async () => {
    const { result } = renderHook(() => useMonitor());

    act(() => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.status).toBe('OPEN');
    });

    const mockES = (global as any).eventSourceInstance as EventSourceMock;
    const closeSpy = jest.spyOn(mockES, 'close');

    act(() => {
      result.current.stop();
    });

    expect(result.current.status).toBe('CLOSED');
    expect(closeSpy).toHaveBeenCalled();
    closeSpy.mockRestore();
  });

  /**
   * EN: Test clearAlerts functionality.
   * ES: Test funcionalidad de clearAlerts.
   */
  it('should clear all alerts', async () => {
    const { result } = renderHook(() => useMonitor());

    act(() => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.status).toBe('OPEN');
    });

    const testEvent = createCriticalEvent('database-03', 'Disk full');
    const mockES = (global as any).eventSourceInstance as EventSourceMock;

    act(() => {
      mockES._simulateEvent(
        'critical_alert',
        JSON.stringify(testEvent),
        '789'
      );
    });

    await waitFor(() => {
      expect(result.current.alerts.length).toBe(1);
    });

    act(() => {
      result.current.clearAlerts();
    });

    expect(result.current.alerts.length).toBe(0);
    expect(result.current.lastEventId).toBeNull();
  });

  /**
   * EN: Test non-critical events are ignored.
   * ES: Test que eventos no-criticos sean ignorados.
   */
  it('should ignore non-critical events', async () => {
    const { result } = renderHook(() => useMonitor());

    act(() => {
      result.current.start();
    });

    await waitFor(() => {
      expect(result.current.status).toBe('OPEN');
    });

    const infEvent: UnifiedEvent = {
      type: 'INFRASTRUCTURE',
      severity: 'INFO',
      source: 'database-04',
      message: 'Connection OK',
      timestamp: new Date().toISOString(),
    };

    const mockES = (global as any).eventSourceInstance as EventSourceMock;

    act(() => {
      mockES._simulateEvent(
        'critical_alert',
        JSON.stringify(infEvent),
        '999'
      );
    });

    // EN: Should not add info events to alerts.
    // ES: No deberia agregar eventos info a alertas.
    expect(result.current.alerts.length).toBe(0);
  });
});

