package eu.accesa.blinkpay.bank.dto;

import java.math.BigDecimal;

/** Retailer sends this to POST /bank/request-to-pay (Flow B2 stretch). */
public record RtpRequest(
        String creditorIBAN,
        String debtorAlias,    // payer's phone or email
        BigDecimal amount,
        String remittanceInfo
) {}
