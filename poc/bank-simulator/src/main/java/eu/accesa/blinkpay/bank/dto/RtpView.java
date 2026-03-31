package eu.accesa.blinkpay.bank.dto;

import eu.accesa.blinkpay.bank.model.RtpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Used by GET /bank/incoming-rtp/{iban} and GET /bank/rtp-status/{rtpId}. */
public record RtpView(
        UUID rtpId,
        String creditorIBAN,
        String creditorName,
        BigDecimal amount,
        String remittanceInfo,
        RtpStatus status,
        Instant createdAt
) {}
