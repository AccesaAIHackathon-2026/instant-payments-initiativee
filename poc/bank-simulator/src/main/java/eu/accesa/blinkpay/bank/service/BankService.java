package eu.accesa.blinkpay.bank.service;

import eu.accesa.blinkpay.bank.dto.AccountView;
import eu.accesa.blinkpay.bank.dto.PaymentInitiatedResponse;
import eu.accesa.blinkpay.bank.dto.PaymentRequest;
import eu.accesa.blinkpay.bank.dto.PaymentResult;
import eu.accesa.blinkpay.bank.dto.ProxyLookupResponse;
import eu.accesa.blinkpay.bank.dto.RegisterRequest;
import eu.accesa.blinkpay.bank.dto.RegisterResponse;
import eu.accesa.blinkpay.bank.dto.RtpRequest;
import eu.accesa.blinkpay.bank.dto.RtpView;
import eu.accesa.blinkpay.bank.dto.ScaRequest;
import eu.accesa.blinkpay.bank.dto.VopResponse;
import eu.accesa.blinkpay.bank.dto.WalletTransferRequest;
import eu.accesa.blinkpay.bank.dto.WalletTransferResponse;
import eu.accesa.blinkpay.bank.dto.WalletView;
import com.prowidesoftware.swift.model.mx.MxPacs00200110;
import com.prowidesoftware.swift.model.mx.MxPacs00800108;
import com.prowidesoftware.swift.model.mx.dic.CreditTransferTransaction39;
import com.prowidesoftware.swift.model.mx.dic.FIToFIPaymentStatusReportV10;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader91;
import com.prowidesoftware.swift.model.mx.dic.PaymentTransaction110;
import eu.accesa.blinkpay.bank.fips.FipsClient;
import eu.accesa.blinkpay.bank.fips.FipsRejectedException;
import eu.accesa.blinkpay.bank.model.Account;
import eu.accesa.blinkpay.bank.model.AccountType;
import eu.accesa.blinkpay.bank.model.DigitalEuroWallet;
import eu.accesa.blinkpay.bank.model.RequestToPay;
import eu.accesa.blinkpay.bank.model.RtpStatus;
import eu.accesa.blinkpay.bank.model.Transaction;
import eu.accesa.blinkpay.bank.model.TransactionStatus;
import eu.accesa.blinkpay.bank.model.VopResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core bank simulator business logic.
 *
 * Orchestrates: proxy lookup → VoP → SCA challenge issuance →
 * DE waterfall → bank debit → FIPS submission → credit → transaction recording.
 *
 * Digital Euro custody is separated from the commercial bank account:
 *   - {@link AccountStore}  manages bank balances (commercial bank liability)
 *   - {@link WalletStore}   manages DE balances (custodied on behalf of ECB)
 */
@Service
public class BankService {

    private static final Logger log = LoggerFactory.getLogger(BankService.class);
    private static final String CORRECT_PIN = "1234";

    private final AccountStore accounts;
    private final WalletStore wallets;
    private final FipsClient fips;
    private final SseNotificationService sse;
    private final String ibanPrefix;

    // Pending payments awaiting SCA confirmation: uetr → PendingPayment context
    private final Map<String, PendingPayment> pendingPayments = new ConcurrentHashMap<>();

    // RTP store: rtpId → RequestToPay
    private final Map<UUID, RequestToPay> rtpStore = new ConcurrentHashMap<>();

