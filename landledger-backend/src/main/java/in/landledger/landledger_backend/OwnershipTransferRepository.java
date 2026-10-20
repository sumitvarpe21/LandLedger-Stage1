package in.landledger.landledger_backend;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface OwnershipTransferRepository extends JpaRepository<OwnershipTransfer, UUID> {
	List<OwnershipTransfer> findAllByOrderByCreatedAtDesc();
	List<OwnershipTransfer> findByFromOwnerIdOrToOwnerIdOrderByCreatedAtDesc(UUID fromOwnerId, UUID toOwnerId);
	boolean existsByParcel_IdAndStatus(UUID parcelId, OwnershipTransfer.Status status);
	long countByStatus(OwnershipTransfer.Status status);
	long countByFromOwner_IdOrToOwner_IdAndStatus(UUID fromOwnerId, UUID toOwnerId, OwnershipTransfer.Status status);
}
