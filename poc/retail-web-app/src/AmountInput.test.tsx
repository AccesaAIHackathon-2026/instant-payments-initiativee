import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { AmountInput } from './AmountInput';

describe('AmountInput', () => {
  it('renders the BlinkPay logo and amount label', () => {
    render(<AmountInput onSubmit={vi.fn()} />);
    expect(screen.getByAltText('BlinkPay')).toBeInTheDocument();
    expect(screen.getByText('Amount (EUR)')).toBeInTheDocument();
  });

  it('renders all numpad keys', () => {
    render(<AmountInput onSubmit={vi.fn()} />);
    for (const digit of ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.']) {
      expect(screen.getByRole('button', { name: digit })).toBeInTheDocument();
    }
    expect(screen.getByRole('button', { name: '⌫' })).toBeInTheDocument();
  });

  it('renders payment method section with card and BlinkPay options', () => {
    render(<AmountInput onSubmit={vi.fn()} />);
    expect(screen.getByText('Payment Method')).toBeInTheDocument();
    expect(screen.getByText('Card Payment')).toBeInTheDocument();
    expect(screen.getByText(/unsupported due to high commissions/i)).toBeInTheDocument();
    expect(screen.getByText('BlinkPay')).toBeInTheDocument();
    expect(screen.getByText(/digital wallet/i)).toBeInTheDocument();
  });

  it('card payment button is always disabled', () => {
    render(<AmountInput onSubmit={vi.fn()} />);
    const cardBtn = screen.getByText('Card Payment').closest('button')!;
    expect(cardBtn).toBeDisabled();
  });

  it('BlinkPay button is disabled when amount is empty', () => {
    render(<AmountInput onSubmit={vi.fn()} />);
    const blinkBtn = screen.getByText('BlinkPay').closest('button')!;
    expect(blinkBtn).toBeDisabled();
  });

  it('BlinkPay button is enabled when amount is valid', async () => {
    const user = userEvent.setup();
    render(<AmountInput onSubmit={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: '5' }));
    const blinkBtn = screen.getByText('BlinkPay').closest('button')!;
    expect(blinkBtn).toBeEnabled();
  });

  it('builds amount from numpad clicks', async () => {
    const user = userEvent.setup();
    render(<AmountInput onSubmit={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: '1' }));
    await user.click(screen.getByRole('button', { name: '2' }));
    await user.click(screen.getByRole('button', { name: '.' }));
    await user.click(screen.getByRole('button', { name: '5' }));
    await user.click(screen.getByRole('button', { name: '0' }));

    const input = screen.getByLabelText(/amount/i) as HTMLInputElement;
    expect(input.value).toBe('12.5');
  });

  it('prevents entering two decimal points', async () => {
    const user = userEvent.setup();
    render(<AmountInput onSubmit={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: '1' }));
    await user.click(screen.getByRole('button', { name: '.' }));
    await user.click(screen.getByRole('button', { name: '.' }));
    await user.click(screen.getByRole('button', { name: '5' }));

    const input = screen.getByLabelText(/amount/i) as HTMLInputElement;
    expect(input.value).toBe('1.5');
  });

  it('limits to 2 decimal places', async () => {
    const user = userEvent.setup();
    render(<AmountInput onSubmit={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: '1' }));
    await user.click(screen.getByRole('button', { name: '.' }));
    await user.click(screen.getByRole('button', { name: '2' }));
    await user.click(screen.getByRole('button', { name: '3' }));
    await user.click(screen.getByRole('button', { name: '4' }));

    const input = screen.getByLabelText(/amount/i) as HTMLInputElement;
    expect(input.value).toBe('1.23');
  });

  it('backspace removes last character', async () => {
    const user = userEvent.setup();
    render(<AmountInput onSubmit={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: '4' }));
    await user.click(screen.getByRole('button', { name: '2' }));
    await user.click(screen.getByRole('button', { name: '⌫' }));

    const input = screen.getByLabelText(/amount/i) as HTMLInputElement;
    expect(input.value).toBe('4');
  });

  it('calls onSubmit when BlinkPay button is clicked with valid amount', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<AmountInput onSubmit={onSubmit} />);

    await user.click(screen.getByRole('button', { name: '9' }));
    await user.click(screen.getByRole('button', { name: '.' }));
    await user.click(screen.getByRole('button', { name: '9' }));
    await user.click(screen.getByRole('button', { name: '9' }));

    const blinkBtn = screen.getByText('BlinkPay').closest('button')!;
    await user.click(blinkBtn);

    expect(onSubmit).toHaveBeenCalledWith(9.99);
  });

  it('allows keyboard input in the text field', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<AmountInput onSubmit={onSubmit} />);

    const input = screen.getByLabelText(/amount/i);
    await user.type(input, '25.50');

    expect((input as HTMLInputElement).value).toBe('25.5');
    const blinkBtn = screen.getByText('BlinkPay').closest('button')!;
    expect(blinkBtn).toBeEnabled();
  });
});
