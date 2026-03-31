package eu.accesa.blinkpay.bank.dto;

import eu.accesa.blinkpay.bank.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

/** Response for GET /bank/accounts/{iban}. */
public record AccountView(
        String iban,
        String holderName,
        BigDecimal bankBalance,
        BigDecimal digitalEuroBalance,
        List<Transaction> recentTransactions
) {}
