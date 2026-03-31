package eu.accesa.blinkpay.bank.dto;

import eu.accesa.blinkpay.bank.model.AccountType;

import java.math.BigDecimal;

/**
 * POST /bank/register
 *
 * CONSUMER: holderName + phoneAlias required; bankBalance / digitalEuroBalance optional
 *           (default €0 — set higher for demo convenience).
 * MERCHANT: holderName required; phoneAlias must be null; balances ignored (always €0).
 */
public record RegisterRequest(
        AccountType accountType,
        String holderName,
        String phoneAlias,         // CONSUMER only
        BigDecimal bankBalance,    // CONSUMER only; defaults to 0 if null
        BigDecimal digitalEuroBalance  // CONSUMER only; defaults to 0 if null
) {}
