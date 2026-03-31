package eu.accesa.blinkpay.bank.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * In-memory pain.013 Request-to-Pay entity (stretch Flow B2).
 */
public class RequestToPay {

    private final UUID rtpId;
    private final String debtorIBAN;   // resolved from alias at creation time
    private final String creditorIBAN;
    private final String creditorName;
    private final BigDecimal amount;
    private final String remittanceInfo;
    private final Instant createdAt;

    private volatile RtpStatus status;
    private volatile UUID paymentUetr;  // set once the underlying payment is submitted

    public RequestToPay(String debtorIBAN, String creditorIBAN, String creditorName,
                        BigDecimal amount, String remittanceInfo) {
        this.rtpId = UUID.randomUUID();
        this.debtorIBAN = debtorIBAN;
        this.creditorIBAN = creditorIBAN;
        this.creditorName = creditorName;
        this.amount = amount;
        this.remittanceInfo = remittanceInfo;
        this.createdAt = Instant.now();
        this.status = RtpStatus.PENDING;
    }

    public UUID getRtpId() { return rtpId; }
    public String getDebtorIBAN() { return debtorIBAN; }
    public String getCreditorIBAN() { return creditorIBAN; }
    public String getCreditorName() { return creditorName; }
    public BigDecimal getAmount() { return amount; }
    public String getRemittanceInfo() { return remittanceInfo; }
    public Instant getCreatedAt() { return createdAt; }

    public RtpStatus getStatus() { return status; }
    public void setStatus(RtpStatus status) { this.status = status; }

    public UUID getPaymentUetr() { return paymentUetr; }
    public void setPaymentUetr(UUID paymentUetr) { this.paymentUetr = paymentUetr; }
}
