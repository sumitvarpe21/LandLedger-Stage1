package in.landledger.landledger_backend;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {
	List<UserNotification> findTop100ByUserIdOrderByCreatedAtDesc(UUID userId);
}
