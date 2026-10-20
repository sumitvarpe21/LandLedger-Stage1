package in.landledger.landledger_backend;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import in.landledger.landledger_backend.ApiModels.AccountUpdateInput;
import in.landledger.landledger_backend.ApiModels.ApplicationView;
import in.landledger.landledger_backend.ApiModels.AuditView;
import in.landledger.landledger_backend.ApiModels.AuthResponse;
import in.landledger.landledger_backend.ApiModels.LandInput;
import in.landledger.landledger_backend.ApiModels.LoginInput;
import in.landledger.landledger_backend.ApiModels.NoticeView;
import in.landledger.landledger_backend.ApiModels.ParcelCreateInput;
import in.landledger.landledger_backend.ApiModels.ParcelView;
import in.landledger.landledger_backend.ApiModels.ProfileInput;
import in.landledger.landledger_backend.ApiModels.PublicUser;
import in.landledger.landledger_backend.ApiModels.RegisterInput;
import in.landledger.landledger_backend.ApiModels.ReviewInput;
import in.landledger.landledger_backend.ApiModels.StatsView;
import in.landledger.landledger_backend.ApiModels.TransferDecisionInput;
import in.landledger.landledger_backend.ApiModels.TransferInput;
import in.landledger.landledger_backend.ApiModels.TransferView;

@RestController
@RequestMapping("/api")
public class LandLedgerController {

	private final LandLedgerService service;

	public LandLedgerController(LandLedgerService service) {
		this.service = service;
	}

	@PostMapping("/auth/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterInput input) {
		return service.register(input);
	}

	@PostMapping("/auth/login")
	public AuthResponse login(@Valid @RequestBody LoginInput input) {
		return service.login(input);
	}

	@GetMapping("/auth/me")
	public PublicUser me(JwtAuthenticationToken authentication) {
		return service.currentUser(currentId(authentication));
	}

	@PutMapping("/users/me")
	public PublicUser updateProfile(
			JwtAuthenticationToken authentication,
			@Valid @RequestBody ProfileInput input) {
		return service.updateProfile(currentId(authentication), input);
	}

	@GetMapping("/registration-requests")
	public List<ApplicationView> listApplications(JwtAuthenticationToken authentication) {
		return service.listApplications(currentId(authentication));
	}

	@GetMapping("/registration-requests/{id}")
	public ApplicationView getApplication(
			JwtAuthenticationToken authentication,
			@PathVariable UUID id) {
		return service.getApplication(currentId(authentication), id);
	}

	@PostMapping("/registration-requests")
	@ResponseStatus(HttpStatus.CREATED)
	public ApplicationView submitApplication(
			JwtAuthenticationToken authentication,
			@Valid @RequestBody LandInput input) {
		return service.submitApplication(currentId(authentication), input);
	}

	@PutMapping("/registration-requests/{id}")
	public ApplicationView resubmitApplication(
			JwtAuthenticationToken authentication,
			@PathVariable UUID id,
			@Valid @RequestBody LandInput input) {
		return service.resubmitApplication(currentId(authentication), id, input);
	}

	@PatchMapping("/registration-requests/{id}/review")
	public ApplicationView reviewApplication(
			JwtAuthenticationToken authentication,
			@PathVariable UUID id,
			@Valid @RequestBody ReviewInput input) {
		return service.reviewApplication(currentId(authentication), id, input);
	}

	@GetMapping("/lands")
	public List<ParcelView> listParcels(
			JwtAuthenticationToken authentication,
			@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "false") boolean mine) {
		return service.listParcels(currentId(authentication), q, mine);
	}

	@PostMapping("/lands")
	@ResponseStatus(HttpStatus.CREATED)
	public ParcelView createParcel(
			JwtAuthenticationToken authentication,
			@Valid @RequestBody ParcelCreateInput input) {
		return service.createParcel(currentId(authentication), input);
	}

	@GetMapping("/lands/{id}")
	public ParcelView getParcel(JwtAuthenticationToken authentication, @PathVariable UUID id) {
		return service.getParcel(currentId(authentication), id);
	}

	@GetMapping("/transfers")
	public List<TransferView> listTransfers(JwtAuthenticationToken authentication) {
		return service.listTransfers(currentId(authentication));
	}

	@PostMapping("/transfers")
	@ResponseStatus(HttpStatus.CREATED)
	public TransferView requestTransfer(
			JwtAuthenticationToken authentication,
			@Valid @RequestBody TransferInput input) {
		return service.requestTransfer(currentId(authentication), input);
	}

	@PatchMapping("/transfers/{id}")
	public TransferView decideTransfer(
			JwtAuthenticationToken authentication,
			@PathVariable UUID id,
			@Valid @RequestBody TransferDecisionInput input) {
		return service.decideTransfer(currentId(authentication), id, input);
	}

	@GetMapping("/notifications")
	public List<NoticeView> notifications(JwtAuthenticationToken authentication) {
		return service.listNotifications(currentId(authentication));
	}

	@GetMapping("/admin/users")
	public List<PublicUser> users(JwtAuthenticationToken authentication) {
		return service.listUsers(currentId(authentication));
	}

	@PatchMapping("/admin/users/{id}")
	public PublicUser updateUser(
			JwtAuthenticationToken authentication,
			@PathVariable UUID id,
			@Valid @RequestBody AccountUpdateInput input) {
		return service.updateUser(currentId(authentication), id, input);
	}

	@GetMapping("/audit-logs")
	public List<AuditView> auditLogs(JwtAuthenticationToken authentication) {
		return service.listAudit(currentId(authentication));
	}

	@GetMapping("/dashboard/stats")
	public StatsView stats(JwtAuthenticationToken authentication) {
		return service.stats(currentId(authentication));
	}

	private UUID currentId(JwtAuthenticationToken authentication) {
		return UUID.fromString(authentication.getName());
	}
}
