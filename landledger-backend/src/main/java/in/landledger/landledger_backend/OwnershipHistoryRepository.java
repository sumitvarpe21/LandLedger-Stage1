package in.landledger.landledger_backend;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface OwnershipHistoryRepository extends JpaRepository<OwnershipHistoryEntry, UUID> {
	List<OwnershipHistoryEntry> findByParcelIdOrderByOccurredAtAsc(UUID parcelId);
	List<OwnershipHistoryEntry> findTop20ByBlockchainStatusOrderByOccurredAtAsc(String blockchainStatus);
}
