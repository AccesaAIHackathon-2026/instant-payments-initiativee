package eu.accesa.blinkpay.bank.fips;

import com.prowidesoftware.swift.model.mx.MxPacs00200110;
import com.prowidesoftware.swift.model.mx.MxPacs00800108;
import com.prowidesoftware.swift.model.mx.dic.AccountIdentification4Choice;
import com.prowidesoftware.swift.model.mx.dic.ActiveCurrencyAndAmount;
import com.prowidesoftware.swift.model.mx.dic.CashAccount38;
import com.prowidesoftware.swift.model.mx.dic.CreditTransferTransaction39;
import com.prowidesoftware.swift.model.mx.dic.FIToFICustomerCreditTransferV08;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader93;
import com.prowidesoftware.swift.model.mx.dic.PartyIdentification135;
import com.prowidesoftware.swift.model.mx.dic.PaymentIdentification7;
import com.prowidesoftware.swift.model.mx.dic.PaymentTransaction110;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * HTTP client for the FIPS simulator.
 *
 * Builds ISO 20022 pacs.008.001.08 messages using prowide model classes
 * (matching the FIPS simulator's expected request format) and parses the
 * pacs.002.001.10 response.
 */
@Component
public class FipsClient {

    private final RestClient restClient;

    public FipsClient(RestClient fipsRestClient) {
        this.restClient = fipsRestClient;
    }

    /**
     * Submits a pacs.008 payment to FIPS and returns the settlement status.
     *
     * @throws FipsRejectedException if FIPS returns RJCT
     */
    public void submit(UUID uetr, String debtorIBAN, String creditorIBAN,
                       BigDecimal amount, String debtorName, String creditorName,
                       String endToEndId, String remittanceInfo) {

        MxPacs00800108 pacs008 = buildPacs008(uetr, debtorIBAN, creditorIBAN,
                amount, debtorName, creditorName, endToEndId);

        MxPacs00200110 pacs002 = restClient.post()
                .uri("/fips/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(pacs008)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { /* parse body regardless */ })
                .body(MxPacs00200110.class);

        if (pacs002 == null) throw new FipsRejectedException(uetr, "NO_RESPONSE");

        PaymentTransaction110 txInfo = pacs002.getFIToFIPmtStsRpt()
                .getTxInfAndSts().getFirst();

        if ("RJCT".equals(txInfo.getTxSts())) {
            String code = extractRejectCode(txInfo);
            throw new FipsRejectedException(uetr, code);
        }
    }

    // -------------------------------------------------------------------------

    private MxPacs00800108 buildPacs008(UUID uetr, String debtorIBAN, String creditorIBAN,
                                        BigDecimal amount, String debtorName, String creditorName,
                                        String endToEndId) {
        CreditTransferTransaction39 txInfo = new CreditTransferTransaction39()
                .setPmtId(new PaymentIdentification7()
                        .setUETR(uetr.toString())
                        .setEndToEndId(endToEndId))
                .setIntrBkSttlmAmt(new ActiveCurrencyAndAmount()
                        .setValue(amount)
                        .setCcy("EUR"))
                .setDbtr(new PartyIdentification135().setNm(debtorName))
                .setDbtrAcct(new CashAccount38()
                        .setId(new AccountIdentification4Choice().setIBAN(debtorIBAN)))
                .setCdtr(new PartyIdentification135().setNm(creditorName))
                .setCdtrAcct(new CashAccount38()
                        .setId(new AccountIdentification4Choice().setIBAN(creditorIBAN)));

        FIToFICustomerCreditTransferV08 transfer = new FIToFICustomerCreditTransferV08()
                .setGrpHdr(new GroupHeader93()
                        .setMsgId(UUID.randomUUID().toString())
                        .setCreDtTm(OffsetDateTime.now(ZoneOffset.UTC)))
                .addCdtTrfTxInf(txInfo);

        return new MxPacs00800108().setFIToFICstmrCdtTrf(transfer);
    }

    private String extractRejectCode(PaymentTransaction110 txInfo) {
        List<?> reasons = txInfo.getStsRsnInf();
        if (reasons == null || reasons.isEmpty()) return "UNKNOWN";
        var rsn = txInfo.getStsRsnInf().getFirst().getRsn();
        return rsn != null ? rsn.getCd() : "UNKNOWN";
    }
}
