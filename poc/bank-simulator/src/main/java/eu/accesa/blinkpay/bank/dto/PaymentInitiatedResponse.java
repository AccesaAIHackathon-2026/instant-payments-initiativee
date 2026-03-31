package eu.accesa.blinkpay.bank.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Returned by POST /bank/pay.
 * Contains the SCA challenge token the app must echo back via POST /bank/sca.
 */
public record PaymentInitiatedResponse(
        UUID uetr,
        String scaChallengeToken,
        String creditorName,
        String creditorIBAN,
        BigDecimal amount
) {}
