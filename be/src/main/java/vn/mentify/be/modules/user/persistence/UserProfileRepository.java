package vn.mentify.be.modules.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {

    @Query("SELECT p FROM UserProfileEntity p " +
           "LEFT JOIN FETCH p.school " +
           "LEFT JOIN FETCH p.majorGroup " +
           "WHERE p.user.id = :userId")
    Optional<UserProfileEntity> findByUserId(@Param("userId") UUID userId);
}
