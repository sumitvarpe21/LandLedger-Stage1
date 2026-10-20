package in.landledger.landledger_backend;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "registration_requests")
public class RegistrationRequest {

	public enum Status {
		PENDING, UNDER_REVIEW, APPROVED, REJECTED
	}

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "applicant_id", nullable = false)
	private UserAccount applicant;

	@Column(name = "parcel_id", nullable = false, length = 80)
	private String parcelId;

	@Column(name = "survey_number", nullable = false, length = 80)
	private String surveyNumber;

	@Column(nullable = false, length = 120)
	private String village;

	@Column(nullable = false, length = 120)
	private String district;

	@Column(nullable = false, length = 120)
	private String state;

	@Column(nullable = false)
	private double area;

	@Column(name = "land_type", nullable = false, length = 40)
	private String landType;

	private Double latitude;
	private Double longitude;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private Status status = Status.PENDING;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by")
	private UserAccount reviewedBy;

	@Column(name = "review_remarks", length = 1000)
	private String reviewRemarks;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public RegistrationRequest() {
	}

	public RegistrationRequest(UserAccount applicant, ApiModels.LandInput input) {
		id = UUID.randomUUID();
		this.applicant = applicant;
		apply(input);
	}

	public void apply(ApiModels.LandInput input) {
		parcelId = input.parcelId().trim();
		surveyNumber = input.surveyNumber().trim();
		village = input.village().trim();
		district = input.district().trim();
		state = input.state().trim();
		area = input.area();
		landType = input.landType().trim();
		latitude = input.latitude();
		longitude = input.longitude();
		updatedAt = Instant.now();
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	public UUID getId() { return id; }
	public UserAccount getApplicant() { return applicant; }
	public String getParcelId() { return parcelId; }
	public String getSurveyNumber() { return surveyNumber; }
	public String getVillage() { return village; }
	public String getDistrict() { return district; }
	public String getState() { return state; }
	public double getArea() { return area; }
	public String getLandType() { return landType; }
	public Double getLatitude() { return latitude; }
	public Double getLongitude() { return longitude; }
	public Status getStatus() { return status; }
	public UserAccount getReviewedBy() { return reviewedBy; }
	public String getReviewRemarks() { return reviewRemarks; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public void setStatus(Status status) { this.status = status; }
	public void setReviewedBy(UserAccount reviewedBy) { this.reviewedBy = reviewedBy; }
	public void setReviewRemarks(String reviewRemarks) { this.reviewRemarks = reviewRemarks; }
}
