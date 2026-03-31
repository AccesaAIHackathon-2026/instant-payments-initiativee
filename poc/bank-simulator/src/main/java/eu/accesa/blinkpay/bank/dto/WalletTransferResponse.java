package eu.accesa.blinkpay.bank.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result of a top-up or redeem operation — returns the new balances of
 * both the bank account and the Digital Euro wallet after the transfer.
 */
public record WalletTransferResponse(
        UUID walletId,
        String ownerIban,
        BigDecimal walletBalance,
        BigDecimal bankBalance
) {}
