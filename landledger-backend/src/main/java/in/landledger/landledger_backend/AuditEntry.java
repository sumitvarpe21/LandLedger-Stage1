package in.landledger.landledger_backend;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "audit_logs")
public class AuditEntry {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Column(nullable = false, length = 254)
	private String actor;

	@Column(nullable = false, length = 500)
	private String action;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public AuditEntry() {
	}

	public AuditEntry(String actor, String action) {
		this.actor = actor;
		this.action = action;
	}

	@PrePersist
	void onCreate() { createdAt = Instant.now(); }

	public UUID getId() { return id; }
	public String getActor() { return actor; }
	public String getAction() { return action; }
	public Instant getCreatedAt() { return createdAt; }
}
