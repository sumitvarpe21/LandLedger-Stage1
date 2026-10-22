package in.landledger.landledger_backend;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

public final class ApiModels {

	private ApiModels() {
	}

	public record PublicUser(UUID id, String name, String email, String role, String phone, boolean active) {
		static PublicUser from(UserAccount user) {
			return new PublicUser(user.getId(), user.getName(), user.getEmail(),
				user.getRole().name(), user.getPhone(), user.isActive());
		}
	}

	public record AuthResponse(String token, PublicUser user) {
	}

	public record RegisterInput(
			@NotBlank @Size(max = 120) String name,
			@NotBlank @Email @Size(max = 254) String email,
			@NotBlank @Size(min = 8, max = 72) String password,
			@Size(max = 40) String phone) {
	}

	public record LoginInput(@NotBlank @Email String email, @NotBlank String password) {
	}

	public record LandInput(
			@NotBlank @Size(max = 80) String parcelId,
			@NotBlank @Size(max = 80) String surveyNumber,
			@NotBlank @Size(max = 120) String village,
			@NotBlank @Size(max = 120) String district,
			@NotBlank @Size(max = 120) String state,
			@NotNull @Positive @DecimalMax("1000000000.0") Double area,
			@NotBlank @Size(max = 40) String landType,
			@DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
			@DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {
		@jakarta.validation.constraints.AssertTrue(message = "Latitude and longitude must be provided together")
		public boolean isCoordinatePairValid() {
			return (latitude == null) == (longitude == null);
		}
	}

	public record BoundaryPoint(
			@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
			@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {
	}

	public record ParcelCreateInput(
			@NotNull @Valid LandInput land,
			@NotBlank @Email String ownerEmail,
			@NotNull @Size(min = 3, max = 500) List<@NotNull @Valid BoundaryPoint> boundary) {
	}

	public record ReviewInput(
			@NotNull RegistrationRequest.Status status,
			@Size(max = 1000) String remarks,
			@Size(min = 3, max = 500) List<@NotNull @Valid BoundaryPoint> boundary) {
	}

	public record ProfileInput(@NotBlank @Size(max = 120) String name, @Size(max = 40) String phone) {
	}

	public record AccountUpdateInput(UserAccount.Role role, Boolean active) {
	}

	public record TransferInput(@NotBlank @Size(max = 80) String parcelId, @NotBlank @Email String toEmail) {
	}

	public record TransferDecisionInput(
			@NotNull OwnershipTransfer.Status status,
			@Size(max = 1000) String reason) {
	}

	public record ApplicationView(
			UUID id, UUID citizenId, String applicantName, String parcelId, String surveyNumber,
			String village, String district, String state, double area, String landType,
			Double latitude, Double longitude, String status, String rejectionReason,
			String reviewedBy, Instant createdAt, Instant updatedAt) {
		static ApplicationView from(RegistrationRequest request) {
			return new ApplicationView(request.getId(), request.getApplicant().getId(),
				request.getApplicant().getName(), request.getParcelId(), request.getSurveyNumber(),
				request.getVillage(), request.getDistrict(), request.getState(), request.getArea(),
				request.getLandType(), request.getLatitude(), request.getLongitude(),
				request.getStatus().name(), request.getReviewRemarks(),
				request.getReviewedBy() == null ? null : request.getReviewedBy().getName(),
				request.getCreatedAt(), request.getUpdatedAt());
		}
	}

	public record HistoryView(
			String ownerName, String event, Instant date, String txRef, String blockchainStatus) {
		static HistoryView from(OwnershipHistoryEntry entry, boolean canViewOwner) {
			return new HistoryView(canViewOwner ? entry.getOwner().getName() : "Owner record",
				entry.getEvent(), entry.getOccurredAt(), entry.getTransactionReference(),
				entry.getBlockchainStatus());
		}
	}

	public record ParcelView(
			UUID id, String parcelId, String surveyNumber, String village, String district, String state,
			double area, String landType, UUID ownerId, String ownerName, Double latitude, Double longitude,
			List<BoundaryPoint> boundary, List<HistoryView> history) {
		static ParcelView from(
				LandParcel parcel,
				List<BoundaryPoint> boundary,
				List<OwnershipHistoryEntry> history,
				boolean canViewOwner) {
			return new ParcelView(parcel.getId(), parcel.getParcelId(), parcel.getSurveyNumber(),
				parcel.getVillage(), parcel.getDistrict(), parcel.getState(), parcel.getArea(),
				parcel.getLandType(), canViewOwner ? parcel.getOwner().getId() : null,
				canViewOwner ? parcel.getOwner().getName() : null,
				parcel.getLatitude(), parcel.getLongitude(), boundary,
				history.stream().map(entry -> HistoryView.from(entry, canViewOwner)).toList());
		}
	}

	public record TransferView(
			UUID id, String parcelId, UUID fromId, String fromName, String toEmail, String toName,
			String status, String reason, Instant createdAt, Instant decidedAt) {
		static TransferView from(OwnershipTransfer transfer) {
			return new TransferView(transfer.getId(), transfer.getParcel().getParcelId(),
				transfer.getFromOwner().getId(), transfer.getFromOwner().getName(),
				transfer.getToOwner().getEmail(), transfer.getToOwner().getName(),
				transfer.getStatus().name(), transfer.getReason(),
				transfer.getCreatedAt(), transfer.getDecidedAt());
		}
	}

	public record NoticeView(UUID id, String userId, String text, Instant at) {
		static NoticeView from(UserNotification notification) {
			return new NoticeView(notification.getId(), notification.getUser().getId().toString(),
				notification.getText(), notification.getCreatedAt());
		}
	}

	public record AuditView(UUID id, String actor, String action, Instant at) {
		static AuditView from(AuditEntry entry) {
			return new AuditView(entry.getId(), entry.getActor(), entry.getAction(), entry.getCreatedAt());
		}
	}

	public record StatsView(long users, long parcels, long pending, long transfers) {
	}
}
