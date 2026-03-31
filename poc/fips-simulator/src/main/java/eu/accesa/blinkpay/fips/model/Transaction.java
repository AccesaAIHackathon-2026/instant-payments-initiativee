package eu.accesa.blinkpay.fips.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mutable in-memory representation of a FIPS transaction.
 *
 * Immutable fields (set at creation from pacs.008) are final.
 * Mutable fields (status, settledAt, rejectReason) are volatile for
 * safe reads across threads without full synchronization.
 */
public class Transaction {

    private final UUID uetr;
    private final String debtorIBAN;
    private final String creditorIBAN;
    private final BigDecimal amount;
    private final String currency;
    private final String debtorName;
    private final String creditorName;
    private final String endToEndId;
    private final String remittanceInfo;
    private final Instant createdAt;

    private volatile TransactionStatus status;
    private volatile Instant settledAt;
    private volatile String rejectReason;

    public Transaction(UUID uetr, String debtorIBAN, String creditorIBAN,
                       BigDecimal amount, String currency,
                       String debtorName, String creditorName,
                       String endToEndId, String remittanceInfo) {
        this.uetr = uetr;
        this.debtorIBAN = debtorIBAN;
        this.creditorIBAN = creditorIBAN;
        this.amount = amount;
        this.currency = currency;
        this.debtorName = debtorName;
        this.creditorName = creditorName;
        this.endToEndId = endToEndId;
        this.remittanceInfo = remittanceInfo;
        this.createdAt = Instant.now();
        this.status = TransactionStatus.RCVD;
    }

    public UUID getUetr() { return uetr; }
    public String getDebtorIBAN() { return debtorIBAN; }
    public String getCreditorIBAN() { return creditorIBAN; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDebtorName() { return debtorName; }
    public String getCreditorName() { return creditorName; }
    public String getEndToEndId() { return endToEndId; }
    public String getRemittanceInfo() { return remittanceInfo; }
    public Instant getCreatedAt() { return createdAt; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
