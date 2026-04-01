package eu.accesa.blinkpay.fips.service;

import com.prowidesoftware.swift.model.mx.MxPacs00200110;
import com.prowidesoftware.swift.model.mx.MxPacs00800108;
import com.prowidesoftware.swift.model.mx.dic.CreditTransferTransaction39;
import com.prowidesoftware.swift.model.mx.dic.FIToFIPaymentStatusReportV10;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader91;
import com.prowidesoftware.swift.model.mx.dic.PaymentTransaction110;
import com.prowidesoftware.swift.model.mx.dic.StatusReason6Choice;
import com.prowidesoftware.swift.model.mx.dic.StatusReasonInformation12;
import eu.accesa.blinkpay.fips.model.Transaction;
import eu.accesa.blinkpay.fips.model.TransactionStatus;
import eu.accesa.blinkpay.fips.model.TransactionView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core FIPS settlement logic.
 *
 * Accepts ISO 20022 pacs.008.001.08 messages (via prowide-generated model classes)
 * and returns pacs.002.001.10 status reports. Message structure is fully derived
 * from the official EPC SCT Inst ISO 20022 XSD schemas — no hand-coded field mapping.
 *
 * Settlement lifecycle: RCVD → ACSP → ACSC (synchronous / instant for POC).
 *
 * SCT Inst rules enforced (ISO 20022 reject codes):
 *   AM01 — zero or negative amount
 *   AM02 — amount exceeds EUR 100,000 maximum
 *   AM03 — non-EUR currency
 *   AM05 — duplicate UETR
 *   AC01 — missing/invalid debtor IBAN or UETR
 *   AC03 — missing/invalid creditor IBAN
 */
@Service
public class FipsService {

    private static final Logger log = LoggerFactory.getLogger(FipsService.class);
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");
    private static final int MAX_PAGE_SIZE = 500;

    private final ConcurrentHashMap<UUID, Transaction> store = new ConcurrentHashMap<>();
    private final BankForwardingClient bankForwarder;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public FipsService(BankForwardingClient bankForwarder) {
        this.bankForwarder = bankForwarder;
    }

    public MxPacs00200110 submit(MxPacs00800108 request) {
        CreditTransferTransaction39 txInfo = extractTxInfo(request);

        var pmtId       = txInfo.getPmtId();
        var dbtrAcct    = txInfo.getDbtrAcct();
        var cdtrAcct    = txInfo.getCdtrAcct();
        var sttlmAmt    = txInfo.getIntrBkSttlmAmt();

        String uetrStr    = pmtId != null ? pmtId.getUETR() : null;
        String endToEndId = pmtId != null ? pmtId.getEndToEndId() : null;
        String debtorIBAN  = dbtrAcct != null && dbtrAcct.getId() != null ? dbtrAcct.getId().getIBAN() : null;
        String creditorIBAN = cdtrAcct != null && cdtrAcct.getId() != null ? cdtrAcct.getId().getIBAN() : null;
        BigDecimal amount   = sttlmAmt != null ? sttlmAmt.getValue() : null;
        String currency     = sttlmAmt != null ? sttlmAmt.getCcy() : null;
        String debtorName   = txInfo.getDbtr() != null ? txInfo.getDbtr().getNm() : null;
        String creditorName = txInfo.getCdtr() != null ? txInfo.getCdtr().getNm() : null;

        log.info("[FIPS] pacs.008 RCVD | uetr={} | {}→{} €{} {}",
                uetrStr, debtorIBAN, creditorIBAN, amount, currency);

        String rejectCode = validate(uetrStr, debtorIBAN, creditorIBAN, amount, currency);
        if (rejectCode != null) {
            log.warn("[FIPS] pacs.002 RJCT {} | uetr={}", rejectCode, uetrStr);
            UUID uetr = uetrStr != null ? parseUUID(uetrStr) : UUID.randomUUID();
            Transaction rejected = new Transaction(uetr, debtorIBAN, creditorIBAN, amount, currency,
                    debtorName, creditorName, endToEndId, null);
            rejected.setStatus(TransactionStatus.RJCT);
            rejected.setRejectReason(rejectCode);
            store.put(uetr, rejected);
            return buildPacs002(uetrStr, endToEndId, TransactionStatus.RJCT, null, rejectCode);
        }

        UUID uetr = UUID.fromString(uetrStr);
        Transaction tx = new Transaction(uetr, debtorIBAN, creditorIBAN, amount, currency,
                debtorName, creditorName, endToEndId, null);
        store.put(uetr, tx);

        // SCT Inst flow: RCVD → ACSP → forward to destination bank → ACSC
        log.info("[FIPS] RCVD→ACSP | uetr={}", uetr);
        tx.setStatus(TransactionStatus.ACSP);

        log.info("[FIPS] FORWARD→ | uetr={} | routing pacs.008 to destination bank", uetr);
        try {
            bankForwarder.forward(creditorIBAN, request);
        } catch (Exception e) {
            log.warn("[FIPS] FORWARD FAILED | uetr={} | {} — treating as RJCT AC03", uetr, e.getMessage());
            tx.setStatus(TransactionStatus.RJCT);
            tx.setRejectReason("AC03");
            store.put(uetr, tx);
            return buildPacs002(uetrStr, endToEndId, TransactionStatus.RJCT, null, "AC03");
        }

        log.info("[FIPS] ACSP→ACSC | uetr={}", uetr);
        tx.setStatus(TransactionStatus.ACSC);
        Instant settledAt = Instant.now();
        tx.setSettledAt(settledAt);

        log.info("[FIPS] pacs.002 ACSC | uetr={} | settledAt={}", uetr, settledAt);
        return buildPacs002(uetrStr, endToEndId, TransactionStatus.ACSC, settledAt, null);
    }

