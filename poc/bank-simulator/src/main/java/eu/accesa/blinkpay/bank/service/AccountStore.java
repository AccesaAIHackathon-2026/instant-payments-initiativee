package eu.accesa.blinkpay.bank.service;

import eu.accesa.blinkpay.bank.model.Account;
import eu.accesa.blinkpay.bank.model.AccountType;
import org.springframework.beans.factory.annotation.Value;
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
 * Seeded accounts depend on the configured bank identity:
 *
 * bank-a (DE89370400440532013xxx):
 *   Alice Consumer      | 001 | +49111000001 | €1 000.00 | CONSUMER
 *   Bob Consumer        | 002 | +49111000002 |   €500.00 | CONSUMER
 *   MediaMarkt Saturn   | 099 | —            |     €0.00 | MERCHANT
 *
 * bank-b (DE89370400440532014xxx):
 *   Charlie Consumer | 001 | +49222000001 | €800.00 | CONSUMER
 *   REWE Group       | 099 | —            |   €0.00 | MERCHANT
 */
@Component
public class AccountStore {

    private final String ibanPrefix;
    private final Map<String, Account> byIban  = new ConcurrentHashMap<>();
    private final Map<String, String>  byAlias = new ConcurrentHashMap<>(); // alias → IBAN
    private final AtomicInteger ibanCounter = new AtomicInteger(100);

    public AccountStore(@Value("${bank.id}") String bankId,
                        @Value("${bank.iban-prefix}") String ibanPrefix) {
        this.ibanPrefix = ibanPrefix;
        if ("bank-b".equals(bankId)) {
            seed("001", "Charlie Consumer",  "+49222000001", "800.00", AccountType.CONSUMER);
            seed("099", "REWE Group",         null,            "0.00", AccountType.MERCHANT);
            // Audience accounts — bank-b (PINs 2001-2010)
            seed("002", "Luca Rossi",         "+49222000002", "500.00", AccountType.CONSUMER);
            seed("003", "Emma Laurent",       "+49222000003", "500.00", AccountType.CONSUMER);
            seed("004", "Marco Bianchi",      "+49222000004", "500.00", AccountType.CONSUMER);
            seed("005", "Sophie Dubois",      "+49222000005", "500.00", AccountType.CONSUMER);
            seed("006", "Jan van der Berg",   "+49222000006", "500.00", AccountType.CONSUMER);
            seed("007", "Elena Popescu",      "+49222000007", "500.00", AccountType.CONSUMER);
            seed("008", "Carlos Garcia",      "+49222000008", "500.00", AccountType.CONSUMER);
            seed("009", "Ingrid Johansson",   "+49222000009", "500.00", AccountType.CONSUMER);
            seed("010", "Andrei Ionescu",     "+49222000010", "500.00", AccountType.CONSUMER);
            seed("011", "Freya Nielsen",      "+49222000011", "500.00", AccountType.CONSUMER);
        } else {
            // bank-a (default)
            seed("001", "Alice Consumer",    "+49111000001", "1000.00", AccountType.CONSUMER);
            seed("002", "Bob Consumer",      "+49111000002",  "500.00", AccountType.CONSUMER);
            seed("099", "MediaMarkt Saturn",  null,              "0.00", AccountType.MERCHANT);
            // Audience accounts — bank-a (PINs 1001-1010)
            seed("003", "Hans Mueller",      "+49111000003", "500.00", AccountType.CONSUMER);
            seed("004", "Maria Schmidt",     "+49111000004", "500.00", AccountType.CONSUMER);
            seed("005", "Klaus Weber",       "+49111000005", "500.00", AccountType.CONSUMER);
            seed("006", "Anna Fischer",      "+49111000006", "500.00", AccountType.CONSUMER);
            seed("007", "Thomas Meyer",      "+49111000007", "500.00", AccountType.CONSUMER);
            seed("008", "Laura Wagner",      "+49111000008", "500.00", AccountType.CONSUMER);
            seed("009", "Stefan Becker",     "+49111000009", "500.00", AccountType.CONSUMER);
            seed("010", "Julia Hoffmann",    "+49111000010", "500.00", AccountType.CONSUMER);
            seed("011", "Michael Schulz",    "+49111000011", "500.00", AccountType.CONSUMER);
            seed("012", "Sophie Koch",       "+49111000012", "500.00", AccountType.CONSUMER);
        }
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
        Account acc = new Account(ibanPrefix + suffix, holderName, phoneAlias,
                bankBalance, type);
        byIban.put(acc.getIban(), acc);
        if (phoneAlias != null) byAlias.put(phoneAlias, acc.getIban());
        return acc;
    }

    private void seed(String suffix, String name, String phone,
                      String bank, AccountType type) {
        Account acc = new Account(ibanPrefix + suffix, name, phone,
                new BigDecimal(bank), type);
        byIban.put(acc.getIban(), acc);
        if (phone != null) byAlias.put(phone, acc.getIban());
    }
}
