package eu.accesa.blinkpay.bank.dto;

import java.math.BigDecimal;

/**
 * Amount to move between the bank account and the Digital Euro custody wallet.
 * Used for both top-up (bank → DE) and redeem (DE → bank) operations.
 */
public record WalletTransferRequest(BigDecimal amount) {}
