package eu.accesa.blinkpay.bank.dto;

import eu.accesa.blinkpay.bank.model.VopResult;

/** Response for GET /bank/vop?iban={}&name={}. */
public record VopResponse(
        VopResult result,
        String actualName   // returned so the UI can show the real name on CLOSE_MATCH
) {}
