package eu.accesa.blinkpay.bank.fips;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Thin HTTP client that talks to the FIPS simulator.
 *
 * Uses simplified JSON payloads matching the FIPS simulator's expected structure.
 * In production this would marshal ISO 20022 XML pacs.008 via prowide.
 */
@Component
public class FipsClient {

    private final RestClient restClient;

    public FipsClient(RestClient fipsRestClient) {
        this.restClient = fipsRestClient;
    }

    /**
     * Submits a pacs.008 payment to FIPS and returns the pacs.002 status.
     *
     * @throws FipsRejectedException if FIPS returns RJCT with a reason code
     */
    public FipsSubmitResponse submit(UUID uetr, String debtorIBAN, String creditorIBAN,
                                     BigDecimal amount, String debtorName, String creditorName,
                                     String endToEndId, String remittanceInfo) {
        var request = new FipsSubmitRequest(uetr, debtorIBAN, creditorIBAN, amount, "EUR",
                debtorName, creditorName, endToEndId, remittanceInfo);

        FipsSubmitResponse response = restClient.post()
                .uri("/fips/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(status -> status.value() == 422,
                        (req, res) -> { /* handled below — body still parsed */ })
                .body(FipsSubmitResponse.class);

        if (response != null && "RJCT".equals(response.status())) {
            throw new FipsRejectedException(uetr, response.statusReason());
        }
        return response;
    }
}
