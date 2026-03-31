import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { PaymentSuccess } from './PaymentSuccess';
import type { PaymentResult } from './types';

const result: PaymentResult = {
  amount: 42.5,
  currency: 'EUR',
  reference: 'abcd1234-5678-90ab-cdef-1234567890ab',
  timestamp: '2026-03-31T14:30:00.000Z',
};

describe('PaymentSuccess', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders success heading and amount', () => {
    render(<PaymentSuccess result={result} onReset={vi.fn()} />);
    expect(screen.getByText('Payment Successful')).toBeInTheDocument();
    expect(screen.getByText('€42.50')).toBeInTheDocument();
  });

  it('renders truncated reference', () => {
    render(<PaymentSuccess result={result} onReset={vi.fn()} />);
    expect(screen.getByText('Reference: abcd1234...')).toBeInTheDocument();
  });

  it('renders close button', () => {
    render(<PaymentSuccess result={result} onReset={vi.fn()} />);
    expect(screen.getByRole('button', { name: /close/i })).toBeInTheDocument();
  });

  it('calls onReset when close button is clicked', async () => {
    vi.useRealTimers();
    const user = userEvent.setup();
    const onReset = vi.fn();
    render(<PaymentSuccess result={result} onReset={onReset} />);

    await user.click(screen.getByRole('button', { name: /close/i }));
    expect(onReset).toHaveBeenCalledOnce();
  });

  it('calls onReset when New Payment button is clicked', async () => {
    vi.useRealTimers();
    const user = userEvent.setup();
    const onReset = vi.fn();
    render(<PaymentSuccess result={result} onReset={onReset} />);

    await user.click(screen.getByRole('button', { name: /new payment/i }));
    expect(onReset).toHaveBeenCalled();
  });

  it('auto-closes after 5 seconds', async () => {
    const onReset = vi.fn();
    render(<PaymentSuccess result={result} onReset={onReset} />);

    expect(onReset).not.toHaveBeenCalled();
    await act(async () => {
      vi.advanceTimersByTime(5000);
    });
    expect(onReset).toHaveBeenCalled();
  });

  it('renders a progress bar', () => {
    const { container } = render(<PaymentSuccess result={result} onReset={vi.fn()} />);
    expect(container.querySelector('.auto-close-bar-track')).toBeInTheDocument();
    expect(container.querySelector('.auto-close-bar-fill')).toBeInTheDocument();
  });
});
