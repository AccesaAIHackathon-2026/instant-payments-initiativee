package eu.accesa.blinkpay.bank.model;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String iban, BigDecimal required, BigDecimal available) {
        super("Insufficient funds on %s: required %s, available %s"
                .formatted(iban, required, available));
    }
}
