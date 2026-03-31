import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import App from './App';

// Mock EventSource so it doesn't try to connect
class MockEventSource {
  close = vi.fn();
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  addEventListener = vi.fn();
}
vi.stubGlobal('EventSource', MockEventSource);

describe('App', () => {
  it('starts on the input screen', () => {
    render(<App />);
    expect(screen.getByAltText('BlinkPay')).toBeInTheDocument();
    expect(screen.getByText('Payment Method')).toBeInTheDocument();
  });

  it('navigates to QR screen after entering amount and selecting BlinkPay', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: '5' }));
    const blinkBtn = screen.getByText('BlinkPay').closest('button')!;
    await user.click(blinkBtn);

    expect(screen.getByText('Scan to Pay')).toBeInTheDocument();
    expect(screen.getByText('€5.00')).toBeInTheDocument();
  });

  it('navigates back to input screen when Cancel is clicked', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: '5' }));
    const blinkBtn = screen.getByText('BlinkPay').closest('button')!;
    await user.click(blinkBtn);
    await user.click(screen.getByRole('button', { name: /cancel/i }));

    expect(screen.getByText('Payment Method')).toBeInTheDocument();
  });
});
