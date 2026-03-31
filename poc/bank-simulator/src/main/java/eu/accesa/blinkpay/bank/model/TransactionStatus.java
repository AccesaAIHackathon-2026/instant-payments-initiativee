package eu.accesa.blinkpay.bank.model;

public enum TransactionStatus {
    RCVD,  // Received
    ACSP,  // Accepted Settlement in Process
    ACSC,  // Accepted Settlement Completed
    RJCT   // Rejected
}
