package in.landledger.landledger_backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = HealthController.class, properties = {
	"landledger.frontend-origin=http://localhost:8080",
	"landledger.jwt-secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
})
@Import(SecurityConfiguration.class)
class HealthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthIsAvailableWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/health"))
			.andExpect(status().isOk())
			.andExpect(content().json("{\"status\":\"UP\",\"service\":\"LandLedger Backend\"}"));
	}

	@Test
	void healthAllowsRequestsFromTheConfiguredFrontendOrigin() throws Exception {
		mockMvc.perform(get("/api/health").header("Origin", "http://localhost:8080"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"));
	}

	@Test
	void otherEndpointsRemainClosedUntilTheyAreImplemented() throws Exception {
		mockMvc.perform(get("/api/private"))
			.andExpect(status().isUnauthorized());
	}
}
