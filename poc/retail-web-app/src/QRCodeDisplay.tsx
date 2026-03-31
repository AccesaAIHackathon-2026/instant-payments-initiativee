import { QRCodeSVG } from 'qrcode.react';
import type { PaymentRequest } from './types';
import type { SseStatus } from './useSSE';

interface Props {
  payment: PaymentRequest;
  wsStatus: SseStatus;
  onCancel: () => void;
  onSimulate: () => void;
}

export function QRCodeDisplay({ payment, wsStatus, onCancel, onSimulate }: Props) {
  const qrValue = JSON.stringify({
    creditorIban: payment.creditorIban,
    creditorName: payment.creditorName,
    amount: payment.amount,
    currency: payment.currency,
    reference: payment.reference,
  });

  return (
    <div className="card">
      <h1>Scan to Pay</h1>
      <p className="amount-display">&euro;{payment.amount.toFixed(2)}</p>

      <div className="qr-container">
        <QRCodeSVG value={qrValue} size={256} level="M" />
      </div>

      <div className="status-row">
        <span className="pulse-dot" />
        <span>Waiting for payment...</span>
      </div>

      {(wsStatus === 'error' || wsStatus === 'disconnected') && (
        <div className="toast toast--warning">
          Bank simulator not connected — payment confirmation will appear when the simulator is running on port 8080
        </div>
      )}

      {wsStatus === 'connected' && (
        <div className="toast toast--success">
          Connected to bank simulator
        </div>
      )}

      <p className="reference">Ref: {payment.reference.slice(0, 8)}...</p>

      <div className="button-row">
        <button className="btn-secondary" onClick={onCancel}>Cancel</button>
        {import.meta.env.DEV && (
          <button className="btn-simulate" onClick={onSimulate}>
            Simulate Payment
          </button>
        )}
      </div>
    </div>
  );
}
