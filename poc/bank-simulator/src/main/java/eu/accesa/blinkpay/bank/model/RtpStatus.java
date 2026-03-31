package eu.accesa.blinkpay.bank.model;

public enum RtpStatus {
    PENDING,   // Sent to payer, awaiting approval
    ACCEPTED,  // Payer approved — payment in progress
    SETTLED,   // Underlying SCT Inst payment confirmed ACSC
    REJECTED,  // Payer explicitly rejected the request
    EXPIRED    // Not acted upon within timeout
}
