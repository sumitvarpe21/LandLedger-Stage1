package in.landledger.landledger_backend;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(BlockchainProperties.class)
@EnableScheduling
class BlockchainConfiguration {
}
