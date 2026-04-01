package eu.accesa.blinkpay.fips.controller;

import com.prowidesoftware.swift.model.mx.MxPacs00200110;
import eu.accesa.blinkpay.fips.model.TransactionStatus;
import eu.accesa.blinkpay.fips.model.TransactionView;
import eu.accesa.blinkpay.fips.util.Pacs008Factory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import eu.accesa.blinkpay.fips.service.BankForwardingClient;
import eu.accesa.blinkpay.fips.config.BankRoutingProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for the FIPS simulator REST layer.
 *
 * Each test uses a fresh UUID so they can share the same Spring context
 * without stepping on each other's in-memory state.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {eu.accesa.blinkpay.fips.FipsSimulatorApplication.class, FipsControllerIT.StubBankForwardingConfig.class})
class FipsControllerIT {

    @Autowired
    private TestRestTemplate rest;

    /**
     * Replaces {@link BankForwardingClient} with a no-op stub so tests don't
     * need the bank-simulator running on localhost:8080.
     */
    @TestConfiguration
    static class StubBankForwardingConfig {
        @Bean
        @Primary
        BankForwardingClient stubBankForwardingClient(BankRoutingProperties props) {
            return new BankForwardingClient(props) {
                @Override
                public com.prowidesoftware.swift.model.mx.MxPacs00200110 forward(
                        String creditorIBAN,
                        com.prowidesoftware.swift.model.mx.MxPacs00800108 pacs008) {
                    // No-op: pretend the bank accepted the payment
                    return null;
                }
            };
        }
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Happy path — settlement")
    class HappyPath {

