package vn.mentify.be.modules.auth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentityEntity, UUID> {

    Optional<UserIdentityEntity> findByProviderAndProviderSubject(String provider, String providerSubject);

    boolean existsByProviderAndProviderSubject(String provider, String providerSubject);
}
