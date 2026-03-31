import type { PaymentRejected } from './types';

const REJECT_LABELS: Record<string, string> = {
  AM04: 'Insufficient funds',
  AC01: 'Invalid account',
  AG01: 'Transaction forbidden',
  DUPL: 'Duplicate transaction',
};

interface Props {
  rejected: PaymentRejected;
  onReset: () => void;
}

export function PaymentFailed({ rejected, onReset }: Props) {
  const label = REJECT_LABELS[rejected.rejectReason] ?? rejected.rejectReason;

  return (
    <div className="card">
      <div className="success-icon" style={{ color: 'var(--color-error, #c0392b)' }}>
        <svg viewBox="0 0 24 24" width="64" height="64" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <path d="M15 9l-6 6M9 9l6 6" />
        </svg>
      </div>
      <h1>Payment Failed</h1>
      <p className="amount-display">&euro;{rejected.amount.toFixed(2)}</p>
      <p className="detail">{label} ({rejected.rejectReason})</p>
      <button className="btn-primary" onClick={onReset}>New Payment</button>
    </div>
  );
}
