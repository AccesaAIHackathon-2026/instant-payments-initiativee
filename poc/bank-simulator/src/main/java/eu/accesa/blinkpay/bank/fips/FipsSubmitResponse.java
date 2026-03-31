package eu.accesa.blinkpay.bank.fips;

import java.time.Instant;
import java.util.UUID;

/** Inbound pacs.002 response from FIPS simulator. */
record FipsSubmitResponse(
        UUID uetr,
        String status,       // "ACSC" | "RJCT"
        String statusReason, // ISO 20022 reject code, null on ACSC
        Instant timestamp
) {}
