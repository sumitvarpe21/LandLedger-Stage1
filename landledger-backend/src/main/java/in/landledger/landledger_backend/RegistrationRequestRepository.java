package in.landledger.landledger_backend;

import java.util.List;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, UUID> {
	List<RegistrationRequest> findAllByOrderByCreatedAtDesc();
	List<RegistrationRequest> findByApplicantIdOrderByCreatedAtDesc(UUID applicantId);
	boolean existsByParcelIdIgnoreCaseAndStatusIn(String parcelId, Collection<RegistrationRequest.Status> statuses);
	boolean existsByParcelIdIgnoreCaseAndStatusInAndIdNot(String parcelId, Collection<RegistrationRequest.Status> statuses, UUID id);
	long countByStatusIn(List<RegistrationRequest.Status> statuses);
	long countByApplicantIdAndStatusIn(UUID applicantId, List<RegistrationRequest.Status> statuses);
}
