package eu.accesa.blinkpay.bank.service;

import eu.accesa.blinkpay.bank.model.DigitalEuroWallet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory Digital Euro wallet registry.
 *
 * Pre-seeded wallets per bank identity (consumers only — merchants have no wallet):
 *
 * bank-a: Alice €50, Bob €20
 * bank-b: Charlie €30
 */
@Component
public class WalletStore {

    private final Map<UUID,   DigitalEuroWallet> byWalletId  = new ConcurrentHashMap<>();
    private final Map<String, DigitalEuroWallet> byOwnerIban = new ConcurrentHashMap<>();

    public WalletStore(@Value("${bank.id}") String bankId,
                       @Value("${bank.iban-prefix}") String ibanPrefix) {
        if ("bank-b".equals(bankId)) {
            seed(ibanPrefix + "001", "30.00");  // Charlie
        } else {
            seed(ibanPrefix + "001", "50.00");  // Alice
            seed(ibanPrefix + "002", "20.00");  // Bob
        }
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
