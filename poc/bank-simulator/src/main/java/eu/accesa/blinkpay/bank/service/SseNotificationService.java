package eu.accesa.blinkpay.bank.service;

import eu.accesa.blinkpay.bank.model.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages Server-Sent Event subscriptions for real-time settlement notifications.
 *
 * The retailer subscribes when it displays a QR code. When the bank settles a
 * credit to that IBAN, all active subscribers receive a "settlement" event
 * containing the transaction details — eliminating the need for polling.
 *
 * Multiple subscribers per IBAN are supported (e.g. multiple POS browser tabs).
 */
@Service
public class SseNotificationService {

    /** 5-minute timeout — enough for any demo transaction to complete. */
    private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1_000L;

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> registry = new ConcurrentHashMap<>();

    /**
     * Creates and registers a new SSE emitter for the given IBAN.
     * The caller (controller) returns this emitter directly as the response body.
     */
    public SseEmitter subscribe(String iban) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        registry.computeIfAbsent(iban, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(iban, emitter));
        emitter.onTimeout(()    -> remove(iban, emitter));
        emitter.onError(e       -> remove(iban, emitter));

        return emitter;
    }

    /**
     * Pushes a "settlement" event to all subscribers watching the given creditor IBAN.
     * Called by BankService after a successful FIPS settlement.
     */
    public void notifySettlement(String creditorIBAN, Transaction tx) {
        List<SseEmitter> emitters = registry.getOrDefault(creditorIBAN, new CopyOnWriteArrayList<>());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("settlement")
                        .data(tx));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }

    private void remove(String iban, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = registry.get(iban);
        if (list != null) list.remove(emitter);
    }
}
