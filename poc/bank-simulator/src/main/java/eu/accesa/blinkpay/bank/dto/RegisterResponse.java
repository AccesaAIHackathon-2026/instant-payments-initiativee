package eu.accesa.blinkpay.bank.dto;

import eu.accesa.blinkpay.bank.model.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterResponse(
        String iban,
        String holderName,
        AccountType accountType,
        String phoneAlias,
        BigDecimal bankBalance,
        UUID walletId       // null for merchants
) {}
