package vn.mentify.be.modules.auth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionTokenEntity, UUID> {

    Optional<AccountActionTokenEntity> findByTokenHashAndPurpose(String tokenHash, AccountActionTokenPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from AccountActionTokenEntity token where token.tokenHash = :tokenHash and token.purpose = :purpose")
    Optional<AccountActionTokenEntity> findByTokenHashAndPurposeForUpdate(
            @Param("tokenHash") String tokenHash,
            @Param("purpose") AccountActionTokenPurpose purpose);

    Optional<AccountActionTokenEntity> findTopByUserIdAndPurposeOrderByCreatedAtDesc(
            UUID userId,
            AccountActionTokenPurpose purpose);
}
