package in.landledger.landledger_backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminBootstrap {

	@Bean
	ApplicationRunner bootstrapInitialAdministrator(
			LandLedgerService service,
			@Value("${landledger.admin.name:}") String name,
			@Value("${landledger.admin.email:}") String email,
			@Value("${landledger.admin.password:}") String password) {
		return arguments -> service.bootstrapAdmin(name, email, password);
	}
}
