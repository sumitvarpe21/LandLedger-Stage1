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

	@Column(name = "blockchain_status", length = 24)
	private String blockchainStatus;

	@Column(name = "blockchain_payload", columnDefinition = "TEXT")
	private String blockchainPayload;

	@Column(name = "blockchain_payload_hash", length = 66)
	private String blockchainPayloadHash;

	@Column(name = "blockchain_attempts", nullable = false)
	private int blockchainAttempts;

	@Column(name = "blockchain_error", length = 500)
	private String blockchainError;

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
	public String getBlockchainStatus() { return blockchainStatus; }
	public String getBlockchainPayload() { return blockchainPayload; }
	public String getBlockchainPayloadHash() { return blockchainPayloadHash; }
	public int getBlockchainAttempts() { return blockchainAttempts; }
	public String getBlockchainError() { return blockchainError; }
	public Instant getOccurredAt() { return occurredAt; }
	public void queueBlockchainProof(String payload, String payloadHash) {
		blockchainPayload = payload;
		blockchainPayloadHash = payloadHash;
		blockchainStatus = "PENDING";
		blockchainError = null;
	}
	public void setTransactionReference(String transactionReference) {
		this.transactionReference = transactionReference;
	}
	public void recordBlockchainAttempt(String error) {
		blockchainAttempts++;
		blockchainError = error;
	}
	public void confirmBlockchainProof(String transactionReference) {
		this.transactionReference = transactionReference;
		blockchainStatus = "CONFIRMED";
		blockchainError = null;
	}
	public void failBlockchainProof(String error) {
		blockchainStatus = "FAILED";
		blockchainError = error;
	}
}
