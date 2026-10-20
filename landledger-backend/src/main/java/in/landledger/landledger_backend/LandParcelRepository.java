package in.landledger.landledger_backend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface LandParcelRepository extends JpaRepository<LandParcel, UUID> {
	List<LandParcel> findAllByOrderByParcelIdAsc();
	List<LandParcel> findByOwnerIdOrderByParcelIdAsc(UUID ownerId);
	Optional<LandParcel> findByParcelIdIgnoreCase(String parcelId);
	boolean existsByParcelIdIgnoreCase(String parcelId);
	long countByOwnerId(UUID ownerId);
}
