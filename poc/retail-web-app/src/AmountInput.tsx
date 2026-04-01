import { useState } from 'react';

interface Props {
  onSubmit: (amount: number) => void;
  bankLabel?: string;
}

const KEYS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '0', '⌫'];

export function AmountInput({ onSubmit, bankLabel }: Props) {
  const [raw, setRaw] = useState('');

  const handleKey = (key: string) => {
    if (key === '⌫') {
      setRaw((prev) => prev.slice(0, -1));
      return;
    }
    if (key === '.' && raw.includes('.')) return;
    // limit to 2 decimal places
    const dot = raw.indexOf('.');
    if (dot !== -1 && raw.length - dot >= 3) return;
    // limit total length
    if (raw.replace('.', '').length >= 8) return;
    setRaw((prev) => prev + key);
  };

  const value = parseFloat(raw);
  const valid = value > 0;

  const handleBlinkPay = () => {
    if (valid) onSubmit(value);
  };

  return (
    <div className="pos-layout">
      <div className="pos-display card">
        <img src="/img/BlinkPay-logo.png" alt="BlinkPay" className="pos-logo" />
        {bankLabel && <div className="pos-bank-label">{bankLabel}</div>}
        <form onSubmit={(e) => { e.preventDefault(); handleBlinkPay(); }}>
          <div className="input-group input-group--centered">
            <label htmlFor="amount" className="amount-label">Amount (EUR)</label>
            <div className="amount-input-wrapper">
              <span className="currency-symbol">&#8364;</span>
              <input
                id="amount"
                type="number"
                step="0.01"
                min="0.01"
                placeholder="0.00"
                value={raw}
                onChange={(e) => setRaw(e.target.value)}
                autoFocus
              />
            </div>
          </div>
        </form>

        <div className="payment-methods-title">Payment Method</div>
        <div className="payment-methods-grid">
          <button className="payment-method-btn payment-method-btn--disabled" disabled>
            <div className="payment-method-logos">
              <svg viewBox="0 0 48 16" className="card-logo" aria-label="Visa">
                <text x="0" y="13" fontWeight="bold" fontSize="14" fill="#1A1F71" fontFamily="Arial, sans-serif">VISA</text>
              </svg>
              <svg viewBox="0 0 40 24" className="card-logo" aria-label="Mastercard">
                <circle cx="14" cy="12" r="10" fill="#EB001B" />
                <circle cx="26" cy="12" r="10" fill="#F79E1B" />
                <path d="M20 4.6a10 10 0 0 1 0 14.8 10 10 0 0 1 0-14.8z" fill="#FF5F00" />
              </svg>
              <svg viewBox="0 0 48 16" className="card-logo" aria-label="Amex">
                <text x="0" y="13" fontWeight="bold" fontSize="11" fill="#006FCF" fontFamily="Arial, sans-serif">AMEX</text>
              </svg>
            </div>
            <span className="payment-method-label">Card Payment</span>
            <span className="payment-method-badge">Unsupported due to high commissions</span>
          </button>

          <button
            className="payment-method-btn payment-method-btn--blinkpay"
            disabled={!valid}
            onClick={handleBlinkPay}
          >
            <img src="/img/BlinkPay-logo.png" alt="" className="payment-method-logo" />
            <span className="payment-method-label">BlinkPay</span>
            <span className="payment-method-sublabel">Digital Wallet &middot; QR Code</span>
          </button>
        </div>
      </div>
      <div className="numpad card">
        <div className="numpad-grid">
          {KEYS.map((key) => (
            <button
              key={key}
              className={`numpad-key ${key === '⌫' ? 'numpad-key--delete' : ''}`}
              onClick={() => handleKey(key)}
              type="button"
            >
              {key}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
