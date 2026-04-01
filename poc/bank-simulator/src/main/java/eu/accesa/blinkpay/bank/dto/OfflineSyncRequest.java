package eu.accesa.blinkpay.bank.dto;

import java.util.List;

/**
 * POST /bank/wallet/{walletId}/sync-offline-transactions
 *
 * Batch of offline NFC P2P transfers to reconcile with the bank.
 * Each transaction carries a unique ID for idempotent processing.
 */
public record OfflineSyncRequest(List<OfflineTransactionEntry> transactions) {

    public record OfflineTransactionEntry(
            String transactionId,       // UUID generated on mobile at transfer time
            String counterpartyIban,
            java.math.BigDecimal amount,
            Direction direction,        // SEND or RECEIVE
            java.time.Instant timestamp
    ) {}

    public enum Direction { SEND, RECEIVE }
}
