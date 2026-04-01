import { useState, useCallback } from 'react';
import { v4 as uuid } from 'uuid';
import { AmountInput } from './AmountInput';
import { QRCodeDisplay } from './QRCodeDisplay';
import { PaymentSuccess } from './PaymentSuccess';
import { PaymentFailed } from './PaymentFailed';
import { useSSE } from './useSSE';
import type { AppScreen, PaymentRequest, PaymentResult, PaymentRejected } from './types';
import './App.css';

const CURRENCY = 'EUR';

export interface RetailerConfig {
  bankBaseUrl: string;
  creditorIban: string;
  creditorName: string;
  bankLabel: string;
  accentColor: string;
}

function App({ config }: { config: RetailerConfig }) {
  const [screen, setScreen] = useState<AppScreen>('input');
  const [payment, setPayment] = useState<PaymentRequest | null>(null);
  const [result, setResult] = useState<PaymentResult | null>(null);
  const [rejected, setRejected] = useState<PaymentRejected | null>(null);

  const handlePaymentConfirmed = useCallback((res: PaymentResult) => {
    setResult(res);
    setScreen('success');
  }, []);

  const handlePaymentRejected = useCallback((rej: PaymentRejected) => {
    setRejected(rej);
    setScreen('failed');
  }, []);

  const sseStatus = useSSE(
    screen === 'qr' ? (payment?.reference ?? null) : null,
    config.bankBaseUrl,
    handlePaymentConfirmed,
    handlePaymentRejected,
  );

  const handleAmountSubmit = (amount: number) => {
    const ref = uuid();
    setPayment({
      creditorIban: config.creditorIban,
      creditorName: config.creditorName,
      amount,
      currency: CURRENCY,
      reference: ref,
    });
    setScreen('qr');
  };

  const handleCancel = () => {
    setPayment(null);
    setResult(null);
    setRejected(null);
    setScreen('input');
  };

  const handleSimulate = () => {
    if (payment) {
      handlePaymentConfirmed({
        amount: payment.amount,
        currency: CURRENCY,
        reference: payment.reference,
        timestamp: new Date().toISOString(),
      });
    }
  };

  return (
    <div className="app-container" style={{ '--retailer-accent': config.accentColor } as React.CSSProperties}>
      {screen === 'input' && <AmountInput onSubmit={handleAmountSubmit} bankLabel={config.bankLabel} />}
      {screen === 'qr' && payment && (
        <QRCodeDisplay
          payment={payment}
          wsStatus={sseStatus}
          onCancel={handleCancel}
          onSimulate={handleSimulate}
        />
      )}
      {screen === 'success' && result && (
        <PaymentSuccess result={result} onReset={handleCancel} />
      )}
      {screen === 'failed' && rejected && (
        <PaymentFailed rejected={rejected} onReset={handleCancel} />
      )}
    </div>
  );
}

export default App;
