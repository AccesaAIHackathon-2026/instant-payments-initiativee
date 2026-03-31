package eu.accesa.blinkpay.bank.dto;

import java.util.UUID;

/**
 * SCA confirmation from the consumer app (POST /bank/sca).
 * For RTP flow, rtpId is set instead of uetr.
 */
public record ScaRequest(
        UUID uetr,        // set for direct payments (Flow A, B1)
        UUID rtpId,       // set for RTP approval (Flow B2)
        String pin        // stub: any value of "1234" is accepted
) {}
