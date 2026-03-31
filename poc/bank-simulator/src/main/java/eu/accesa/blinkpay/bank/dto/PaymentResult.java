package eu.accesa.blinkpay.bank.dto;

import eu.accesa.blinkpay.bank.model.TransactionStatus;

import java.util.UUID;

/** Final outcome returned by POST /bank/sca after settlement attempt. */
public record PaymentResult(
        UUID uetr,
        TransactionStatus status,
        String rejectReason   // null on ACSC
) {}
