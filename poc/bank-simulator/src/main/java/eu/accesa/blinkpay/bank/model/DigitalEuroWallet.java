package eu.accesa.blinkpay.bank.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Digital Euro custody wallet.
 *
 * Separate from the commercial bank account: the bank holds these funds
 * as a custodian on behalf of the account holder, not as a bank liability.
 * In a real deployment this would interface with the ECB's Digital Euro
 * infrastructure (TIPS DE module) rather than an in-memory store.
 *
 * Each consumer account gets exactly one wallet at registration time.
 * Merchants receive settlement directly into their bank account.
 */
public class DigitalEuroWallet {

    private final UUID walletId;
    private final String ownerIban;
    private BigDecimal balance;

    public DigitalEuroWallet(String ownerIban, BigDecimal initialBalance) {
        this.walletId = UUID.randomUUID();
        this.ownerIban = ownerIban;
        this.balance = initialBalance;
    }

    /**
     * Debits up to {@code amount} from this wallet.
     *
     * @return the unmet remainder that must be covered from the bank account.
     *         Zero if the wallet fully covered the payment.
     */
    public synchronized BigDecimal debit(BigDecimal amount) {
        if (balance.compareTo(amount) >= 0) {
            balance = balance.subtract(amount);
            return BigDecimal.ZERO;
        }
        BigDecimal remainder = amount.subtract(balance);
        balance = BigDecimal.ZERO;
        return remainder;
    }

    /** Credits the wallet (used for rollback on FIPS rejection). */
    public synchronized void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public synchronized BigDecimal getBalance() { return balance; }
    public UUID getWalletId() { return walletId; }
    public String getOwnerIban() { return ownerIban; }
}
