package eu.accesa.blinkpay.bank.controller;

import eu.accesa.blinkpay.bank.dto.AccountView;
import eu.accesa.blinkpay.bank.dto.PaymentInitiatedResponse;
import eu.accesa.blinkpay.bank.dto.PaymentRequest;
import eu.accesa.blinkpay.bank.dto.PaymentResult;
import eu.accesa.blinkpay.bank.dto.ProxyLookupResponse;
import eu.accesa.blinkpay.bank.dto.RtpRequest;
import eu.accesa.blinkpay.bank.dto.RtpView;
import eu.accesa.blinkpay.bank.dto.ScaRequest;
import eu.accesa.blinkpay.bank.dto.VopResponse;
import eu.accesa.blinkpay.bank.model.Transaction;
import eu.accesa.blinkpay.bank.service.BankService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bank")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
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

    /** Account details + balances */
    @GetMapping("/accounts/{iban}")
    public AccountView account(@PathVariable String iban) {
        return bankService.getAccount(iban);
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
