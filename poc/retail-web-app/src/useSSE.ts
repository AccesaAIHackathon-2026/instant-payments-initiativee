import { useEffect, useRef, useState, useCallback } from 'react';
import type { PaymentRejected, PaymentResult } from './types';

const SSE_BASE_URL = 'http://localhost:8080/bank/payment-events';
const API_KEY = import.meta.env.VITE_BANK_API_KEY as string;
const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 2000;

export type SseStatus = 'idle' | 'connecting' | 'connected' | 'disconnected' | 'error';

export function useSSE(
  reference: string | null,
  onPaymentConfirmed: (result: PaymentResult) => void,
  onPaymentRejected?: (rejected: PaymentRejected) => void,
): SseStatus {
  const [status, setStatus] = useState<SseStatus>('idle');
  const sourceRef = useRef<EventSource | null>(null);
  const retriesRef = useRef(0);
  const callbackRef = useRef(onPaymentConfirmed);
  callbackRef.current = onPaymentConfirmed;
  const rejectedRef = useRef(onPaymentRejected);
  rejectedRef.current = onPaymentRejected;

  const connect = useCallback((ref: string) => {
    setStatus('connecting');
    const source = new EventSource(`${SSE_BASE_URL}/${ref}?apiKey=${encodeURIComponent(API_KEY)}`);
    sourceRef.current = source;

    source.onopen = () => {
      setStatus('connected');
      retriesRef.current = 0;
    };

    source.addEventListener('settlement', (event) => {
      try {
        const data = JSON.parse(event.data);
        callbackRef.current({
          amount: data.amount,
          currency: data.currency ?? 'EUR',
          reference: data.uetr,
          timestamp: data.settledAt ?? new Date().toISOString(),
        });
      } catch {
        // ignore malformed messages
      }
    });

    source.addEventListener('rejection', (event) => {
      try {
        const data = JSON.parse(event.data);
        rejectedRef.current?.({
          amount: data.amount,
          currency: data.currency ?? 'EUR',
          rejectReason: data.rejectReason ?? 'RJCT',
        });
      } catch {
        // ignore malformed messages
      }
    });

    source.onerror = () => {
      source.close();
      if (retriesRef.current < MAX_RETRIES) {
        retriesRef.current++;
        setStatus('disconnected');
        setTimeout(() => {
          if (sourceRef.current === source) {
            connect(ref);
          }
        }, RETRY_DELAY_MS);
      } else {
        setStatus('error');
      }
    };
  }, []);

  useEffect(() => {
    if (!reference) {
      setStatus('idle');
      return;
    }

    retriesRef.current = 0;
    connect(reference);

    return () => {
      const source = sourceRef.current;
      if (source) {
        sourceRef.current = null;
        source.close();
      }
    };
  }, [reference, connect]);

  return status;
}
