package eu.accesa.blinkpay.bank.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory account entity.
 *
 * Holds two distinct balances per the Digital Euro two-wallet model:
 *   - bankBalance      — commercial bank money (traditional)
 *   - digitalEuroBalance — Digital Euro (CBDC, settled via TIPS DE module)
 *
 * All monetary mutations are synchronised on the instance to keep the POC
 * thread-safe without a full transaction manager.
 */
public class Account {

    private final String iban;
    private final String holderName;
    private final String phoneAlias;   // nullable — retail store has none

    private BigDecimal bankBalance;
    private BigDecimal digitalEuroBalance;

    private final List<Transaction> transactions = new ArrayList<>();

    public Account(String iban, String holderName, String phoneAlias,
                   BigDecimal bankBalance, BigDecimal digitalEuroBalance) {
        this.iban = iban;
        this.holderName = holderName;
        this.phoneAlias = phoneAlias;
        this.bankBalance = bankBalance;
        this.digitalEuroBalance = digitalEuroBalance;
    }

    // --- thread-safe balance operations ------------------------------------

    public synchronized void debit(BigDecimal amount) {
        if (bankBalance.add(digitalEuroBalance).compareTo(amount) < 0) {
            throw new InsufficientFundsException(iban, amount,
                    bankBalance.add(digitalEuroBalance));
        }
        // Waterfall: spend Digital Euro first, top up from bank if needed
        if (digitalEuroBalance.compareTo(amount) >= 0) {
            digitalEuroBalance = digitalEuroBalance.subtract(amount);
        } else {
            BigDecimal shortfall = amount.subtract(digitalEuroBalance);
            digitalEuroBalance = BigDecimal.ZERO;
            bankBalance = bankBalance.subtract(shortfall);
        }
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

    public String getIban() { return iban; }
    public String getHolderName() { return holderName; }
    public String getPhoneAlias() { return phoneAlias; }

    public synchronized BigDecimal getBankBalance() { return bankBalance; }
    public synchronized BigDecimal getDigitalEuroBalance() { return digitalEuroBalance; }
}
