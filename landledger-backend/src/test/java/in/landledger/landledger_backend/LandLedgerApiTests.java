package in.landledger.landledger_backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LandLedgerApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper objectMapper;

	@Test
	void citizensCanRegisterAndPersistTheirOwnApplications() throws Exception {
		String email = "citizen-" + System.nanoTime() + "@landledger.test";
		MvcResult registration = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"Test Citizen","email":"%s","password":"Citizen-Test-Password-2026","phone":"1234567890"}
					""".formatted(email)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.user.role").value("CITIZEN"))
			.andExpect(jsonPath("$.user.password").doesNotExist())
			.andReturn();
		String token = tokenFrom(registration);

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(email));

		mockMvc.perform(post("/api/registration-requests")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"parcelId":"TEST-%s","surveyNumber":"12/A","village":"Test village","district":"Test district","state":"Test state","area":1.25,"landType":"Agricultural"}
					""".formatted(System.nanoTime())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING"))
			.andExpect(jsonPath("$.applicantName").value("Test Citizen"));

		mockMvc.perform(get("/api/registration-requests")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].applicantName").value("Test Citizen"));
	}

	@Test
	void citizenCannotReadAdminDataAndAnonymousUsersCannotReadRegistryData() throws Exception {
		mockMvc.perform(get("/api/admin/users"))
			.andExpect(status().isUnauthorized());

		String email = "citizen-" + System.nanoTime() + "@landledger.test";
		MvcResult registration = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"Limited Citizen","email":"%s","password":"Citizen-Test-Password-2026","phone":""}
					""".formatted(email)))
			.andExpect(status().isCreated())
			.andReturn();

		mockMvc.perform(get("/api/admin/users")
				.header("Authorization", "Bearer " + tokenFrom(registration)))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminRoleChangesArePersistedAndCitizenRegistrationCannotChooseARole() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"Attempted Admin","email":"role-tamper-%s@landledger.test","password":"Citizen-Test-Password-2026","phone":"","role":"ADMIN"}
					""".formatted(System.nanoTime())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.user.role").value("CITIZEN"));

		String adminToken = loginToken("test-admin@landledger.test", "Test-Administrator-Secret-2026");
		String citizenEmail = "officer-" + System.nanoTime() + "@landledger.test";
		MvcResult citizen = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"Officer Candidate","email":"%s","password":"Citizen-Test-Password-2026","phone":""}
					""".formatted(citizenEmail)))
			.andExpect(status().isCreated())
			.andReturn();
		JsonNode citizenJson = objectMapper.readTree(citizen.getResponse().getContentAsString());
		String citizenId = citizenJson.path("user").path("id").asText();

		mockMvc.perform(patch("/api/admin/users/" + citizenId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"role":"LAND_OFFICER"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("LAND_OFFICER"));
	}

	@Test
	void invalidRegistrationIsRejected() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"","email":"not-an-email","password":"short","phone":""}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void onlyAdminsCanCreateParcelsAndAssignThemToAnActiveCitizen() throws Exception {
		String citizenEmail = "parcel-owner-" + System.nanoTime() + "@landledger.test";
		String citizenToken = registerToken("Parcel Owner", citizenEmail);
		String adminToken = loginToken("test-admin@landledger.test", "Test-Administrator-Secret-2026");
		String parcelNumber = "ADMIN-" + System.nanoTime();
		String parcelInput = """
			{"land":{"parcelId":"%s","surveyNumber":"22/C","village":"Test village","district":"Test district","state":"Test state","area":3.5,"landType":"Agricultural"},"ownerEmail":"%s","boundary":[{"latitude":18.52,"longitude":73.85},{"latitude":18.521,"longitude":73.85},{"latitude":18.521,"longitude":73.851}]}
			""".formatted(parcelNumber, citizenEmail);

		mockMvc.perform(post("/api/lands")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(parcelInput))
			.andExpect(status().isForbidden());

		String officerEmail = "parcel-officer-" + System.nanoTime() + "@landledger.test";
		String officerRegistrationToken = registerToken("Officer Candidate", officerEmail);
		String officerId = objectMapper.readTree(mockMvc.perform(get("/api/auth/me")
				.header("Authorization", "Bearer " + officerRegistrationToken))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
			.path("id").asText();
		mockMvc.perform(patch("/api/admin/users/" + officerId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"LAND_OFFICER\"}"))
			.andExpect(status().isOk());
		String officerToken = loginToken(officerEmail, "Citizen-Test-Password-2026");

		mockMvc.perform(post("/api/lands")
				.header("Authorization", "Bearer " + officerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(parcelInput))
			.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/lands")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(parcelInput))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.ownerName").value("Parcel Owner"))
			.andExpect(jsonPath("$.surveyNumber").value("22/C"))
			.andExpect(jsonPath("$.area").value(3.5))
			.andExpect(jsonPath("$.boundary.length()").value(3))
			.andExpect(jsonPath("$.boundary[0].latitude").value(18.52))
			.andExpect(jsonPath("$.history[0].event").value("Parcel registered by administrator"));

		String viewerToken = registerToken("Parcel Viewer", "parcel-viewer-" + System.nanoTime() + "@landledger.test");
		mockMvc.perform(get("/api/lands?q=" + parcelNumber)
				.header("Authorization", "Bearer " + viewerToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].ownerName").value("Parcel Owner"))
			.andExpect(jsonPath("$[0].history[0].ownerName").value("Parcel Owner"));
	}

	@Test
	void onlyAdminCanApproveApplicationsAndOfficersCanDecideTransfers() throws Exception {
		String ownerEmail = "owner-" + System.nanoTime() + "@landledger.test";
		String recipientEmail = "recipient-" + System.nanoTime() + "@landledger.test";
		String officerEmail = "officer-" + System.nanoTime() + "@landledger.test";
		String ownerToken = registerToken("Parcel Owner", ownerEmail);
		String recipientToken = registerToken("New Owner", recipientEmail);
		String officerRegistrationToken = registerToken("Officer", officerEmail);
		String officerId = objectMapper.readTree(mockMvc.perform(get("/api/auth/me")
				.header("Authorization", "Bearer " + officerRegistrationToken))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
			.path("id").asText();
		String adminToken = loginToken("test-admin@landledger.test", "Test-Administrator-Secret-2026");

		mockMvc.perform(patch("/api/admin/users/" + officerId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"role\":\"LAND_OFFICER\"}"))
			.andExpect(status().isOk());
		String officerToken = loginToken(officerEmail, "Citizen-Test-Password-2026");

		String parcelNumber = "PARCEL-" + System.nanoTime();
		MvcResult application = mockMvc.perform(post("/api/registration-requests")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"parcelId":"%s","surveyNumber":"17/B","village":"Test village","district":"Test district","state":"Test state","area":2.5,"landType":"Agricultural"}
					""".formatted(parcelNumber)))
			.andExpect(status().isCreated())
			.andReturn();
		String requestId = objectMapper.readTree(application.getResponse().getContentAsString())
			.path("id").asText();

		mockMvc.perform(patch("/api/registration-requests/" + requestId + "/review")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"APPROVED\"}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(patch("/api/registration-requests/" + requestId + "/review")
				.header("Authorization", "Bearer " + officerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"APPROVED\"}"))
			.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/registration-requests/" + requestId + "/review")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"status":"APPROVED","boundary":[{"latitude":18.52,"longitude":73.85},{"latitude":18.521,"longitude":73.85},{"latitude":18.521,"longitude":73.851}]}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APPROVED"));

		MvcResult transfer = mockMvc.perform(post("/api/transfers")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"parcelId":"%s","toEmail":"%s"}
					""".formatted(parcelNumber, recipientEmail)))
			.andExpect(status().isCreated())
			.andReturn();
		String transferId = objectMapper.readTree(transfer.getResponse().getContentAsString())
			.path("id").asText();

		mockMvc.perform(patch("/api/transfers/" + transferId)
				.header("Authorization", "Bearer " + officerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"APPROVED\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APPROVED"));

		mockMvc.perform(get("/api/lands?mine=true")
				.header("Authorization", "Bearer " + recipientToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].parcelId").value(parcelNumber))
			.andExpect(jsonPath("$[0].ownerName").value("New Owner"))
			.andExpect(jsonPath("$[0].history.length()").value(2));
	}

	private String registerToken(String name, String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"%s","email":"%s","password":"Citizen-Test-Password-2026","phone":""}
					""".formatted(name, email)))
			.andExpect(status().isCreated())
			.andReturn();
		return tokenFrom(result);
	}

	private String loginToken(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(
					java.util.Map.of("email", email, "password", password))))
			.andExpect(status().isOk())
			.andReturn();
		return tokenFrom(result);
	}

	private String tokenFrom(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
	}
}
