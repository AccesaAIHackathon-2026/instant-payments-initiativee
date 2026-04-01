package eu.accesa.blinkpay.bank.dto;

import java.math.BigDecimal;

public record FlowEvent(
        String id,
        String timestamp,
        String uetr,
        String step,
        String source,
        String target,
        String sourceType,
        String targetType,
        String actor,
        String debtorName,
        String debtorIban,
        String creditorName,
        String creditorIban,
        BigDecimal amount,
        String currency,
        String status,
        String detail
) {}
