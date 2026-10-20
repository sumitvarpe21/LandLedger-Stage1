package in.landledger.landledger_backend;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "ownership_history")
public class OwnershipHistoryEntry {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "parcel_id", nullable = false)
	private LandParcel parcel;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private UserAccount owner;

	@Column(nullable = false, length = 500)
	private String event;

	@Column(name = "transaction_reference", length = 200)
	private String transactionReference;

	@Column(nullable = false)
	private Instant occurredAt;

	public OwnershipHistoryEntry() {
	}

	public OwnershipHistoryEntry(LandParcel parcel, UserAccount owner, String event) {
		this.parcel = parcel;
		this.owner = owner;
		this.event = event;
	}

	@PrePersist
	void onCreate() { occurredAt = Instant.now(); }

	public UUID getId() { return id; }
	public LandParcel getParcel() { return parcel; }
	public UserAccount getOwner() { return owner; }
	public String getEvent() { return event; }
	public String getTransactionReference() { return transactionReference; }
	public Instant getOccurredAt() { return occurredAt; }
}
