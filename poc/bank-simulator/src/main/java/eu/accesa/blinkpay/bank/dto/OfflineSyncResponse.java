package eu.accesa.blinkpay.bank.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Response from offline NFC transaction sync.
 * Returns the updated wallet balance and which transactions were accepted.
 * Duplicate transaction IDs are acknowledged but not re-applied.
 */
public record OfflineSyncResponse(
        UUID walletId,
        BigDecimal walletBalance,
        List<String> acceptedTransactionIds,
        List<String> duplicateTransactionIds
) {}
