package in.landledger.landledger_backend;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class UserAccount {

	public enum Role {
		CITIZEN, LAND_OFFICER, ADMIN
	}

	@Id
	private UUID id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private Role role;

	@Column(nullable = false, length = 40)
	private String phone = "";

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public UserAccount() {
	}

	public UserAccount(String name, String email, String passwordHash, Role role, String phone) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.phone = phone;
	}

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

	@PreUpdate
	void onUpdate() {
		email = email.toLowerCase();
	}

	public UUID getId() { return id; }
	public String getName() { return name; }
	public String getEmail() { return email; }
	public String getPasswordHash() { return passwordHash; }
	public Role getRole() { return role; }
	public String getPhone() { return phone; }
	public boolean isActive() { return active; }
	public Instant getCreatedAt() { return createdAt; }
	public void setName(String name) { this.name = name; }
	public void setPhone(String phone) { this.phone = phone; }
	public void setRole(Role role) { this.role = role; }
	public void setActive(boolean active) { this.active = active; }
}
