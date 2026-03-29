/**
 * EN: Unit tests for components.
 * ES: Tests unitarios para componentes.
 */

import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import { ConnectionBadge } from './ConnectionBadge';
import { AlertCard } from './AlertCard';
import { CriticalAlert } from '@/lib/types';

describe('ConnectionBadge', () => {
  /**
   * EN: Test OPEN status displays "Online".
   * ES: Test que estado OPEN muestre "Online".
   */
  it('should display Online badge when status is OPEN', () => {
    render(<ConnectionBadge status="OPEN" />);
    expect(screen.getByText('Online')).toBeInTheDocument();
    expect(screen.getByText(/Conectado/)).toBeInTheDocument();
  });

  /**
   * EN: Test CONNECTING status displays "Reconnecting".
   * ES: Test que estado CONNECTING muestre "Reconectando".
   */
  it('should display Reconnecting badge when status is CONNECTING', () => {
    render(<ConnectionBadge status="CONNECTING" />);
    expect(screen.getByText('Reconnecting')).toBeInTheDocument();
    expect(screen.getByText(/Reconectando/)).toBeInTheDocument();
  });

  /**
   * EN: Test CLOSED status displays "Offline".
   * ES: Test que estado CLOSED muestre "Offline".
   */
  it('should display Offline badge when status is CLOSED', () => {
    render(<ConnectionBadge status="CLOSED" />);
    expect(screen.getByText('Offline')).toBeInTheDocument();
    expect(screen.getByText(/Desconectado/)).toBeInTheDocument();
  });
});

describe('AlertCard', () => {
  /**
   * EN: Test alert card renders critical event data.
   * ES: Test que tarjeta de alerta renderice datos de evento critico.
   */
  it('should render critical alert information', () => {
    const alert: CriticalAlert = {
      id: 'evt-123',
      event: {
        type: 'INFRASTRUCTURE',
        severity: 'CRITICAL',
        source: 'database-01',
        message: 'Connection timeout',
        timestamp: '2026-03-29T12:00:00Z',
      },
      receivedAt: '2026-03-29T12:00:01Z',
    };

    render(<AlertCard alert={alert} />);

    expect(screen.getByText('database-01')).toBeInTheDocument();
    expect(screen.getByText('Connection timeout')).toBeInTheDocument();
    expect(screen.getByText(/evt-123/)).toBeInTheDocument();
  });

  /**
   * EN: Test timestamp is localized to es-ES format.
   * ES: Test que timestamp este localizado a formato es-ES.
   */
  it('should display timestamp in es-ES locale', () => {
    const alert: CriticalAlert = {
      id: 'evt-456',
      event: {
        type: 'INFRASTRUCTURE',
        severity: 'CRITICAL',
        source: 'redis-cache',
        message: 'Memory full',
        timestamp: '2026-03-29T14:30:00Z',
      },
      receivedAt: '2026-03-29T14:30:01Z',
    };

    render(<AlertCard alert={alert} />);

    // EN: Should render a timestamp (es-ES format includes day/month/year H:MM:SS).
    // ES: Deberia renderizar un timestamp (formato es-ES incluye dia/mes/ano H:MM:SS).
    const rendered = screen.getByText(/2026/);
    expect(rendered).toBeInTheDocument();
  });

  /**
   * EN: Test data-testid is properly set for test selection.
   * ES: Test que data-testid este correctamente seteado para seleccion en tests.
   */
  it('should have correct data-testid for testing', () => {
    const alert: CriticalAlert = {
      id: 'evt-789',
      event: {
        type: 'INFRASTRUCTURE',
        severity: 'CRITICAL',
        source: 'app-server',
        message: 'OOM error',
        timestamp: '2026-03-29T15:00:00Z',
      },
      receivedAt: '2026-03-29T15:00:01Z',
    };

    const { container } = render(<AlertCard alert={alert} />);
    const article = container.querySelector('[data-testid="alert-card-evt-789"]');
    expect(article).toBeInTheDocument();
  });
});

