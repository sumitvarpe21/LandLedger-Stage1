package in.landledger.landledger_backend;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID> {
	List<AuditEntry> findTop100ByOrderByCreatedAtDesc();
}
