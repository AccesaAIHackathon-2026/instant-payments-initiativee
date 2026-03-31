package eu.accesa.blinkpay.bank.dto;

import java.math.BigDecimal;

/**
 * Inbound payment initiation from the consumer app.
 *
 * Either creditorAlias (phone/email → proxy lookup) or creditorIBAN must be set.
 * When creditorIBAN is provided directly (QR flow) the alias is ignored.
 */
public record PaymentRequest(
        String debtorIBAN,
        String creditorIBAN,    // set directly from QR code (Flow B1)
        String creditorAlias,   // phone/email for P2P proxy lookup (Flow A)
        BigDecimal amount,
        String remittanceInfo
) {}