        @Test
        @DisplayName("P2P payment Alice → Bob settles with ACSC")
        void p2pPayment_settles() {
            UUID uetr = UUID.randomUUID();

            ResponseEntity<MxPacs00200110> response = rest.postForEntity(
                    "/fips/submit", Pacs008Factory.aliceToBob(uetr, new BigDecimal("10.00")),
                    MxPacs00200110.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertTxStatus(response.getBody(), uetr, "ACSC");
            assertThat(response.getBody().getFIToFIPmtStsRpt()
                    .getTxInfAndSts().get(0).getAccptncDtTm()).isNotNull();
        }

        @Test
        @DisplayName("QR merchant payment Alice → Retail Store settles with ACSC")
        void merchantQrPayment_settles() {
            UUID uetr = UUID.randomUUID();

            ResponseEntity<MxPacs00200110> response = rest.postForEntity(
                    "/fips/submit", Pacs008Factory.aliceToRetail(uetr, new BigDecimal("25.00")),
                    MxPacs00200110.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertTxStatus(response.getBody(), uetr, "ACSC");
        }

        @Test
        @DisplayName("Payment at SCT Inst maximum amount (EUR 100,000) settles")
        void maxAmount_settles() {
            UUID uetr = UUID.randomUUID();

            ResponseEntity<MxPacs00200110> response = rest.postForEntity(
                    "/fips/submit", Pacs008Factory.withAmount(uetr, new BigDecimal("100000.00")),
                    MxPacs00200110.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertTxStatus(response.getBody(), uetr, "ACSC");
        }
    }

    // -------------------------------------------------------------------------
    // Rejection scenarios
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Rejection — validation failures")
    class RejectionScenarios {

        @Test
        @DisplayName("Duplicate UETR → RJCT AM05 (DuplicatePayment)")
        void duplicateUetr_rejected_AM05() {
            UUID uetr = UUID.randomUUID();
            rest.postForEntity("/fips/submit", Pacs008Factory.aliceToBob(uetr, new BigDecimal("5.00")),
                    MxPacs00200110.class);

            ResponseEntity<MxPacs00200110> second = rest.postForEntity(
                    "/fips/submit", Pacs008Factory.aliceToBob(uetr, new BigDecimal("5.00")),
                    MxPacs00200110.class);

            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertRejected(second.getBody(), uetr, "AM05");
        }

        @Test
        @DisplayName("Zero amount → RJCT AM01")
        void zeroAmount_rejected_AM01() {
            UUID uetr = UUID.randomUUID();

            ResponseEntity<MxPacs00200110> response = rest.postForEntity(
                    "/fips/submit", Pacs008Factory.withAmount(uetr, BigDecimal.ZERO),
                    MxPacs00200110.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertRejected(response.getBody(), uetr, "AM01");
        }

        @Test
        @DisplayName("Negative amount → RJCT AM01")
        void negativeAmount_rejected_AM01() {
            UUID uetr = UUID.randomUUID();

            ResponseEntity<MxPacs00200110> response = rest.postForEntity(
                    "/fips/submit", Pacs008Factory.withAmount(uetr, new BigDecimal("-1.00")),
                    MxPacs00200110.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertRejected(response.getBody(), uetr, "AM01");
        }

        @Test
        @DisplayName("Amount exceeds SCT Inst limit (EUR 100,000) → RJCT AM02")
        void amountOverLimit_rejected_AM02() {
            UUID uetr = UUID.randomUUID();

            ResponseEntity<MxPacs00200110> response = rest.postForEntity(
                    "/fips/submit", Pacs008Factory.withAmount(uetr, new BigDecimal("100000.01")),
                    MxPacs00200110.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertRejected(response.getBody(), uetr, "AM02");
        }

        @Test
        @DisplayName("Non-EUR currency → RJCT AM03 (NotAllowedCurrency)")
        void nonEurCurrency_rejected_AM03() {
            UUID uetr = UUID.randomUUID();

            ResponseEntity<MxPacs00200110> response = rest.postForEntity(
                    "/fips/submit", Pacs008Factory.withCurrency(uetr, "USD"),
                    MxPacs00200110.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertRejected(response.getBody(), uetr, "AM03");
        }

        @Test
        @DisplayName("Missing debtor IBAN → RJCT AC01 (IncorrectAccountNumber)")
        void missingDebtorIBAN_rejected_AC01() {
            UUID uetr = UUID.randomUUID();

            ResponseEntity<MxPacs00200110> response = rest.postForEntity(
                    "/fips/submit", Pacs008Factory.withoutDebtorIBAN(uetr),
                    MxPacs00200110.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertRejected(response.getBody(), uetr, "AC01");
        }

        @Test
        @DisplayName("Missing creditor IBAN → RJCT AC03 (InvalidCreditorAccountNumber)")
        void missingCreditorIBAN_rejected_AC03() {
            UUID uetr = UUID.randomUUID();

            ResponseEntity<MxPacs00200110> response = rest.postForEntity(
                    "/fips/submit", Pacs008Factory.withoutCreditorIBAN(uetr),
                    MxPacs00200110.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertRejected(response.getBody(), uetr, "AC03");
        }

        @Test
        @DisplayName("Missing UETR → RJCT AC01")
        void missingUetr_rejected_AC01() {
            ResponseEntity<MxPacs00200110> response = rest.postForEntity(
                    "/fips/submit", Pacs008Factory.withoutUetr(),
                    MxPacs00200110.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            String txSts = response.getBody().getFIToFIPmtStsRpt().getTxInfAndSts().get(0).getTxSts();
            assertThat(txSts).isEqualTo("RJCT");
        }
    }

    // -------------------------------------------------------------------------
    // Status polling
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Status polling — GET /fips/status/{uetr}")
    class StatusPolling {

        @Test
        @DisplayName("Settled UETR returns ACSC status")
        void settledUetr_returnsACSC() {
            UUID uetr = UUID.randomUUID();
            rest.postForEntity("/fips/submit", Pacs008Factory.aliceToBob(uetr, new BigDecimal("15.00")),
                    MxPacs00200110.class);

            ResponseEntity<MxPacs00200110> response = rest.getForEntity(
                    "/fips/status/" + uetr, MxPacs00200110.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertTxStatus(response.getBody(), uetr, "ACSC");
        }

        @Test
        @DisplayName("Rejected UETR returns RJCT status with reason code")
        void rejectedUetr_returnsRJCTWithReason() {
            UUID uetr = UUID.randomUUID();
            rest.postForEntity("/fips/submit", Pacs008Factory.withCurrency(uetr, "GBP"),
                    MxPacs00200110.class);

            ResponseEntity<MxPacs00200110> response = rest.getForEntity(
                    "/fips/status/" + uetr, MxPacs00200110.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertRejected(response.getBody(), uetr, "AM03");
        }

        @Test
        @DisplayName("Unknown UETR returns 404")
        void unknownUetr_returns404() {
            ResponseEntity<Void> response = rest.getForEntity(
                    "/fips/status/" + UUID.randomUUID(), Void.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // -------------------------------------------------------------------------
    // Admin transactions view
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Admin — GET /fips/transactions")
    class AdminTransactions {

        @Test
        @DisplayName("Submitted transactions appear in the list")
        void submittedTransactions_appear() {
            UUID uetr1 = UUID.randomUUID();
            UUID uetr2 = UUID.randomUUID();
            rest.postForEntity("/fips/submit", Pacs008Factory.aliceToBob(uetr1, new BigDecimal("10.00")),
                    MxPacs00200110.class);
            rest.postForEntity("/fips/submit", Pacs008Factory.aliceToRetail(uetr2, new BigDecimal("30.00")),
                    MxPacs00200110.class);

            List<TransactionView> txs = fetchTransactions(500);

            assertThat(txs).extracting(TransactionView::uetr)
                    .contains(uetr1, uetr2);
        }

        @Test
        @DisplayName("Transactions are ordered newest first")
        void transactions_newestFirst() {
            UUID uetrOlder = UUID.randomUUID();
            UUID uetrNewer = UUID.randomUUID();
            rest.postForEntity("/fips/submit", Pacs008Factory.aliceToBob(uetrOlder, new BigDecimal("1.00")),
                    MxPacs00200110.class);
            rest.postForEntity("/fips/submit", Pacs008Factory.aliceToBob(uetrNewer, new BigDecimal("2.00")),
                    MxPacs00200110.class);

            List<TransactionView> txs = fetchTransactions(500);

            int idxOlder = indexOfUetr(txs, uetrOlder);
            int idxNewer = indexOfUetr(txs, uetrNewer);
            assertThat(idxNewer).isLessThan(idxOlder);
        }

        @Test
        @DisplayName("limit parameter caps the result set")
        void limitParam_capsResults() {
            // Submit 3 transactions
            for (int i = 0; i < 3; i++) {
                rest.postForEntity("/fips/submit",
                        Pacs008Factory.aliceToBob(UUID.randomUUID(), new BigDecimal("1.00")),
                        MxPacs00200110.class);
            }

            List<TransactionView> txs = fetchTransactions(2);

            assertThat(txs).hasSizeLessThanOrEqualTo(2);
        }

        @Test
        @DisplayName("limit exceeding 500 is capped at 500")
        void limitAboveMax_cappedAt500() {
            List<TransactionView> txs = fetchTransactions(9999);

            assertThat(txs).hasSizeLessThanOrEqualTo(500);
        }

        @Test
        @DisplayName("Settled transactions have status ACSC, rejected have RJCT")
        void transactionStatuses_reflectOutcome() {
            UUID settledUetr  = UUID.randomUUID();
            UUID rejectedUetr = UUID.randomUUID();
            rest.postForEntity("/fips/submit", Pacs008Factory.aliceToBob(settledUetr, new BigDecimal("5.00")),
                    MxPacs00200110.class);
            rest.postForEntity("/fips/submit", Pacs008Factory.withAmount(rejectedUetr, BigDecimal.ZERO),
                    MxPacs00200110.class);

            List<TransactionView> txs = fetchTransactions(500);

            assertThat(txs).anySatisfy(tx -> {
                assertThat(tx.uetr()).isEqualTo(settledUetr);
                assertThat(tx.status()).isEqualTo(TransactionStatus.ACSC);
            });
            assertThat(txs).anySatisfy(tx -> {
                assertThat(tx.uetr()).isEqualTo(rejectedUetr);
                assertThat(tx.status()).isEqualTo(TransactionStatus.RJCT);
            });
        }
    }

    // -------------------------------------------------------------------------
    // Actuator
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Actuator health endpoint reports UP")
    void actuatorHealth_reportsUp() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    // -------------------------------------------------------------------------
    // Assertion helpers
    // -------------------------------------------------------------------------

    private void assertTxStatus(MxPacs00200110 body, UUID expectedUetr, String expectedStatus) {
        assertThat(body).isNotNull();
        var txInfo = body.getFIToFIPmtStsRpt().getTxInfAndSts().get(0);
        assertThat(txInfo.getOrgnlUETR()).isEqualTo(expectedUetr.toString());
        assertThat(txInfo.getTxSts()).isEqualTo(expectedStatus);
    }

    private void assertRejected(MxPacs00200110 body, UUID expectedUetr, String expectedRejectCode) {
        assertTxStatus(body, expectedUetr, "RJCT");
        var reasonCode = body.getFIToFIPmtStsRpt()
                .getTxInfAndSts().get(0)
                .getStsRsnInf().get(0)
                .getRsn().getCd();
        assertThat(reasonCode).isEqualTo(expectedRejectCode);
    }

    private List<TransactionView> fetchTransactions(int limit) {
        ResponseEntity<List<TransactionView>> response = rest.exchange(
                "/fips/transactions?limit=" + limit,
                HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        return response.getBody();
    }

    private int indexOfUetr(List<TransactionView> txs, UUID uetr) {
        for (int i = 0; i < txs.size(); i++) {
            if (uetr.equals(txs.get(i).uetr())) return i;
        }
        return -1;
    }
}
