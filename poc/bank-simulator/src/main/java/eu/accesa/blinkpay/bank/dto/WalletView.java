package eu.accesa.blinkpay.bank.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Response for GET /bank/wallet/{walletId}. */
public record WalletView(
        UUID walletId,
        String ownerIban,
        BigDecimal balance
) {}
