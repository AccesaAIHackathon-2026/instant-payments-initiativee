package eu.accesa.blinkpay.bank.service;

import eu.accesa.blinkpay.bank.model.Account;
import eu.accesa.blinkpay.bank.model.AccountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory commercial bank account registry.
 *
 * Pre-seeded with the three POC test accounts. Digital Euro wallets for
 * the consumer accounts are managed separately by {@link WalletStore}.
 *
 * | Name               | IBAN                    | Phone        | Bank Balance | Type     |
 * |--------------------|-------------------------|--------------|--------------|----------|
 * | Alice Consumer     | DE89370400440532013001  | +49111000001 | €1 000.00    | CONSUMER |
 * | Bob Consumer       | DE89370400440532013002  | +49111000002 |   €500.00    | CONSUMER |
 * | Retail Store GmbH  | DE89370400440532013099  | —            |     €0.00    | MERCHANT |
 */
@Component
public class AccountStore {

    private static final String IBAN_PREFIX = "DE89370400440532013";

    private final Map<String, Account> byIban  = new ConcurrentHashMap<>();
    private final Map<String, String>  byAlias = new ConcurrentHashMap<>(); // alias → IBAN
    private final AtomicInteger ibanCounter = new AtomicInteger(100);

    public AccountStore() {
        seed("001", "Alice Consumer",    "+49111000001", "1000.00", AccountType.CONSUMER);
        seed("002", "Bob Consumer",      "+49111000002",  "500.00", AccountType.CONSUMER);
        seed("099", "Retail Store GmbH", null,              "0.00", AccountType.MERCHANT);
    }

    public Optional<Account> findByIban(String iban) {
        return Optional.ofNullable(byIban.get(iban));
    }

    public Optional<Account> findByAlias(String alias) {
        return Optional.ofNullable(byAlias.get(alias)).flatMap(this::findByIban);
    }

    public Collection<Account> all() {
        return byIban.values();
    }

    /**
     * Registers a new account and returns it.
     * IBAN is auto-assigned; phoneAlias may be null for merchants.
     */
    public Account register(String holderName, String phoneAlias,
                            AccountType type, BigDecimal bankBalance) {
        if (phoneAlias != null && byAlias.containsKey(phoneAlias)) {
            throw new ValidationException("Phone alias already registered: " + phoneAlias);
        }
        String suffix = String.format("%03d", ibanCounter.getAndIncrement());
        Account acc = new Account(IBAN_PREFIX + suffix, holderName, phoneAlias,
                bankBalance, type);
        byIban.put(acc.getIban(), acc);
        if (phoneAlias != null) byAlias.put(phoneAlias, acc.getIban());
        return acc;
    }

    private void seed(String suffix, String name, String phone,
                      String bank, AccountType type) {
        Account acc = new Account(IBAN_PREFIX + suffix, name, phone,
                new BigDecimal(bank), type);
        byIban.put(acc.getIban(), acc);
        if (phone != null) byAlias.put(phone, acc.getIban());
    }
}