    public BankService(AccountStore accounts, WalletStore wallets,
                       FipsClient fips, SseNotificationService sse,
                       @Value("${bank.iban-prefix}") String ibanPrefix) {
        this.accounts   = accounts;
        this.wallets    = wallets;
        this.fips       = fips;
        this.sse        = sse;
        this.ibanPrefix = ibanPrefix;
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    public RegisterResponse register(RegisterRequest req) {
        if (req.accountType() == null) throw new ValidationException("accountType is required");
        if (req.holderName() == null || req.holderName().isBlank()) throw new ValidationException("holderName is required");
        if (req.accountType() == AccountType.CONSUMER && (req.phoneAlias() == null || req.phoneAlias().isBlank())) {
            throw new ValidationException("phoneAlias is required for CONSUMER accounts");
        }
        if (req.accountType() == AccountType.MERCHANT && req.phoneAlias() != null) {
            throw new ValidationException("phoneAlias must not be set for MERCHANT accounts");
        }

        BigDecimal bank = req.accountType() == AccountType.MERCHANT ? BigDecimal.ZERO
                : (req.bankBalance() != null ? req.bankBalance() : BigDecimal.ZERO);

        Account acc = accounts.register(req.holderName(), req.phoneAlias(), req.accountType(), bank);

        // Consumers get a Digital Euro custody wallet; merchants do not.
        UUID walletId = null;
        if (acc.getAccountType() == AccountType.CONSUMER) {
            BigDecimal initialWallet = req.digitalEuroBalance() != null
                    ? req.digitalEuroBalance() : BigDecimal.ZERO;
            walletId = wallets.create(acc.getIban(), initialWallet).getWalletId();
        }

        log.info("[REGISTER] {} | {} | IBAN={} alias={} bank=€{} de=€{}",
                acc.getAccountType(), acc.getHolderName(), acc.getIban(),
                acc.getPhoneAlias(), acc.getBankBalance(),
                walletId != null ? req.digitalEuroBalance() : "n/a");

        return new RegisterResponse(acc.getIban(), acc.getHolderName(), acc.getAccountType(),
                acc.getPhoneAlias(), acc.getBankBalance(), walletId);
    }

    // -------------------------------------------------------------------------
    // Proxy lookup
    // -------------------------------------------------------------------------

    public ProxyLookupResponse proxyLookup(String alias) {
        Account acc = accounts.findByAlias(alias)
                .orElseThrow(() -> new NotFoundException("No account found for alias: " + alias));
        return new ProxyLookupResponse(acc.getIban(), acc.getHolderName());
    }

    // -------------------------------------------------------------------------
    // Verification of Payee (VoP)
    // -------------------------------------------------------------------------

    public VopResponse verifyPayee(String iban, String name) {
        Optional<Account> opt = accounts.findByIban(iban);
        if (opt.isEmpty()) return new VopResponse(VopResult.NOT_FOUND, null);

        String actual = opt.get().getHolderName();
        VopResult result = match(actual, name);
        return new VopResponse(result, actual);
    }

    // -------------------------------------------------------------------------
    // Account info
    // -------------------------------------------------------------------------

    public AccountView getAccount(String iban) {
        Account acc = accounts.findByIban(iban)
                .orElseThrow(() -> new NotFoundException("Account not found: " + iban));
        return toView(acc);
    }

    public List<Transaction> getTransactions(String iban) {
        Account acc = accounts.findByIban(iban)
                .orElseThrow(() -> new NotFoundException("Account not found: " + iban));
        return acc.getTransactions().stream()
                .sorted(Comparator.comparing(Transaction::createdAt).reversed())
                .toList();
    }

    // -------------------------------------------------------------------------
    // Wallet info
    // -------------------------------------------------------------------------

    public WalletView getWallet(UUID walletId) {
        DigitalEuroWallet wallet = wallets.findByWalletId(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet not found: " + walletId));
        return toWalletView(wallet);
    }

    // -------------------------------------------------------------------------
    // Digital Euro wallet — top-up and redeem
    // -------------------------------------------------------------------------

    /**
     * Top-up: move funds from the owner's bank account into their DE custody wallet.
     * Used to pre-load the wallet before an offline NFC payment.
     */
    public WalletTransferResponse topUpWallet(UUID walletId, WalletTransferRequest req) {
        if (req.amount() == null || req.amount().signum() <= 0)
            throw new ValidationException("Amount must be positive");

        DigitalEuroWallet wallet = wallets.findByWalletId(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet not found: " + walletId));
        Account account = accounts.findByIban(wallet.getOwnerIban())
                .orElseThrow(() -> new NotFoundException("Account not found: " + wallet.getOwnerIban()));

        account.debit(req.amount());   // throws InsufficientFundsException if short
        wallet.credit(req.amount());

        log.info("[WALLET] TOP-UP | walletId={} | bank -€{} → de +€{} | newBank=€{} newDe=€{}",
                walletId, req.amount(), req.amount(), account.getBankBalance(), wallet.getBalance());

        return new WalletTransferResponse(wallet.getWalletId(), wallet.getOwnerIban(),
                wallet.getBalance(), account.getBankBalance());
    }

    /**
     * Redeem: move funds from the DE custody wallet back into the owner's bank account.
     * Used to convert unused DE balance back to commercial bank money.
     */
    public WalletTransferResponse redeemWallet(UUID walletId, WalletTransferRequest req) {
        if (req.amount() == null || req.amount().signum() <= 0)
            throw new ValidationException("Amount must be positive");

        DigitalEuroWallet wallet = wallets.findByWalletId(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet not found: " + walletId));
        Account account = accounts.findByIban(wallet.getOwnerIban())
                .orElseThrow(() -> new NotFoundException("Account not found: " + wallet.getOwnerIban()));

        if (wallet.getBalance().compareTo(req.amount()) < 0)
            throw new ValidationException("Insufficient DE wallet balance: have €"
                    + wallet.getBalance() + ", need €" + req.amount());

        wallet.debit(req.amount());
        account.credit(req.amount());

        log.info("[WALLET] REDEEM | walletId={} | de -€{} → bank +€{} | newBank=€{} newDe=€{}",
                walletId, req.amount(), req.amount(), account.getBankBalance(), wallet.getBalance());

        return new WalletTransferResponse(wallet.getWalletId(), wallet.getOwnerIban(),
                wallet.getBalance(), account.getBankBalance());
    }

    // -------------------------------------------------------------------------
    // Payment flow — step 1: initiate (returns SCA challenge)
    // -------------------------------------------------------------------------

    public PaymentInitiatedResponse initiatePayment(PaymentRequest req) {
        Account debtor = accounts.findByIban(req.debtorIBAN())
                .orElseThrow(() -> new NotFoundException("Debtor account not found: " + req.debtorIBAN()));

        // Resolve creditor IBAN — either direct (QR) or via proxy (P2P)
        String creditorIBAN;
        String creditorName;
        if (req.creditorIBAN() != null && !req.creditorIBAN().isBlank()) {
            creditorIBAN = req.creditorIBAN();
            creditorName = accounts.findByIban(creditorIBAN)
                    .map(Account::getHolderName)
                    .orElse("Unknown");
        } else if (req.creditorAlias() != null && !req.creditorAlias().isBlank()) {
            Account creditor = accounts.findByAlias(req.creditorAlias())
                    .orElseThrow(() -> new NotFoundException("No account for alias: " + req.creditorAlias()));
            creditorIBAN = creditor.getIban();
            creditorName = creditor.getHolderName();
        } else {
            throw new ValidationException("Either creditorIBAN or creditorAlias must be provided");
        }

        if (req.amount() == null || req.amount().signum() <= 0) {
            throw new ValidationException("Amount must be positive");
        }

        UUID uetr = UUID.randomUUID();

        pendingPayments.put(uetr.toString(), new PendingPayment(
                uetr, debtor.getIban(), creditorIBAN,
                debtor.getHolderName(), creditorName,
                req.amount(), req.creditorReference(), req.remittanceInfo()));

        log.info("[PAY] INITIATED | uetr={} | {}→{} | €{} | ref={}",
                uetr, debtor.getIban(), creditorIBAN, req.amount(), req.creditorReference());

        return new PaymentInitiatedResponse(uetr, uetr.toString(), creditorName, creditorIBAN, req.amount());
    }

    // -------------------------------------------------------------------------
    // Payment flow — step 2: SCA confirmation → settlement
    // -------------------------------------------------------------------------

    public PaymentResult confirmPayment(ScaRequest sca) {
        if (!CORRECT_PIN.equals(sca.pin())) {
            log.warn("[SCA] WRONG_PIN | uetr={} rtpId={}", sca.uetr(), sca.rtpId());
            throw new ValidationException("Invalid PIN");
        }

        // RTP path (Flow B2)
        if (sca.rtpId() != null) {
            log.info("[SCA] CONFIRMED | rtpId={}", sca.rtpId());
            return confirmRtp(sca.rtpId());
        }

        // Direct payment path (Flow A, B1)
        if (sca.uetr() == null) throw new ValidationException("uetr is required for direct payment SCA");
        log.info("[SCA] CONFIRMED | uetr={}", sca.uetr());
        PendingPayment pending = Optional.ofNullable(
                        pendingPayments.remove(sca.uetr().toString()))
                .orElseThrow(() -> new NotFoundException("No pending payment for UETR: " + sca.uetr()));

        return settle(pending);
    }

    // -------------------------------------------------------------------------
    // Request-to-Pay (Flow B2 — stretch)
    // -------------------------------------------------------------------------

    public RtpView createRtp(RtpRequest req) {
        Account creditor = accounts.findByIban(req.creditorIBAN())
                .orElseThrow(() -> new NotFoundException("Creditor account not found: " + req.creditorIBAN()));
        Account debtor = accounts.findByAlias(req.debtorAlias())
                .orElseThrow(() -> new NotFoundException("No account for alias: " + req.debtorAlias()));

        RequestToPay rtp = new RequestToPay(
                debtor.getIban(), creditor.getIban(), creditor.getHolderName(),
                req.amount(), req.remittanceInfo());
        rtpStore.put(rtp.getRtpId(), rtp);
        return toRtpView(rtp);
    }

    public List<RtpView> getIncomingRtps(String debtorIBAN) {
        return rtpStore.values().stream()
                .filter(r -> debtorIBAN.equals(r.getDebtorIBAN()))
                .filter(r -> r.getStatus() == RtpStatus.PENDING)
                .map(this::toRtpView)
                .toList();
    }

    public RtpView getRtpStatus(UUID rtpId) {
        RequestToPay rtp = rtpStore.get(rtpId);
        if (rtp == null) throw new NotFoundException("RTP not found: " + rtpId);
        return toRtpView(rtp);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private PaymentResult settle(PendingPayment p) {
        Account debtor = accounts.findByIban(p.debtorIBAN()).orElseThrow();
        boolean isIntraBank = p.creditorIBAN().startsWith(ibanPrefix);

        BigDecimal deBalance = wallets.findByOwnerIban(p.debtorIBAN())
                .map(DigitalEuroWallet::getBalance).orElse(BigDecimal.ZERO);

        log.info("[SETTLE] BEGIN | uetr={} | €{} | bank=€{} de=€{} | {}",
                p.uetr(), p.amount(), debtor.getBankBalance(), deBalance,
                isIntraBank ? "INTRA-BANK" : "INTER-BANK");

        // A2A: debit bank balance only — DE wallet is managed via top-up/redeem
        if (debtor.getBankBalance().compareTo(p.amount()) < 0) {
            log.warn("[SETTLE] RJCT AM04 | uetr={} | need=€{} have=€{}", p.uetr(), p.amount(), debtor.getBankBalance());
            Transaction rejected = Transaction.rejected(p.uetr(), p.debtorIBAN(), p.creditorIBAN(),
                    p.debtorName(), p.creditorName(), p.amount(),
                    p.creditorReference(), p.remittanceInfo());
            debtor.addTransaction(rejected);
            sse.notifyRejection(p.creditorReference(), rejected);
            return new PaymentResult(p.uetr(), TransactionStatus.RJCT, "AM04");
        }

        debtor.debit(p.amount());
        log.info("[SETTLE] DEBIT | uetr={} | bank -€{} → remaining=€{}", p.uetr(), p.amount(), debtor.getBankBalance());

        // --- Intra-bank: creditor is on this bank — settle directly, no FIPS --
        if (isIntraBank) {
            Account creditor = accounts.findByIban(p.creditorIBAN()).orElseThrow();
            creditor.credit(p.amount());

            Transaction settled = Transaction.settled(p.uetr(), p.debtorIBAN(), p.creditorIBAN(),
                    p.debtorName(), p.creditorName(), p.amount(),
                    p.creditorReference(), p.remittanceInfo());
            debtor.addTransaction(settled);
            creditor.addTransaction(settled);

            log.info("[SETTLE] ACSC (intra) | uetr={} | {}→{} €{} | ref={}",
                    p.uetr(), p.debtorIBAN(), p.creditorIBAN(), p.amount(), p.creditorReference());
            sse.notifySettlement(p.creditorReference(), settled);
            return new PaymentResult(p.uetr(), TransactionStatus.ACSC, null);
        }

        // --- Inter-bank: route through FIPS — FIPS forwards to destination bank
        log.info("[SETTLE] INTER-BANK | uetr={} | submitting pacs.008 to FIPS", p.uetr());
        try {
            // Use creditorReference as endToEndId so bank-a can fire the SSE event on the correct key
            String endToEndId = p.creditorReference() != null ? p.creditorReference() : p.uetr().toString();
            fips.submit(p.uetr(), p.debtorIBAN(), p.creditorIBAN(), p.amount(),
                    p.debtorName(), p.creditorName(), endToEndId, p.remittanceInfo());
        } catch (FipsRejectedException e) {
            log.warn("[SETTLE] FIPS RJCT {} | uetr={} | rolling back bank +€{}", e.getRejectCode(), p.uetr(), p.amount());
            debtor.credit(p.amount());

            Transaction rejected = Transaction.rejected(p.uetr(), p.debtorIBAN(), p.creditorIBAN(),
                    p.debtorName(), p.creditorName(), p.amount(),
                    p.creditorReference(), p.remittanceInfo());
            debtor.addTransaction(rejected);
            sse.notifyRejection(p.creditorReference(), rejected);
            return new PaymentResult(p.uetr(), TransactionStatus.RJCT, e.getRejectCode());
        }

        // FIPS confirmed ACSC — record outgoing transaction on debtor's side
        Transaction settled = Transaction.settled(p.uetr(), p.debtorIBAN(), p.creditorIBAN(),
                p.debtorName(), p.creditorName(), p.amount(),
                p.creditorReference(), p.remittanceInfo());
        debtor.addTransaction(settled);

        log.info("[SETTLE] ACSC (inter) | uetr={} | {}→{} €{} | ref={}",
                p.uetr(), p.debtorIBAN(), p.creditorIBAN(), p.amount(), p.creditorReference());
        sse.notifySettlement(p.creditorReference(), settled);

        return new PaymentResult(p.uetr(), TransactionStatus.ACSC, null);
    }

    private PaymentResult confirmRtp(UUID rtpId) {
        RequestToPay rtp = rtpStore.get(rtpId);
        if (rtp == null) throw new NotFoundException("RTP not found: " + rtpId);
        if (rtp.getStatus() != RtpStatus.PENDING) {
            throw new ValidationException("RTP is not in PENDING state: " + rtp.getStatus());
        }
        rtp.setStatus(RtpStatus.ACCEPTED);
        log.info("[RTP] ACCEPTED | rtpId={} | {}→{} €{}",
                rtpId, rtp.getDebtorIBAN(), rtp.getCreditorIBAN(), rtp.getAmount());

        PendingPayment p = new PendingPayment(UUID.randomUUID(),
                rtp.getDebtorIBAN(), rtp.getCreditorIBAN(),
                accounts.findByIban(rtp.getDebtorIBAN()).map(Account::getHolderName).orElse("Unknown"),
                rtp.getCreditorName(), rtp.getAmount(), null, rtp.getRemittanceInfo());

        PaymentResult result = settle(p);
        rtp.setStatus(result.status() == TransactionStatus.ACSC ? RtpStatus.SETTLED : RtpStatus.REJECTED);
        rtp.setPaymentUetr(p.uetr());
        log.info("[RTP] {} | rtpId={} uetr={}", rtp.getStatus(), rtpId, p.uetr());
        return result;
    }

    private VopResult match(String actual, String provided) {
        if (actual == null || provided == null) return VopResult.NO_MATCH;
        String a = actual.trim().toLowerCase();
        String b = provided.trim().toLowerCase();
        if (a.equals(b)) return VopResult.MATCH;
        if (a.contains(b) || b.contains(a)) return VopResult.CLOSE_MATCH;
        return VopResult.NO_MATCH;
    }

    private AccountView toView(Account acc) {
        UUID walletId = wallets.findByOwnerIban(acc.getIban())
                .map(DigitalEuroWallet::getWalletId)
                .orElse(null);
        return new AccountView(acc.getIban(), acc.getHolderName(),
                acc.getBankBalance(), walletId,
                acc.getTransactions().stream()
                        .sorted(Comparator.comparing(Transaction::createdAt).reversed())
                        .limit(20)
                        .toList());
    }

    private WalletView toWalletView(DigitalEuroWallet w) {
        return new WalletView(w.getWalletId(), w.getOwnerIban(), w.getBalance());
    }

    private RtpView toRtpView(RequestToPay rtp) {
        return new RtpView(rtp.getRtpId(), rtp.getCreditorIBAN(), rtp.getCreditorName(),
                rtp.getAmount(), rtp.getRemittanceInfo(), rtp.getStatus(), rtp.getCreatedAt());
    }

    private MxPacs00200110 buildPacs002Acsc(String uetrStr, String endToEndId, Instant settledAt) {
        GroupHeader91 grpHdr = new GroupHeader91()
                .setMsgId(UUID.randomUUID().toString())
                .setCreDtTm(OffsetDateTime.now(ZoneOffset.UTC));
        PaymentTransaction110 txStatus = new PaymentTransaction110()
                .setOrgnlUETR(uetrStr)
                .setOrgnlEndToEndId(endToEndId)
                .setTxSts(TransactionStatus.ACSC.name())
                .setAccptncDtTm(OffsetDateTime.ofInstant(settledAt, ZoneOffset.UTC));
        FIToFIPaymentStatusReportV10 report = new FIToFIPaymentStatusReportV10()
                .setGrpHdr(grpHdr)
                .addTxInfAndSts(txStatus);
        return new MxPacs00200110().setFIToFIPmtStsRpt(report);
    }

    // -------------------------------------------------------------------------
    // Incoming payment from FIPS (inter-bank credit leg)
    // -------------------------------------------------------------------------

    /**
     * Called by FIPS to credit the payee on this bank after inter-bank settlement.
     * This is the receiving side of an SCT Inst: no SCA, no debit — only credit.
     *
     * @return pacs.002 ACSC confirming the credit was applied
     */
    public MxPacs00200110 receiveIncoming(MxPacs00800108 pacs008) {
        CreditTransferTransaction39 txInfo = pacs008.getFIToFICstmrCdtTrf().getCdtTrfTxInf().get(0);

        String uetrStr      = txInfo.getPmtId().getUETR();
        String endToEndId   = txInfo.getPmtId().getEndToEndId();
        String creditorIBAN = txInfo.getCdtrAcct().getId().getIBAN();
        String debtorIBAN   = txInfo.getDbtrAcct().getId().getIBAN();
        String debtorName   = txInfo.getDbtr() != null ? txInfo.getDbtr().getNm() : "Unknown";
        String creditorName = txInfo.getCdtr() != null ? txInfo.getCdtr().getNm() : "Unknown";
        BigDecimal amount   = txInfo.getIntrBkSttlmAmt().getValue();
        UUID uetr           = UUID.fromString(uetrStr);

        Account creditor = accounts.findByIban(creditorIBAN)
                .orElseThrow(() -> new NotFoundException("Creditor not found on this bank: " + creditorIBAN));

        creditor.credit(amount);
        Instant now = Instant.now();

        // endToEndId carries the creditorReference set by the sending bank (see settle())
        String creditorReference = endToEndId;

        Transaction settled = Transaction.settled(uetr, debtorIBAN, creditorIBAN,
                debtorName, creditorName, amount, creditorReference, null);
        creditor.addTransaction(settled);

        log.info("[RECEIVE] CREDIT | uetr={} | {}→{} €{} | ref={}", uetr, debtorIBAN, creditorIBAN, amount, creditorReference);

        sse.notifySettlement(creditorReference, settled);

        return buildPacs002Acsc(uetrStr, endToEndId, now);
    }

    // Pending payment context while awaiting SCA
    record PendingPayment(UUID uetr, String debtorIBAN, String creditorIBAN,
                          String debtorName, String creditorName,
                          BigDecimal amount, String creditorReference, String remittanceInfo) {}
}
