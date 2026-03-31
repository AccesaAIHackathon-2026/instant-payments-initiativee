package eu.accesa.blinkpay.bank.service;

import eu.accesa.blinkpay.bank.model.DigitalEuroWallet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory Digital Euro wallet registry.
 *
 * Pre-seeded with wallets for the two consumer test accounts.
 * Merchants are not seeded — they receive settlement directly into their
 * commercial bank account, as per the ECB two-tier model.
 *
 * | Owner IBAN              | Initial Balance |
 * |-------------------------|-----------------|
 * | DE89370400440532013001  | €50.00 (Alice)  |
 * | DE89370400440532013002  | €20.00 (Bob)    |
 */
@Component
public class WalletStore {

    private final Map<UUID,   DigitalEuroWallet> byWalletId  = new ConcurrentHashMap<>();
    private final Map<String, DigitalEuroWallet> byOwnerIban = new ConcurrentHashMap<>();

    public WalletStore() {
        seed("DE89370400440532013001", "50.00");
        seed("DE89370400440532013002", "20.00");
    }

    /** Creates and registers a new wallet for the given account. */
    public DigitalEuroWallet create(String ownerIban, BigDecimal initialBalance) {
        DigitalEuroWallet wallet = new DigitalEuroWallet(ownerIban, initialBalance);
        byWalletId.put(wallet.getWalletId(), wallet);
        byOwnerIban.put(ownerIban, wallet);
        return wallet;
    }

    public Optional<DigitalEuroWallet> findByWalletId(UUID walletId) {
        return Optional.ofNullable(byWalletId.get(walletId));
    }

    public Optional<DigitalEuroWallet> findByOwnerIban(String iban) {
        return Optional.ofNullable(byOwnerIban.get(iban));
    }

    private void seed(String iban, String balance) {
        create(iban, new BigDecimal(balance));
    }
}
