package eu.accesa.blinkpay.bank.dto;

import eu.accesa.blinkpay.bank.model.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Response for GET /bank/accounts/{iban}. */
public record AccountView(
        String iban,
        String holderName,
        BigDecimal bankBalance,
        UUID walletId,                  // null for merchants; resolve via GET /bank/wallet/{walletId}
        List<Transaction> recentTransactions
) {}
