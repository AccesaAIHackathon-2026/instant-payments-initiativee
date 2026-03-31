package eu.accesa.blinkpay.fips.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only view of a transaction for the admin GET /fips/transactions endpoint.
 */
public record TransactionView(
        UUID uetr,
        String debtorIBAN,
        String creditorIBAN,
        BigDecimal amount,
        String currency,
        String debtorName,
        String creditorName,
        String endToEndId,
        TransactionStatus status,
        Instant createdAt,
        Instant settledAt,
        String rejectReason
) {}
