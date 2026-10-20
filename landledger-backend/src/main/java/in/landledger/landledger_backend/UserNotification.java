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
@Table(name = "user_notifications")
public class UserNotification {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserAccount user;

	@Column(nullable = false, length = 500)
	private String text;

	@Column(nullable = false)
	private Instant createdAt;

	public UserNotification() {
	}

	public UserNotification(UserAccount user, String text) {
		this.user = user;
		this.text = text;
	}

	@PrePersist
	void onCreate() { createdAt = Instant.now(); }

	public UUID getId() { return id; }
	public UserAccount getUser() { return user; }
	public String getText() { return text; }
	public Instant getCreatedAt() { return createdAt; }
}
