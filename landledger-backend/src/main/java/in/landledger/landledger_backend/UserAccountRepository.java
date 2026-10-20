package in.landledger.landledger_backend;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
	Optional<UserAccount> findByEmailIgnoreCase(String email);
	boolean existsByEmailIgnoreCase(String email);
	boolean existsByRole(UserAccount.Role role);
}
