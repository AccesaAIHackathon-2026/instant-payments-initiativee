package eu.accesa.blinkpay.fips.service;

import com.prowidesoftware.swift.model.mx.MxPacs00200110;
import com.prowidesoftware.swift.model.mx.MxPacs00800108;
import eu.accesa.blinkpay.fips.config.BankRoutingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Forwards a pacs.008 to the destination bank's {@code POST /bank/receive} endpoint
 * based on the creditor IBAN prefix, and returns the pacs.002 response.
 *
 * The routing table is populated from {@link BankRoutingProperties} at startup.
 */
@Component
public class BankForwardingClient {

    private static final Logger log = LoggerFactory.getLogger(BankForwardingClient.class);

    /** prefix → RestClient pre-configured with the bank's base URL */
    private final Map<String, RestClient> routingTable;

    public BankForwardingClient(BankRoutingProperties props) {
        this.routingTable = props.getBanks().stream()
                .collect(Collectors.toMap(
                        BankRoutingProperties.BankEntry::getPrefix,
                        e -> RestClient.builder().baseUrl(e.getUrl()).build()
                ));
        log.info("[FIPS] Routing table: {}", props.getBanks().stream()
                .map(e -> e.getPrefix() + "→" + e.getUrl())
                .collect(Collectors.joining(", ")));
    }

    /**
     * Finds the destination bank by IBAN prefix and forwards the pacs.008.
     *
     * @throws IllegalArgumentException if no bank is registered for the creditor IBAN
     */
    public MxPacs00200110 forward(String creditorIBAN, MxPacs00800108 pacs008) {
        RestClient bankClient = routingTable.entrySet().stream()
                .filter(e -> creditorIBAN.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No bank registered for IBAN: " + creditorIBAN));

        log.info("[FIPS] Forwarding pacs.008 to bank for IBAN prefix of {}", creditorIBAN);

        return bankClient.post()
                .uri("/bank/receive")
                .contentType(MediaType.APPLICATION_JSON)
                .body(pacs008)
                .retrieve()
                .body(MxPacs00200110.class);
    }
}
