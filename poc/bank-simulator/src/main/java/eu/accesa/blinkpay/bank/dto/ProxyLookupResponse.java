package eu.accesa.blinkpay.bank.dto;

/** Response for GET /bank/proxy?alias={phone|email}. */
public record ProxyLookupResponse(
        String iban,
        String holderName
) {}
