package eu.accesa.blinkpay.bank.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transaction(
        UUID uetr,
        String debtorIBAN,
        String creditorIBAN,
        String debtorName,
        String creditorName,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        String remittanceInfo,
        Instant createdAt,
        Instant settledAt
) {
    /** Convenience factory for a settled transaction. */
    public static Transaction settled(UUID uetr, String debtorIBAN, String creditorIBAN,
                                      String debtorName, String creditorName,
                                      BigDecimal amount, String remittanceInfo) {
        Instant now = Instant.now();
        return new Transaction(uetr, debtorIBAN, creditorIBAN, debtorName, creditorName,
                amount, "EUR", TransactionStatus.ACSC, remittanceInfo, now, now);
    }

    /** Convenience factory for a rejected transaction. */
    public static Transaction rejected(UUID uetr, String debtorIBAN, String creditorIBAN,
                                       String debtorName, String creditorName,
                                       BigDecimal amount, String remittanceInfo) {
        Instant now = Instant.now();
        return new Transaction(uetr, debtorIBAN, creditorIBAN, debtorName, creditorName,
                amount, "EUR", TransactionStatus.RJCT, remittanceInfo, now, null);
    }
}
