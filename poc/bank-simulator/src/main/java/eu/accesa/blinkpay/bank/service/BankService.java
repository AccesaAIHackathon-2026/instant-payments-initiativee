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
import eu.accesa.blinkpay.bank.dto.WalletView;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    private static final String CORRECT_PIN = "1234";

    private final AccountStore accounts;
    private final WalletStore wallets;
    private final FipsClient fips;
    private final SseNotificationService sse;

    // Pending payments awaiting SCA confirmation: uetr → PendingPayment context
    private final Map<String, PendingPayment> pendingPayments = new ConcurrentHashMap<>();

    // RTP store: rtpId → RequestToPay
    private final Map<UUID, RequestToPay> rtpStore = new ConcurrentHashMap<>();

    public BankService(AccountStore accounts, WalletStore wallets,
                       FipsClient fips, SseNotificationService sse) {
        this.accounts = accounts;
        this.wallets  = wallets;
        this.fips     = fips;
        this.sse      = sse;
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

        // --- Balance check (wallet + bank combined) ---------------------------
        Optional<DigitalEuroWallet> wallet = wallets.findByOwnerIban(p.debtorIBAN());
        BigDecimal deBalance   = wallet.map(DigitalEuroWallet::getBalance).orElse(BigDecimal.ZERO);
        BigDecimal totalFunds  = debtor.getBankBalance().add(deBalance);

        if (totalFunds.compareTo(p.amount()) < 0) {
            Transaction rejected = Transaction.rejected(p.uetr(), p.debtorIBAN(), p.creditorIBAN(),
                    p.debtorName(), p.creditorName(), p.amount(), p.remittanceInfo());
            debtor.addTransaction(rejected);
            return new PaymentResult(p.uetr(), TransactionStatus.RJCT, "AM04");
        }

        // --- Waterfall: DE wallet first, bank account for any shortfall -------
        BigDecimal bankDebited   = BigDecimal.ZERO;
        BigDecimal walletDebited = BigDecimal.ZERO;

        BigDecimal remaining = p.amount();
        if (wallet.isPresent() && deBalance.compareTo(BigDecimal.ZERO) > 0) {
            remaining    = wallet.get().debit(remaining);
            walletDebited = p.amount().subtract(remaining);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            debtor.debit(remaining);
            bankDebited = remaining;
        }

        // --- FIPS submission --------------------------------------------------
        final BigDecimal finalWalletDebited = walletDebited;
        final BigDecimal finalBankDebited   = bankDebited;
        try {
            fips.submit(p.uetr(), p.debtorIBAN(), p.creditorIBAN(), p.amount(),
                    p.debtorName(), p.creditorName(), "E2E-" + p.uetr(), p.remittanceInfo());
        } catch (FipsRejectedException e) {
            // Roll back exactly what was debited
            if (finalBankDebited.compareTo(BigDecimal.ZERO) > 0) {
                debtor.credit(finalBankDebited);
            }
            if (wallet.isPresent() && finalWalletDebited.compareTo(BigDecimal.ZERO) > 0) {
                wallet.get().credit(finalWalletDebited);
            }

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

        sse.notifySettlement(p.creditorIBAN(), settled);

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

    // Pending payment context while awaiting SCA
    record PendingPayment(UUID uetr, String debtorIBAN, String creditorIBAN,
                          String debtorName, String creditorName,
                          BigDecimal amount, String remittanceInfo) {}
}
