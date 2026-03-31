package eu.accesa.blinkpay.bank.fips;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Outbound pacs.008 payload to FIPS simulator (simplified JSON).
 * Field names mirror the ISO 20022 semantics used by the FIPS service.
 */
record FipsSubmitRequest(
        UUID uetr,
        String debtorIBAN,
        String creditorIBAN,
        BigDecimal amount,
        String currency,
        String debtorName,
        String creditorName,
        String endToEndId,
        String remittanceInfo
) {}
