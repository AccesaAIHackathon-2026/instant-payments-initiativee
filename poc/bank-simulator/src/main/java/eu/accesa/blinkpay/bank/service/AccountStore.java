package eu.accesa.blinkpay.bank.service;

import eu.accesa.blinkpay.bank.model.Account;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory account registry, pre-seeded with the three POC test accounts.
 *
 * | Name               | IBAN                    | Phone        | Bank    | Digital Euro |
 * |--------------------|-------------------------|--------------|---------|--------------|
 * | Alice Consumer     | DE89370400440532013001  | +49111000001 | 1000.00 | 50.00        |
 * | Bob Consumer       | DE89370400440532013002  | +49111000002 | 500.00  | 20.00        |
 * | Retail Store GmbH  | DE89370400440532013099  | —            | 0.00    | 0.00         |
 */
@Component
public class AccountStore {

    private final Map<String, Account> byIban = new ConcurrentHashMap<>();
    private final Map<String, String>  byAlias = new ConcurrentHashMap<>(); // alias → IBAN

    public AccountStore() {
        seed("DE89370400440532013001", "Alice Consumer",    "+49111000001", "1000.00", "50.00");
        seed("DE89370400440532013002", "Bob Consumer",      "+49111000002",  "500.00", "20.00");
        seed("DE89370400440532013099", "Retail Store GmbH", null,              "0.00",  "0.00");
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

    private void seed(String iban, String name, String phone,
                      String bank, String de) {
        Account acc = new Account(iban, name, phone,
                new BigDecimal(bank), new BigDecimal(de));
        byIban.put(iban, acc);
        if (phone != null) byAlias.put(phone, iban);
    }
}
