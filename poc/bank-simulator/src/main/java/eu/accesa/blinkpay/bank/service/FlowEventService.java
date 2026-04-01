package eu.accesa.blinkpay.bank.service;

import eu.accesa.blinkpay.bank.dto.FlowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Broadcasts flow events to all connected visualizer clients via SSE.
 * Unlike {@link SseNotificationService} (keyed by creditor reference),
 * this service is a simple broadcast — every subscriber receives every event.
 */
@Service
public class FlowEventService {

    private static final Logger log = LoggerFactory.getLogger(FlowEventService.class);
    private static final long EMITTER_TIMEOUT_MS = 10 * 60 * 1_000L;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Value("${bank.id:bank}")
    private String bankId;

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void emit(String step, String source, String target,
                     String sourceType, String targetType,
                     UUID uetr, String actor,
                     String debtorName, String debtorIban,
                     String creditorName, String creditorIban,
                     BigDecimal amount, String status, String detail) {
        FlowEvent event = new FlowEvent(
                "evt-" + UUID.randomUUID(),
                Instant.now().toString(),
                uetr != null ? uetr.toString() : null,
                step, source, target, sourceType, targetType,
                actor, debtorName, debtorIban, creditorName, creditorIban,
                amount, "EUR", status, detail
        );

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("flow").data(event));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }

    public String getBankId() {
        return bankId;
    }
}
