package eu.accesa.blinkpay.fips.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Routing table that maps IBAN prefixes to bank URLs.
 * FIPS uses this to forward inter-bank pacs.008 messages to the receiving bank.
 *
 * Configuration (application.properties or env vars):
 *
 *   fips.banks[0].prefix=DE89370400440532013
 *   fips.banks[0].url=http://bank-a:8080
 *   fips.banks[1].prefix=DE89370400440532014
 *   fips.banks[1].url=http://bank-b:8082
 *
 * Env var equivalents (Spring Boot relaxed binding):
 *   FIPS_BANKS_0_PREFIX=DE89370400440532013
 *   FIPS_BANKS_0_URL=http://bank-a:8080
 */
@ConfigurationProperties(prefix = "fips")
public class BankRoutingProperties {

    private List<BankEntry> banks = new ArrayList<>();

    public List<BankEntry> getBanks() { return banks; }
    public void setBanks(List<BankEntry> banks) { this.banks = banks; }

    public static class BankEntry {
        private String prefix;
        private String url;

        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
