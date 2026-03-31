package eu.accesa.blinkpay.fips.util;

import com.prowidesoftware.swift.model.mx.MxPacs00800108;
import com.prowidesoftware.swift.model.mx.dic.AccountIdentification4Choice;
import com.prowidesoftware.swift.model.mx.dic.ActiveCurrencyAndAmount;
import com.prowidesoftware.swift.model.mx.dic.CashAccount38;
import com.prowidesoftware.swift.model.mx.dic.CreditTransferTransaction39;
import com.prowidesoftware.swift.model.mx.dic.FIToFICustomerCreditTransferV08;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader93;
import com.prowidesoftware.swift.model.mx.dic.PartyIdentification135;
import com.prowidesoftware.swift.model.mx.dic.PaymentIdentification7;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Builds minimal but valid MxPacs00800108 messages for integration tests.
 *
 * Passing null for any parameter omits that field, allowing tests to exercise
 * validation rejection paths (missing IBAN, missing amount, etc.).
 */
public class Pacs008Factory {

    public static final String ALICE_IBAN  = "DE89370400440532013001";
    public static final String BOB_IBAN    = "DE89370400440532013002";
    public static final String RETAIL_IBAN = "DE89370400440532013099";

    /** Standard valid payment — all required fields populated. */
    public static MxPacs00800108 valid(UUID uetr, String debtorIBAN, String creditorIBAN, BigDecimal amount) {
        return build(uetr, debtorIBAN, creditorIBAN, amount, "EUR", "Alice Consumer", "Bob Consumer");
    }

    /** Convenience overload with default Alice → Bob IBANs. */
    public static MxPacs00800108 aliceToBob(UUID uetr, BigDecimal amount) {
        return valid(uetr, ALICE_IBAN, BOB_IBAN, amount);
    }

    /** Convenience overload for Alice paying the retail store. */
    public static MxPacs00800108 aliceToRetail(UUID uetr, BigDecimal amount) {
        return build(uetr, ALICE_IBAN, RETAIL_IBAN, amount, "EUR", "Alice Consumer", "Retail Store GmbH");
    }

    public static MxPacs00800108 withCurrency(UUID uetr, String currency) {
        return build(uetr, ALICE_IBAN, BOB_IBAN, new BigDecimal("10.00"), currency, "Alice Consumer", "Bob Consumer");
    }

    public static MxPacs00800108 withAmount(UUID uetr, BigDecimal amount) {
        return build(uetr, ALICE_IBAN, BOB_IBAN, amount, "EUR", "Alice Consumer", "Bob Consumer");
    }

    public static MxPacs00800108 withoutDebtorIBAN(UUID uetr) {
        return build(uetr, null, BOB_IBAN, new BigDecimal("10.00"), "EUR", "Alice Consumer", "Bob Consumer");
    }

    public static MxPacs00800108 withoutCreditorIBAN(UUID uetr) {
        return build(uetr, ALICE_IBAN, null, new BigDecimal("10.00"), "EUR", "Alice Consumer", "Bob Consumer");
    }

    public static MxPacs00800108 withoutUetr() {
        return build(null, ALICE_IBAN, BOB_IBAN, new BigDecimal("10.00"), "EUR", "Alice Consumer", "Bob Consumer");
    }

    // -------------------------------------------------------------------------

    private static MxPacs00800108 build(UUID uetr, String debtorIBAN, String creditorIBAN,
                                        BigDecimal amount, String currency,
                                        String debtorName, String creditorName) {
        CreditTransferTransaction39 txInfo = new CreditTransferTransaction39();

        if (uetr != null) {
            txInfo.setPmtId(new PaymentIdentification7()
                    .setUETR(uetr.toString())
                    .setEndToEndId("E2E-" + uetr));
        }

        if (amount != null) {
            txInfo.setIntrBkSttlmAmt(new ActiveCurrencyAndAmount()
                    .setValue(amount)
                    .setCcy(currency));
        }

        if (debtorIBAN != null) {
            txInfo.setDbtr(new PartyIdentification135().setNm(debtorName));
            txInfo.setDbtrAcct(new CashAccount38()
                    .setId(new AccountIdentification4Choice().setIBAN(debtorIBAN)));
        }

        if (creditorIBAN != null) {
            txInfo.setCdtr(new PartyIdentification135().setNm(creditorName));
            txInfo.setCdtrAcct(new CashAccount38()
                    .setId(new AccountIdentification4Choice().setIBAN(creditorIBAN)));
        }

        FIToFICustomerCreditTransferV08 transfer = new FIToFICustomerCreditTransferV08()
                .setGrpHdr(new GroupHeader93()
                        .setMsgId(UUID.randomUUID().toString())
                        .setCreDtTm(OffsetDateTime.now(ZoneOffset.UTC)))
                .addCdtTrfTxInf(txInfo);

        return new MxPacs00800108().setFIToFICstmrCdtTrf(transfer);
    }
}
