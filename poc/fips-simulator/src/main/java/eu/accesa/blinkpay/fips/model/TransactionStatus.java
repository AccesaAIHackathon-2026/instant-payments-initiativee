package eu.accesa.blinkpay.fips.model;

/**
 * SCT Inst transaction status lifecycle as defined in ISO 20022 / EPC SCT Inst rulebook.
 *
 * Happy path:  RCVD → ACSP → ACSC
 * Rejection:   RCVD → RJCT
 */
public enum TransactionStatus {
    /** Received — pacs.008 accepted by FIPS, awaiting processing. */
    RCVD,

    /** Accepted Settlement in Process — routing to creditor PSP underway. */
    ACSP,

    /** Accepted Settlement Completed — funds irrevocably settled. */
    ACSC,

    /** Rejected — payment could not be processed (validation failure, timeout, etc.). */
    RJCT
}
