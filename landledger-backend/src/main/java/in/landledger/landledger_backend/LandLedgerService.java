package in.landledger.landledger_backend;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.landledger.landledger_backend.ApiModels.AccountUpdateInput;
import in.landledger.landledger_backend.ApiModels.ApplicationView;
import in.landledger.landledger_backend.ApiModels.AuditView;
import in.landledger.landledger_backend.ApiModels.AuthResponse;
import in.landledger.landledger_backend.ApiModels.BoundaryPoint;
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
import in.landledger.landledger_backend.OwnershipTransfer.Status;

@Service
public class LandLedgerService {

	private final UserAccountRepository users;
	private final LandParcelRepository parcels;
	private final RegistrationRequestRepository requests;
	private final OwnershipTransferRepository transfers;
	private final AuditEntryRepository audit;
	private final UserNotificationRepository notifications;
	private final OwnershipHistoryRepository history;
	private final PasswordEncoder passwords;
	private final JwtEncoder jwtEncoder;
	private final JsonMapper jsonMapper;

	@Value("${landledger.jwt-issuer}")
	private String jwtIssuer;

	@Value("${landledger.jwt-expiration}")
	private java.time.Duration jwtExpiration;

	public LandLedgerService(
			UserAccountRepository users,
			LandParcelRepository parcels,
			RegistrationRequestRepository requests,
			OwnershipTransferRepository transfers,
			AuditEntryRepository audit,
			UserNotificationRepository notifications,
			OwnershipHistoryRepository history,
			PasswordEncoder passwords,
			JwtEncoder jwtEncoder,
			JsonMapper jsonMapper) {
		this.users = users;
		this.parcels = parcels;
		this.requests = requests;
		this.transfers = transfers;
		this.audit = audit;
		this.notifications = notifications;
		this.history = history;
		this.passwords = passwords;
		this.jwtEncoder = jwtEncoder;
		this.jsonMapper = jsonMapper;
	}

	@Transactional
	public AuthResponse register(RegisterInput input) {
		if (input.password().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw ApiException.badRequest("Password must be at most 72 UTF-8 bytes");
		}
		String email = normalizeEmail(input.email());
		if (users.existsByEmailIgnoreCase(email)) {
			throw ApiException.conflict("Email is already registered");
		}
		UserAccount user = new UserAccount(input.name().trim(), email,
			passwords.encode(input.password()), UserAccount.Role.CITIZEN,
			input.phone() == null ? "" : input.phone().trim());
		try {
			users.saveAndFlush(user);
		} catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict("Email is already registered");
		}
		recordAudit(user, "Registered as CITIZEN");
		return authResponse(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginInput input) {
		UserAccount user = users.findByEmailIgnoreCase(normalizeEmail(input.email()))
			.orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
		if (!user.isActive() || !passwords.matches(input.password(), user.getPasswordHash())) {
			throw ApiException.unauthorized("Invalid email or password");
		}
		return authResponse(user);
	}

	@Transactional(readOnly = true)
	public PublicUser currentUser(UUID userId) {
		return PublicUser.from(requireUser(userId));
	}

	@Transactional
	public PublicUser updateProfile(UUID userId, ProfileInput input) {
		UserAccount user = requireUser(userId);
		user.setName(input.name().trim());
		user.setPhone(input.phone() == null ? "" : input.phone().trim());
		recordAudit(user, "Updated profile");
		return PublicUser.from(user);
	}

	@Transactional(readOnly = true)
	public List<ApplicationView> listApplications(UUID actorId) {
		UserAccount actor = requireUser(actorId);
		List<RegistrationRequest> found = actor.getRole() == UserAccount.Role.CITIZEN
			? requests.findByApplicantIdOrderByCreatedAtDesc(actorId)
			: requests.findAllByOrderByCreatedAtDesc();
		return found.stream().map(ApplicationView::from).toList();
	}

