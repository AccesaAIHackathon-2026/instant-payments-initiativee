package eu.accesa.blinkpay.bank.dto;

import java.math.BigDecimal;

/**
 * Inbound payment initiation from the consumer app.
 *
 * Either creditorAlias (phone/email → proxy lookup) or creditorIBAN must be set.
 * When creditorIBAN is provided directly (QR flow) the alias is ignored.
 *
 * creditorReference — ISO 11649 structured creditor reference, embedded in the
 * EPC069-12 QR code (line 9). Set by the merchant POS; absent in P2P payments.
 * Scopes the SSE settlement notification to the exact payment session.
 */
public record PaymentRequest(
        String debtorIBAN,
        String creditorIBAN,         // set directly from QR code (Flow B1)
        String creditorAlias,        // phone/email for P2P proxy lookup (Flow A)
        BigDecimal amount,
        String creditorReference,    // ISO 11649 from QR line 9; null for P2P
        String remittanceInfo        // unstructured text from QR line 10
) {}
