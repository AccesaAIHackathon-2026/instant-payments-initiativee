package eu.accesa.blinkpay.bank.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory commercial bank account entity.
 *
 * Holds only the traditional bank balance. Digital Euro custody is managed
 * separately by {@link eu.accesa.blinkpay.bank.model.DigitalEuroWallet} and
 * {@link eu.accesa.blinkpay.bank.service.WalletStore}.
 *
 * All monetary mutations are synchronised on the instance.
 */
public class Account {

    private final String iban;
    private final String holderName;
    private final String phoneAlias;   // nullable — merchants have none
    private final AccountType accountType;

    private BigDecimal bankBalance;

    private final List<Transaction> transactions = new ArrayList<>();

    public Account(String iban, String holderName, String phoneAlias,
                   BigDecimal bankBalance, AccountType accountType) {
        this.iban = iban;
        this.holderName = holderName;
        this.phoneAlias = phoneAlias;
        this.bankBalance = bankBalance;
        this.accountType = accountType;
    }

    // --- thread-safe balance operations ------------------------------------

    /** Debits the bank balance only. Caller is responsible for the DE waterfall. */
    public synchronized void debit(BigDecimal amount) {
        if (bankBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(iban, amount, bankBalance);
        }
        bankBalance = bankBalance.subtract(amount);
    }

    public synchronized void credit(BigDecimal amount) {
        bankBalance = bankBalance.add(amount);
    }

    public synchronized void addTransaction(Transaction tx) {
        transactions.add(tx);
    }

    public synchronized List<Transaction> getTransactions() {
        return List.copyOf(transactions);
    }

    // --- getters -----------------------------------------------------------

    public String getIban()            { return iban; }
    public String getHolderName()      { return holderName; }
    public String getPhoneAlias()      { return phoneAlias; }
    public AccountType getAccountType(){ return accountType; }

    public synchronized BigDecimal getBankBalance() { return bankBalance; }
}
