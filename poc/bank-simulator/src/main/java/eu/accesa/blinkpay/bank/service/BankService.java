package eu.accesa.blinkpay.bank.service;

import eu.accesa.blinkpay.bank.dto.AccountView;
import eu.accesa.blinkpay.bank.dto.PaymentInitiatedResponse;
import eu.accesa.blinkpay.bank.dto.PaymentRequest;
import eu.accesa.blinkpay.bank.dto.PaymentResult;
import eu.accesa.blinkpay.bank.dto.ProxyLookupResponse;
import eu.accesa.blinkpay.bank.dto.RtpRequest;
import eu.accesa.blinkpay.bank.dto.RtpView;
import eu.accesa.blinkpay.bank.dto.ScaRequest;
import eu.accesa.blinkpay.bank.dto.VopResponse;
import eu.accesa.blinkpay.bank.fips.FipsClient;
import eu.accesa.blinkpay.bank.fips.FipsRejectedException;
import eu.accesa.blinkpay.bank.model.Account;
import eu.accesa.blinkpay.bank.model.InsufficientFundsException;
import eu.accesa.blinkpay.bank.model.RequestToPay;
import eu.accesa.blinkpay.bank.model.RtpStatus;
import eu.accesa.blinkpay.bank.model.Transaction;
import eu.accesa.blinkpay.bank.model.TransactionStatus;
import eu.accesa.blinkpay.bank.model.VopResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core bank simulator business logic.
 *
 * Orchestrates: proxy lookup → VoP → SCA challenge issuance → balance check →
 * waterfall → FIPS submission → balance mutation → transaction recording.
 */
@Service
public class BankService {

    private static final String CORRECT_PIN = "1234";

    private final AccountStore accounts;
    private final FipsClient fips;

    // Pending payments awaiting SCA confirmation: challengeToken → PaymentRequest context
    private final Map<String, PendingPayment> pendingPayments = new ConcurrentHashMap<>();

    // RTP store: rtpId → RequestToPay
    private final Map<UUID, RequestToPay> rtpStore = new ConcurrentHashMap<>();

    public BankService(AccountStore accounts, FipsClient fips) {
        this.accounts = accounts;
        this.fips = fips;
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
                req.amount(), req.remittanceInfo()));

        return new PaymentInitiatedResponse(uetr, uetr.toString(), creditorName, creditorIBAN, req.amount());
    }

    // -------------------------------------------------------------------------
    // Payment flow — step 2: SCA confirmation → settlement
    // -------------------------------------------------------------------------

    public PaymentResult confirmPayment(ScaRequest sca) {
        if (!CORRECT_PIN.equals(sca.pin())) {
            throw new ValidationException("Invalid PIN");
        }

        // RTP path (Flow B2)
        if (sca.rtpId() != null) {
            return confirmRtp(sca.rtpId());
        }

        // Direct payment path (Flow A, B1)
        if (sca.uetr() == null) throw new ValidationException("uetr is required for direct payment SCA");
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
        Account debtor   = accounts.findByIban(p.debtorIBAN()).orElseThrow();
        Account creditor = accounts.findByIban(p.creditorIBAN()).orElseThrow();

        // Balance check before hitting FIPS
        try {
            debtor.debit(p.amount());
        } catch (InsufficientFundsException e) {
            Transaction rejected = Transaction.rejected(p.uetr(), p.debtorIBAN(), p.creditorIBAN(),
                    p.debtorName(), p.creditorName(), p.amount(), p.remittanceInfo());
            debtor.addTransaction(rejected);
            return new PaymentResult(p.uetr(), TransactionStatus.RJCT, "AM04");
        }

        try {
            fips.submit(p.uetr(), p.debtorIBAN(), p.creditorIBAN(), p.amount(),
                    p.debtorName(), p.creditorName(), p.uetr().toString(), p.remittanceInfo());
        } catch (FipsRejectedException e) {
            // Roll back debit — FIPS refused (e.g. duplicate UETR)
            debtor.credit(p.amount());
            Transaction rejected = Transaction.rejected(p.uetr(), p.debtorIBAN(), p.creditorIBAN(),
                    p.debtorName(), p.creditorName(), p.amount(), p.remittanceInfo());
            debtor.addTransaction(rejected);
            creditor.addTransaction(rejected);
            return new PaymentResult(p.uetr(), TransactionStatus.RJCT, e.getRejectCode());
        }

        creditor.credit(p.amount());
        Transaction settled = Transaction.settled(p.uetr(), p.debtorIBAN(), p.creditorIBAN(),
                p.debtorName(), p.creditorName(), p.amount(), p.remittanceInfo());
        debtor.addTransaction(settled);
        creditor.addTransaction(settled);
        return new PaymentResult(p.uetr(), TransactionStatus.ACSC, null);
    }

    private PaymentResult confirmRtp(UUID rtpId) {
        RequestToPay rtp = rtpStore.get(rtpId);
        if (rtp == null) throw new NotFoundException("RTP not found: " + rtpId);
        if (rtp.getStatus() != RtpStatus.PENDING) {
            throw new ValidationException("RTP is not in PENDING state: " + rtp.getStatus());
        }
        rtp.setStatus(RtpStatus.ACCEPTED);

        PendingPayment p = new PendingPayment(UUID.randomUUID(),
                rtp.getDebtorIBAN(), rtp.getCreditorIBAN(),
                accounts.findByIban(rtp.getDebtorIBAN()).map(Account::getHolderName).orElse("Unknown"),
                rtp.getCreditorName(), rtp.getAmount(), rtp.getRemittanceInfo());

        PaymentResult result = settle(p);
        rtp.setStatus(result.status() == TransactionStatus.ACSC ? RtpStatus.SETTLED : RtpStatus.REJECTED);
        rtp.setPaymentUetr(p.uetr());
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
        return new AccountView(acc.getIban(), acc.getHolderName(),
                acc.getBankBalance(), acc.getDigitalEuroBalance(),
                acc.getTransactions().stream()
                        .sorted(Comparator.comparing(Transaction::createdAt).reversed())
                        .limit(20)
                        .toList());
    }

    private RtpView toRtpView(RequestToPay rtp) {
        return new RtpView(rtp.getRtpId(), rtp.getCreditorIBAN(), rtp.getCreditorName(),
                rtp.getAmount(), rtp.getRemittanceInfo(), rtp.getStatus(), rtp.getCreatedAt());
    }

    // Pending payment context while awaiting SCA
    record PendingPayment(UUID uetr, String debtorIBAN, String creditorIBAN,
                          String debtorName, String creditorName,
                          java.math.BigDecimal amount, String remittanceInfo) {}
}
