import { QRCodeSVG } from 'qrcode.react';
import type { PaymentRequest } from './types';
import type { SseStatus } from './useSSE';

/** Build an EPC069-12 (GiroCode) QR payload for SEPA Credit Transfer. */
function toEpcQr(p: PaymentRequest): string {
  return [
    'BCD',                                    // Service Tag
    '002',                                    // Version
    '1',                                      // Character set (1 = UTF-8)
    'SCT',                                    // Identification (SEPA Credit Transfer)
    '',                                       // BIC (optional in v002)
    p.creditorName,                           // Beneficiary name (max 70)
    p.creditorIban,                           // Beneficiary IBAN
    `${p.currency}${p.amount.toFixed(2)}`,    // Amount (e.g. "EUR25.00")
    '',                                       // Purpose code (optional)
    p.reference,                              // Structured creditor reference (ISO 11649, QR line 9)
    '',                                       // Unstructured remittance info (max 140)
  ].join('\n');
}

interface Props {
  payment: PaymentRequest;
  wsStatus: SseStatus;
  onCancel: () => void;
  onSimulate: () => void;
}

export function QRCodeDisplay({ payment, wsStatus, onCancel, onSimulate }: Props) {
  const qrValue = toEpcQr(payment);

  const handleCopyQr = () => {
    navigator.clipboard.writeText(qrValue).catch(() => {
      // Fallback: select a hidden textarea
      const el = document.createElement('textarea');
      el.value = qrValue;
      document.body.appendChild(el);
      el.select();
      document.execCommand('copy');
      document.body.removeChild(el);
    });
  };

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
        <button className="btn-secondary" onClick={handleCopyQr} title={qrValue}>
          Copy QR
        </button>
        {import.meta.env.DEV && (
          <button className="btn-simulate" onClick={onSimulate}>
            Simulate Payment
          </button>
        )}
      </div>
    </div>
  );
}
