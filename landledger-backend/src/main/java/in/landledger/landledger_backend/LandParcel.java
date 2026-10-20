package in.landledger.landledger_backend;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "land_parcels", uniqueConstraints = @UniqueConstraint(name = "uk_land_parcels_parcel_id", columnNames = "parcel_id"))
public class LandParcel {

	@Id
	private UUID id;

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

	@Column(name = "boundary_coordinates", columnDefinition = "TEXT")
	private String boundaryCoordinates;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private UserAccount owner;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public LandParcel() {
	}

	public LandParcel(RegistrationRequest request, String boundaryCoordinates) {
		id = UUID.randomUUID();
		parcelId = request.getParcelId();
		surveyNumber = request.getSurveyNumber();
		village = request.getVillage();
		district = request.getDistrict();
		state = request.getState();
		area = request.getArea();
		landType = request.getLandType();
		latitude = request.getLatitude();
		longitude = request.getLongitude();
		this.boundaryCoordinates = boundaryCoordinates;
		owner = request.getApplicant();
	}

	public LandParcel(ApiModels.LandInput input, UserAccount owner, String boundaryCoordinates) {
		id = UUID.randomUUID();
		parcelId = input.parcelId().trim();
		surveyNumber = input.surveyNumber().trim();
		village = input.village().trim();
		district = input.district().trim();
		state = input.state().trim();
		area = input.area();
		landType = input.landType().trim();
		latitude = input.latitude();
		longitude = input.longitude();
		this.boundaryCoordinates = boundaryCoordinates;
		this.owner = owner;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

	public UUID getId() { return id; }
	public String getParcelId() { return parcelId; }
	public String getSurveyNumber() { return surveyNumber; }
	public String getVillage() { return village; }
	public String getDistrict() { return district; }
	public String getState() { return state; }
	public double getArea() { return area; }
	public String getLandType() { return landType; }
	public Double getLatitude() { return latitude; }
	public Double getLongitude() { return longitude; }
	public String getBoundaryCoordinates() { return boundaryCoordinates; }
	public UserAccount getOwner() { return owner; }
	public void setOwner(UserAccount owner) { this.owner = owner; }
}
