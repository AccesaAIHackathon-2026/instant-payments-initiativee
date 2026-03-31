package eu.accesa.blinkpay.bank.fips;

import java.util.UUID;

public class FipsRejectedException extends RuntimeException {

    private final String rejectCode;

    public FipsRejectedException(UUID uetr, String rejectCode) {
        super("FIPS rejected payment %s with code %s".formatted(uetr, rejectCode));
        this.rejectCode = rejectCode;
    }

    public String getRejectCode() { return rejectCode; }
}
