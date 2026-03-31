import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { QRCodeDisplay } from './QRCodeDisplay';
import type { PaymentRequest } from './types';

const payment: PaymentRequest = {
  creditorIban: 'DE89370400440532013099',
  creditorName: 'Retail Store GmbH',
  amount: 25.0,
  currency: 'EUR',
  reference: 'abcd1234-5678-90ab-cdef-1234567890ab',
};

describe('QRCodeDisplay', () => {
  it('renders the amount', () => {
    render(
      <QRCodeDisplay payment={payment} wsStatus="idle" onCancel={vi.fn()} onSimulate={vi.fn()} />,
    );
    expect(screen.getByText('€25.00')).toBeInTheDocument();
  });

  it('renders heading and waiting status', () => {
    render(
      <QRCodeDisplay payment={payment} wsStatus="idle" onCancel={vi.fn()} onSimulate={vi.fn()} />,
    );
    expect(screen.getByText('Scan to Pay')).toBeInTheDocument();
    expect(screen.getByText('Waiting for payment...')).toBeInTheDocument();
  });

  it('shows truncated reference', () => {
    render(
      <QRCodeDisplay payment={payment} wsStatus="idle" onCancel={vi.fn()} onSimulate={vi.fn()} />,
    );
    expect(screen.getByText('Ref: abcd1234...')).toBeInTheDocument();
  });

  it('shows warning banner when disconnected', () => {
    render(
      <QRCodeDisplay payment={payment} wsStatus="disconnected" onCancel={vi.fn()} onSimulate={vi.fn()} />,
    );
    expect(screen.getByText(/bank simulator not connected/i)).toBeInTheDocument();
  });

  it('shows warning banner on error', () => {
    render(
      <QRCodeDisplay payment={payment} wsStatus="error" onCancel={vi.fn()} onSimulate={vi.fn()} />,
    );
    expect(screen.getByText(/bank simulator not connected/i)).toBeInTheDocument();
  });

  it('shows connected banner when connected', () => {
    render(
      <QRCodeDisplay payment={payment} wsStatus="connected" onCancel={vi.fn()} onSimulate={vi.fn()} />,
    );
    expect(screen.getByText('Connected to bank simulator')).toBeInTheDocument();
  });

  it('calls onCancel when Cancel button is clicked', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(
      <QRCodeDisplay payment={payment} wsStatus="idle" onCancel={onCancel} onSimulate={vi.fn()} />,
    );

    await user.click(screen.getByRole('button', { name: /cancel/i }));
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('renders a QR code SVG', () => {
    const { container } = render(
      <QRCodeDisplay payment={payment} wsStatus="idle" onCancel={vi.fn()} onSimulate={vi.fn()} />,
    );
    expect(container.querySelector('svg')).toBeInTheDocument();
  });
});
