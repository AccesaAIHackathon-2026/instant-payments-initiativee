package eu.accesa.blinkpay.bank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FipsClientConfig {

    @Bean
    public RestClient fipsRestClient(@Value("${fips.base-url}") String fipsBaseUrl) {
        return RestClient.builder()
                .baseUrl(fipsBaseUrl)
                .build();
    }
}
