package eu.accesa.blinkpay.fips.controller;

import com.prowidesoftware.swift.model.mx.MxPacs00200110;
import com.prowidesoftware.swift.model.mx.MxPacs00800108;
import eu.accesa.blinkpay.fips.model.TransactionView;
import eu.accesa.blinkpay.fips.service.FipsService;
import eu.accesa.blinkpay.fips.service.FlowEventService;
import org.springframework.http.HttpStatus;
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

/**
 * FIPS simulator REST endpoints — called only by the bank simulator.
 *
 * Request/response bodies use ISO 20022 pacs.008.001.08 and pacs.002.001.10
 * message structures derived from official EPC XSD schemas (via prowide-iso20022).
 *
 * POST /fips/submit             — Submit pacs.008, receive synchronous pacs.002
 * GET  /fips/status/{uetr}      — Poll pacs.002 status for a previously submitted UETR
 * GET  /fips/transactions        — Admin view, newest-first, max 500 per call
 */
@RestController
@RequestMapping("/fips")
public class FipsController {

    private final FipsService fipsService;
    private final FlowEventService flowEvents;

    public FipsController(FipsService fipsService, FlowEventService flowEvents) {
        this.fipsService = fipsService;
        this.flowEvents = flowEvents;
    }

    /** SSE stream for the flow visualizer — broadcasts all FIPS flow events. */
    @GetMapping(value = "/flow-events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter flowEvents() {
        return flowEvents.subscribe();
    }

    /**
     * Submit a pacs.008.001.08 payment instruction and receive a pacs.002.001.10 status report.
     *
     * HTTP 200 — ACSC (settled)
     * HTTP 422 — RJCT (validation failure; reason code in pacs.002 StsRsnInf/Rsn/Cd)
     */
    @PostMapping("/submit")
    public ResponseEntity<MxPacs00200110> submit(@RequestBody MxPacs00800108 request) {
        MxPacs00200110 response = fipsService.submit(request);
        String txSts = response.getFIToFIPmtStsRpt().getTxInfAndSts().get(0).getTxSts();
        HttpStatus status = "RJCT".equals(txSts) ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Poll the pacs.002 status for a previously submitted transaction by UETR.
     *
     * HTTP 200 — found; HTTP 404 — UETR unknown.
     */
    @GetMapping("/status/{uetr}")
    public ResponseEntity<MxPacs00200110> getStatus(@PathVariable UUID uetr) {
        return fipsService.getStatus(uetr)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Admin view of recent transactions, sorted newest-first.
     *
     * @param limit Number of records to return (1–500, default 500).
     */
    @GetMapping("/transactions")
    public List<TransactionView> getAllTransactions(
            @RequestParam(defaultValue = "500") int limit) {
        return fipsService.getAllTransactions(limit);
    }
}
