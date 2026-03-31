import { useState, useCallback } from 'react';
import { v4 as uuid } from 'uuid';
import { AmountInput } from './AmountInput';
import { QRCodeDisplay } from './QRCodeDisplay';
import { PaymentSuccess } from './PaymentSuccess';
import { PaymentFailed } from './PaymentFailed';
import { useSSE } from './useSSE';
import type { AppScreen, PaymentRequest, PaymentResult, PaymentRejected } from './types';
import './App.css';

const CREDITOR_IBAN = 'DE89370400440532013099';
const CREDITOR_NAME = 'Retail Store GmbH';
const CURRENCY = 'EUR';

function App() {
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
    handlePaymentConfirmed,
    handlePaymentRejected,
  );

  const handleAmountSubmit = (amount: number) => {
    const ref = uuid();
    setPayment({
      creditorIban: CREDITOR_IBAN,
      creditorName: CREDITOR_NAME,
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
    <div className="app-container">
      {screen === 'input' && <AmountInput onSubmit={handleAmountSubmit} />}
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
