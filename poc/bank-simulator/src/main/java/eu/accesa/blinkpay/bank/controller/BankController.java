package eu.accesa.blinkpay.bank.controller;

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
import com.prowidesoftware.swift.model.mx.MxPacs00200110;
import com.prowidesoftware.swift.model.mx.MxPacs00800108;
import eu.accesa.blinkpay.bank.dto.VopResponse;
import eu.accesa.blinkpay.bank.dto.WalletTransferRequest;
import eu.accesa.blinkpay.bank.dto.WalletTransferResponse;
import eu.accesa.blinkpay.bank.dto.WalletView;
import eu.accesa.blinkpay.bank.model.Transaction;
import eu.accesa.blinkpay.bank.service.BankService;
import eu.accesa.blinkpay.bank.service.FlowEventService;
import eu.accesa.blinkpay.bank.service.SseNotificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bank")
public class BankController {

    private final BankService bankService;
    private final SseNotificationService sse;
    private final FlowEventService flowEvents;

    public BankController(BankService bankService, SseNotificationService sse, FlowEventService flowEvents) {
        this.bankService = bankService;
        this.sse = sse;
        this.flowEvents = flowEvents;
    }

    /** Register a new consumer or merchant account. */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(bankService.register(request));
    }

    /**
     * SSE stream — retailer subscribes when the QR code is displayed.
     * Receives a "settlement" event the moment Alice's payment clears.
     * Keyed by the ISO 11649 creditor reference from QR line 9, scoping each
     * stream to a single payment session.
     * React usage: new EventSource('/bank/payment-events/{creditorReference}')
     */
    @GetMapping(value = "/payment-events/{creditorReference}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter paymentEvents(@PathVariable String creditorReference) {
        return sse.subscribe(creditorReference);
    }

    /** SSE stream for the flow visualizer — broadcasts all payment flow events. */
    @GetMapping(value = "/flow-events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter flowEvents() {
        return flowEvents.subscribe();
    }

    /** Proxy lookup: phone/email → IBAN + name */
    @GetMapping("/proxy")
    public ProxyLookupResponse proxy(@RequestParam String alias) {
        return bankService.proxyLookup(alias);
    }

    /** Verification of Payee */
    @GetMapping("/vop")
    public VopResponse vop(@RequestParam String iban, @RequestParam String name) {
        return bankService.verifyPayee(iban, name);
    }

    /** Account details + bank balance (walletId links to the DE custody wallet) */
    @GetMapping("/accounts/{iban}")
    public AccountView account(@PathVariable String iban) {
        return bankService.getAccount(iban);
    }

    /** Digital Euro custody wallet — balance and owner */
    @GetMapping("/wallet/{walletId}")
    public WalletView wallet(@PathVariable UUID walletId) {
        return bankService.getWallet(walletId);
    }

    /**
     * Top-up: move funds from the bank account into the DE wallet.
     * Used to pre-load the wallet before an offline NFC payment.
     */
    @PostMapping("/wallet/{walletId}/topup")
    public WalletTransferResponse topUp(@PathVariable UUID walletId,
                                        @RequestBody WalletTransferRequest request) {
        return bankService.topUpWallet(walletId, request);
    }

    /**
     * Redeem: move funds from the DE wallet back into the bank account.
     * Used to convert unused DE balance back to commercial bank money.
     */
    @PostMapping("/wallet/{walletId}/redeem")
    public WalletTransferResponse redeem(@PathVariable UUID walletId,
                                         @RequestBody WalletTransferRequest request) {
        return bankService.redeemWallet(walletId, request);
    }

    /** Transaction history for settlement polling (retailer polls this in Flow B1) */
    @GetMapping("/transactions/{iban}")
    public List<Transaction> transactions(@PathVariable String iban) {
        return bankService.getTransactions(iban);
    }

    /** Step 1 — initiate payment, receive SCA challenge */
    @PostMapping("/pay")
    public PaymentInitiatedResponse pay(@RequestBody PaymentRequest request) {
        return bankService.initiatePayment(request);
    }

    /** Step 2 — confirm SCA, trigger FIPS settlement */
    @PostMapping("/sca")
    public PaymentResult sca(@RequestBody ScaRequest request) {
        return bankService.confirmPayment(request);
    }

    /**
     * Inter-bank credit leg — called by FIPS to credit the payee on this bank.
     * Not authenticated by API key (internal network call from FIPS).
     * Returns pacs.002 ACSC confirming the credit was applied.
     */
    @PostMapping("/receive")
    public MxPacs00200110 receive(@RequestBody MxPacs00800108 pacs008) {
        return bankService.receiveIncoming(pacs008);
    }

    // --- Stretch: Request-to-Pay (Flow B2) -----------------------------------

    /** Retailer sends a Request-to-Pay to the payer by alias */
    @PostMapping("/request-to-pay")
    public ResponseEntity<RtpView> requestToPay(@RequestBody RtpRequest request) {
        return ResponseEntity.status(201).body(bankService.createRtp(request));
    }

    /** Payer app polls for incoming RTPs (every 2s) */
    @GetMapping("/incoming-rtp/{iban}")
    public List<RtpView> incomingRtp(@PathVariable String iban) {
        return bankService.getIncomingRtps(iban);
    }

    /** Retailer polls for RTP settlement status */
    @GetMapping("/rtp-status/{rtpId}")
    public RtpView rtpStatus(@PathVariable UUID rtpId) {
        return bankService.getRtpStatus(rtpId);
    }
}
