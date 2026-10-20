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
@Table(name = "ownership_transfers")
public class OwnershipTransfer {

	public enum Status {
		PENDING, APPROVED, REJECTED
	}

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "parcel_id", nullable = false)
	private LandParcel parcel;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "from_owner_id", nullable = false)
	private UserAccount fromOwner;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "to_owner_id", nullable = false)
	private UserAccount toOwner;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requested_by", nullable = false)
	private UserAccount requestedBy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private Status status = Status.PENDING;

	@Column(length = 1000)
	private String reason;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by")
	private UserAccount reviewedBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	private Instant decidedAt;

	public OwnershipTransfer() {
	}

	public OwnershipTransfer(LandParcel parcel, UserAccount fromOwner, UserAccount toOwner) {
		id = UUID.randomUUID();
		this.parcel = parcel;
		this.fromOwner = fromOwner;
		this.toOwner = toOwner;
		requestedBy = fromOwner;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

	public UUID getId() { return id; }
	public LandParcel getParcel() { return parcel; }
	public UserAccount getFromOwner() { return fromOwner; }
	public UserAccount getToOwner() { return toOwner; }
	public UserAccount getRequestedBy() { return requestedBy; }
	public Status getStatus() { return status; }
	public String getReason() { return reason; }
	public UserAccount getReviewedBy() { return reviewedBy; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getDecidedAt() { return decidedAt; }
	public void setStatus(Status status) { this.status = status; }
	public void setReason(String reason) { this.reason = reason; }
	public void setReviewedBy(UserAccount reviewedBy) { this.reviewedBy = reviewedBy; }
	public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