    public Optional<MxPacs00200110> getStatus(UUID uetr) {
        Transaction tx = store.get(uetr);
        if (tx == null) return Optional.empty();
        return Optional.of(buildPacs002(
                tx.getUetr().toString(),
                tx.getEndToEndId(),
                tx.getStatus(),
                tx.getSettledAt(),
                tx.getRejectReason()
        ));
    }

    public List<TransactionView> getAllTransactions(int limit) {
        int effectiveLimit = Math.min(limit, MAX_PAGE_SIZE);
        return store.values().stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .limit(effectiveLimit)
                .map(this::toView)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CreditTransferTransaction39 extractTxInfo(MxPacs00800108 request) {
        return request.getFIToFICstmrCdtTrf().getCdtTrfTxInf().get(0);
    }

    private MxPacs00200110 buildPacs002(String originalUetr, String originalEndToEndId,
                                        TransactionStatus status, Instant settledAt,
                                        String rejectCode) {
        GroupHeader91 grpHdr = new GroupHeader91()
                .setMsgId(UUID.randomUUID().toString())
                .setCreDtTm(OffsetDateTime.now(ZoneOffset.UTC));

        PaymentTransaction110 txStatus = new PaymentTransaction110()
                .setOrgnlUETR(originalUetr)
                .setOrgnlEndToEndId(originalEndToEndId)
                .setTxSts(status.name());

        if (settledAt != null) {
            txStatus.setAccptncDtTm(OffsetDateTime.ofInstant(settledAt, ZoneOffset.UTC));
        }
        if (rejectCode != null) {
            txStatus.addStsRsnInf(new StatusReasonInformation12()
                    .setRsn(new StatusReason6Choice().setCd(rejectCode)));
        }

        FIToFIPaymentStatusReportV10 report = new FIToFIPaymentStatusReportV10()
                .setGrpHdr(grpHdr)
                .addTxInfAndSts(txStatus);

        return new MxPacs00200110().setFIToFIPmtStsRpt(report);
    }

    private String validate(String uetrStr, String debtorIBAN, String creditorIBAN,
                            BigDecimal amount, String currency) {
        if (uetrStr == null || uetrStr.isBlank())          return "AC01";
        if (parseUUID(uetrStr) == null)                    return "AC01";
        if (store.containsKey(UUID.fromString(uetrStr)))   return "AM05";
        if (debtorIBAN == null || debtorIBAN.isBlank())    return "AC01";
        if (creditorIBAN == null || creditorIBAN.isBlank()) return "AC03";
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return "AM01";
        if (amount.compareTo(MAX_AMOUNT) > 0)              return "AM02";
        if (!"EUR".equals(currency))                       return "AM03";
        return null;
    }

    private UUID parseUUID(String s) {
        try { return UUID.fromString(s); }
        catch (IllegalArgumentException e) { return null; }
    }

    private TransactionView toView(Transaction tx) {
        return new TransactionView(
                tx.getUetr(), tx.getDebtorIBAN(), tx.getCreditorIBAN(),
                tx.getAmount(), tx.getCurrency(),
                tx.getDebtorName(), tx.getCreditorName(),
                tx.getEndToEndId(), tx.getStatus(),
                tx.getCreatedAt(), tx.getSettledAt(), tx.getRejectReason()
        );
    }
}