	@Transactional
	public ApplicationView submitApplication(UUID actorId, LandInput input) {
		UserAccount applicant = requireRole(actorId, UserAccount.Role.CITIZEN);
		validateCoordinates(input.latitude(), input.longitude());
		String parcelId = input.parcelId().trim();
		if (parcels.existsByParcelIdIgnoreCase(parcelId) || hasActiveRequest(parcelId)) {
			throw ApiException.conflict("An application or parcel with this Parcel ID already exists");
		}
		RegistrationRequest request;
		try {
			request = requests.saveAndFlush(new RegistrationRequest(applicant, input));
		} catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict("An application or parcel with this Parcel ID already exists");
		}
		recordAudit(applicant, "Submitted registration request " + request.getId());
		return ApplicationView.from(request);
	}

	@Transactional(readOnly = true)
	public ApplicationView getApplication(UUID actorId, UUID requestId) {
		UserAccount actor = requireUser(actorId);
		RegistrationRequest request = requests.findById(requestId)
			.orElseThrow(() -> ApiException.notFound("Registration request not found"));
		if (actor.getRole() == UserAccount.Role.CITIZEN
				&& !request.getApplicant().getId().equals(actorId)) {
			throw ApiException.notFound("Registration request not found");
		}
		return ApplicationView.from(request);
	}

	@Transactional
	public ApplicationView resubmitApplication(UUID actorId, UUID requestId, LandInput input) {
		UserAccount applicant = requireRole(actorId, UserAccount.Role.CITIZEN);
		RegistrationRequest request = requests.findById(requestId)
			.orElseThrow(() -> ApiException.notFound("Registration request not found"));
		if (!request.getApplicant().getId().equals(actorId)) {
			throw ApiException.notFound("Registration request not found");
		}
		if (request.getStatus() != RegistrationRequest.Status.REJECTED) {
			throw ApiException.conflict("Only rejected requests can be resubmitted");
		}
		validateCoordinates(input.latitude(), input.longitude());
		String parcelId = input.parcelId().trim();
		if (parcels.existsByParcelIdIgnoreCase(parcelId) || hasActiveRequest(parcelId)) {
			throw ApiException.conflict("An application or parcel with this Parcel ID already exists");
		}
		request.apply(input);
		request.setStatus(RegistrationRequest.Status.PENDING);
		request.setReviewRemarks(null);
		request.setReviewedBy(null);
		recordAudit(applicant, "Resubmitted registration request " + requestId);
		return ApplicationView.from(request);
	}

	@Transactional
	public ApplicationView reviewApplication(UUID reviewerId, UUID requestId, ReviewInput input) {
		UserAccount reviewer = requireUser(reviewerId);
		if (reviewer.getRole() != UserAccount.Role.LAND_OFFICER
				&& reviewer.getRole() != UserAccount.Role.ADMIN) {
			throw ApiException.forbidden("Not permitted for your role");
		}
		if (input.status() == RegistrationRequest.Status.APPROVED
				&& reviewer.getRole() != UserAccount.Role.ADMIN) {
			throw ApiException.forbidden("Only an administrator can register a parcel");
		}
		if (input.status() == RegistrationRequest.Status.APPROVED
				&& (input.boundary() == null || input.boundary().size() < 3)) {
			throw ApiException.badRequest("Draw at least three points for the parcel boundary before approval");
		}
		RegistrationRequest request = requests.findById(requestId)
			.orElseThrow(() -> ApiException.notFound("Registration request not found"));
		if (request.getStatus() == RegistrationRequest.Status.APPROVED
				|| request.getStatus() == RegistrationRequest.Status.REJECTED) {
			throw ApiException.conflict("This registration request has already been decided");
		}
		if (input.status() == RegistrationRequest.Status.PENDING) {
			throw ApiException.badRequest("A request cannot be reviewed back to PENDING");
		}
		if (input.status() == RegistrationRequest.Status.REJECTED
				&& (input.remarks() == null || input.remarks().isBlank())) {
			throw ApiException.badRequest("A rejection reason is required");
		}
		request.setStatus(input.status());
		request.setReviewedBy(reviewer);
		request.setReviewRemarks(input.remarks() == null ? null : input.remarks().trim());
		if (input.status() == RegistrationRequest.Status.APPROVED) {
			if (parcels.existsByParcelIdIgnoreCase(request.getParcelId())) {
				throw ApiException.conflict("Parcel ID is already registered");
			}
			LandParcel parcel;
			try {
				parcel = parcels.saveAndFlush(new LandParcel(request, encodeBoundary(input.boundary())));
			} catch (DataIntegrityViolationException exception) {
				throw ApiException.conflict("Parcel ID is already registered");
			}
			history.save(new OwnershipHistoryEntry(parcel, request.getApplicant(), "Registration approved"));
			createNotification(request.getApplicant(), "Registration request " + requestId + " was approved");
		} else if (input.status() == RegistrationRequest.Status.REJECTED) {
			createNotification(request.getApplicant(),
				"Registration request " + requestId + " was rejected: " + request.getReviewRemarks());
		} else {
			createNotification(request.getApplicant(), "Registration request " + requestId + " is under review");
		}
		recordAudit(reviewer, "Reviewed registration request " + requestId + " as " + input.status());
		return ApplicationView.from(request);
	}

	@Transactional
	public ParcelView createParcel(UUID adminId, ParcelCreateInput input) {
		UserAccount admin = requireRole(adminId, UserAccount.Role.ADMIN);
		LandInput land = input.land();
		validateCoordinates(land.latitude(), land.longitude());
		String parcelId = land.parcelId().trim();
		if (parcels.existsByParcelIdIgnoreCase(parcelId) || hasActiveRequest(parcelId)) {
			throw ApiException.conflict("An application or parcel with this Parcel ID already exists");
		}
		UserAccount owner = users.findByEmailIgnoreCase(normalizeEmail(input.ownerEmail()))
			.filter(user -> user.getRole() == UserAccount.Role.CITIZEN && user.isActive())
			.orElseThrow(() -> ApiException.badRequest("Parcel owner must be an active citizen"));
		LandParcel parcel;
		try {
			parcel = parcels.saveAndFlush(new LandParcel(land, owner, encodeBoundary(input.boundary())));
		} catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict("Parcel ID is already registered");
		}
		history.save(new OwnershipHistoryEntry(parcel, owner, "Parcel registered by administrator"));
		createNotification(owner, "Parcel " + parcel.getParcelId() + " was registered to you");
		recordAudit(admin, "Created parcel " + parcel.getParcelId() + " for " + owner.getEmail());
		return parcelView(parcel);
	}

	@Transactional(readOnly = true)
	public List<ParcelView> listParcels(UUID actorId, String query, boolean mineOnly) {
		UserAccount actor = requireUser(actorId);
		List<LandParcel> found = mineOnly
			? parcels.findByOwnerIdOrderByParcelIdAsc(actorId)
			: parcels.findAllByOrderByParcelIdAsc();
		String search = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		return found.stream()
			.filter(parcel -> search.isEmpty()
				|| (parcel.getParcelId() + " " + parcel.getSurveyNumber() + " "
					+ parcel.getVillage() + " " + parcel.getDistrict()).toLowerCase(Locale.ROOT).contains(search))
			.map(this::parcelView)
			.toList();
	}

	@Transactional(readOnly = true)
	public ParcelView getParcel(UUID actorId, UUID parcelDatabaseId) {
		UserAccount actor = requireUser(actorId);
		LandParcel parcel = parcels.findById(parcelDatabaseId)
			.orElseThrow(() -> ApiException.notFound("Parcel not found"));
		return parcelView(parcel);
	}

	@Transactional
	public TransferView requestTransfer(UUID actorId, TransferInput input) {
		UserAccount fromOwner = requireRole(actorId, UserAccount.Role.CITIZEN);
		LandParcel parcel = parcels.findByParcelIdIgnoreCase(input.parcelId().trim())
			.orElseThrow(() -> ApiException.notFound("Parcel not found"));
		if (!parcel.getOwner().getId().equals(actorId)) {
			throw ApiException.forbidden("You can only request a transfer for a parcel you own");
		}
		UserAccount toOwner = users.findByEmailIgnoreCase(normalizeEmail(input.toEmail()))
			.orElseThrow(() -> ApiException.badRequest("Recipient must be a registered citizen"));
		if (toOwner.getRole() != UserAccount.Role.CITIZEN || !toOwner.isActive()) {
			throw ApiException.badRequest("Recipient must be an active citizen");
		}
		if (toOwner.getId().equals(actorId)) {
			throw ApiException.badRequest("A parcel cannot be transferred to its current owner");
		}
		if (transfers.existsByParcel_IdAndStatus(parcel.getId(), Status.PENDING)) {
			throw ApiException.conflict("A transfer for this parcel is already pending");
		}
		OwnershipTransfer transfer;
		try {
			transfer = transfers.saveAndFlush(new OwnershipTransfer(parcel, fromOwner, toOwner));
		} catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict("A transfer for this parcel is already pending");
		}
		recordAudit(fromOwner, "Requested ownership transfer " + transfer.getId());
		return TransferView.from(transfer);
	}

	@Transactional(readOnly = true)
	public List<TransferView> listTransfers(UUID actorId) {
		UserAccount actor = requireUser(actorId);
		List<OwnershipTransfer> found = actor.getRole() == UserAccount.Role.CITIZEN
			? transfers.findByFromOwnerIdOrToOwnerIdOrderByCreatedAtDesc(actorId, actorId)
			: transfers.findAllByOrderByCreatedAtDesc();
		return found.stream().map(TransferView::from).toList();
	}

	@Transactional
	public TransferView decideTransfer(UUID officerId, UUID transferId, TransferDecisionInput input) {
		UserAccount officer = requireRole(officerId, UserAccount.Role.LAND_OFFICER);
		OwnershipTransfer transfer = transfers.findById(transferId)
			.orElseThrow(() -> ApiException.notFound("Transfer request not found"));
		if (transfer.getStatus() != Status.PENDING) {
			throw ApiException.conflict("Transfer request is no longer pending");
		}
		if (input.status() == Status.PENDING) {
			throw ApiException.badRequest("A pending transfer cannot be decided as PENDING");
		}
		if (input.status() == Status.REJECTED && (input.reason() == null || input.reason().isBlank())) {
			throw ApiException.badRequest("A rejection reason is required");
		}
		if (!transfer.getParcel().getOwner().getId().equals(transfer.getFromOwner().getId())) {
			throw ApiException.conflict("Current owner no longer matches the transfer request");
		}
		transfer.setStatus(input.status());
		transfer.setReason(input.reason() == null ? null : input.reason().trim());
		transfer.setReviewedBy(officer);
		transfer.setDecidedAt(Instant.now());
		if (input.status() == Status.APPROVED) {
			transfer.getParcel().setOwner(transfer.getToOwner());
			history.save(new OwnershipHistoryEntry(transfer.getParcel(), transfer.getToOwner(),
				"Transfer " + transferId + " approved"));
			createNotification(transfer.getToOwner(), "Parcel " + transfer.getParcel().getParcelId() + " was transferred to you");
		}
		createNotification(transfer.getFromOwner(),
			"Transfer request " + transferId + " was " + input.status()
				+ (transfer.getReason() == null ? "" : ": " + transfer.getReason()));
		recordAudit(officer, "Decided transfer " + transferId + " as " + input.status());
		return TransferView.from(transfer);
	}

	@Transactional(readOnly = true)
	public List<NoticeView> listNotifications(UUID actorId) {
		requireUser(actorId);
		return notifications.findTop100ByUserIdOrderByCreatedAtDesc(actorId).stream()
			.map(NoticeView::from).toList();
	}

	@Transactional(readOnly = true)
	public List<PublicUser> listUsers(UUID actorId) {
		requireRole(actorId, UserAccount.Role.ADMIN);
		return users.findAll().stream().map(PublicUser::from).toList();
	}

	@Transactional
	public PublicUser updateUser(UUID adminId, UUID targetId, AccountUpdateInput input) {
		UserAccount admin = requireRole(adminId, UserAccount.Role.ADMIN);
		if (adminId.equals(targetId)) {
			throw ApiException.badRequest("Admins cannot change their own account");
		}
		if (input.role() == null && input.active() == null) {
			throw ApiException.badRequest("Provide a role or active status to update");
		}
		UserAccount target = requireUser(targetId);
		if (input.role() != null) {
			target.setRole(input.role());
		}
		if (input.active() != null) {
			target.setActive(input.active());
		}
		recordAudit(admin, "Updated account " + target.getEmail());
		return PublicUser.from(target);
	}

	@Transactional(readOnly = true)
	public List<AuditView> listAudit(UUID actorId) {
		requireRole(actorId, UserAccount.Role.ADMIN);
		return audit.findTop100ByOrderByCreatedAtDesc().stream().map(AuditView::from).toList();
	}

	@Transactional(readOnly = true)
	public StatsView stats(UUID actorId) {
		UserAccount actor = requireUser(actorId);
		if (actor.getRole() == UserAccount.Role.CITIZEN) {
			return new StatsView(1, parcels.countByOwnerId(actorId),
				requests.countByApplicantIdAndStatusIn(actorId, List.of(
					RegistrationRequest.Status.PENDING, RegistrationRequest.Status.UNDER_REVIEW)),
				transfers.countByFromOwner_IdOrToOwner_IdAndStatus(actorId, actorId, Status.PENDING));
		}
		return new StatsView(users.count(), parcels.count(),
			requests.countByStatusIn(List.of(
				RegistrationRequest.Status.PENDING, RegistrationRequest.Status.UNDER_REVIEW)),
			transfers.countByStatus(Status.PENDING));
	}

	@Transactional
	public void bootstrapAdmin(String name, String email, String password) {
		boolean hasAnyConfiguration = !name.isBlank() || !email.isBlank() || !password.isBlank();
		if (!hasAnyConfiguration) {
			return;
		}
		if (name.isBlank() || email.isBlank() || password.isBlank()) {
			throw new IllegalStateException("Set LANDLEDGER_ADMIN_NAME, LANDLEDGER_ADMIN_EMAIL, and LANDLEDGER_ADMIN_PASSWORD together");
		}
		if (password.length() < 12 || password.length() > 72) {
			throw new IllegalStateException("LANDLEDGER_ADMIN_PASSWORD must be between 12 and 72 characters");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalStateException("LANDLEDGER_ADMIN_PASSWORD must be at most 72 UTF-8 bytes");
		}
		if (users.existsByRole(UserAccount.Role.ADMIN)) {
			return;
		}
		String normalizedEmail = normalizeEmail(email);
		if (users.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new IllegalStateException("The configured initial admin email already belongs to a non-admin account");
		}
		UserAccount admin = users.saveAndFlush(new UserAccount(name.trim(), normalizedEmail,
			passwords.encode(password), UserAccount.Role.ADMIN, ""));
		recordAudit(admin, "Initial administrator account bootstrapped");
	}

	private ParcelView parcelView(LandParcel parcel) {
		return ParcelView.from(parcel, decodeBoundary(parcel.getBoundaryCoordinates()),
			history.findByParcelIdOrderByOccurredAtAsc(parcel.getId()), true);
	}

	private String encodeBoundary(List<BoundaryPoint> boundary) {
		try {
			return jsonMapper.writeValueAsString(boundary);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Could not serialize parcel boundary", exception);
		}
	}

	private List<BoundaryPoint> decodeBoundary(String boundaryCoordinates) {
		if (boundaryCoordinates == null) {
			return List.of();
		}
		try {
			return jsonMapper.readValue(boundaryCoordinates, new TypeReference<List<BoundaryPoint>>() {});
		} catch (JacksonException exception) {
			throw new IllegalStateException("Could not read saved parcel boundary", exception);
		}
	}

	private boolean hasActiveRequest(String parcelId) {
		return requests.existsByParcelIdIgnoreCaseAndStatusIn(parcelId,
			List.of(RegistrationRequest.Status.PENDING, RegistrationRequest.Status.UNDER_REVIEW));
	}

	private AuthResponse authResponse(UserAccount user) {
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(jwtIssuer)
			.issuedAt(issuedAt)
			.expiresAt(issuedAt.plus(jwtExpiration))
			.subject(user.getId().toString())
			.claim("email", user.getEmail())
			.claim("role", user.getRole().name())
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return new AuthResponse(jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(),
			PublicUser.from(user));
	}

	private UserAccount requireUser(UUID id) {
		UserAccount user = users.findById(id).orElseThrow(() -> ApiException.unauthorized("User account not found"));
		if (!user.isActive()) {
			throw ApiException.unauthorized("Account is deactivated");
		}
		return user;
	}

	private UserAccount requireRole(UUID id, UserAccount.Role role) {
		UserAccount user = requireUser(id);
		if (user.getRole() != role) {
			throw ApiException.forbidden("Not permitted for your role");
		}
		return user;
	}

	private void recordAudit(UserAccount actor, String action) {
		audit.save(new AuditEntry(actor.getEmail(), action));
	}

	private void createNotification(UserAccount user, String text) {
		notifications.save(new UserNotification(user, text));
	}

	private void validateCoordinates(Double latitude, Double longitude) {
		if ((latitude == null) != (longitude == null)) {
			throw ApiException.badRequest("Latitude and longitude must be provided together");
		}
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
