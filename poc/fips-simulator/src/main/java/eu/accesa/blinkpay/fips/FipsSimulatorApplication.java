package eu.accesa.blinkpay.fips;

import eu.accesa.blinkpay.fips.config.BankRoutingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BankRoutingProperties.class)
public class FipsSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FipsSimulatorApplication.class, args);
    }
}
