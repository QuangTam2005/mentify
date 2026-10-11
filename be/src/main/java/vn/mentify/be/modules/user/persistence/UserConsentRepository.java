package vn.mentify.be.modules.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserConsentRepository extends JpaRepository<UserConsentEntity, UUID> {

    List<UserConsentEntity> findByUserIdOrderByAcceptedAtDesc(UUID userId);
}
