export interface PaymentRequest {
  creditorIban: string;
  creditorName: string;
  amount: number;
  currency: string;
  reference: string;
}

export interface PaymentResult {
  amount: number;
  currency: string;
  reference: string;
  timestamp: string;
}

export type AppScreen = 'input' | 'qr' | 'success';
