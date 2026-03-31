import { useEffect, useState } from 'react';
import type { PaymentResult } from './types';

const AUTO_CLOSE_MS = 5000;

interface Props {
  result: PaymentResult;
  onReset: () => void;
}

export function PaymentSuccess({ result, onReset }: Props) {
  const time = new Date(result.timestamp).toLocaleTimeString();
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setElapsed((prev) => {
        const next = prev + 50;
        if (next >= AUTO_CLOSE_MS) {
          clearInterval(interval);
          onReset();
        }
        return next;
      });
    }, 50);
    return () => clearInterval(interval);
  }, [onReset]);

  const progress = Math.min(elapsed / AUTO_CLOSE_MS, 1);

  return (
    <div className="card" style={{ position: 'relative' }}>
      <button className="close-btn" onClick={onReset} aria-label="Close">&times;</button>
      <div className="success-icon">
        <svg viewBox="0 0 24 24" width="64" height="64" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <path d="M8 12l3 3 5-5" />
        </svg>
      </div>
      <h1>Payment Successful</h1>
      <p className="amount-display">&euro;{result.amount.toFixed(2)}</p>
      <p className="detail">Reference: {result.reference.slice(0, 8)}...</p>
      <p className="detail">Settled at: {time}</p>
      <div className="auto-close-bar-track">
        <div className="auto-close-bar-fill" style={{ width: `${progress * 100}%` }} />
      </div>
      <button className="btn-primary" onClick={onReset}>New Payment</button>
    </div>
  );
}
