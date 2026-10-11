package vn.mentify.be.modules.mentor.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mentify.be.modules.mentor.entity.MentorProfileEntity;
import vn.mentify.be.modules.mentor.entity.MentorStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MentorProfileRepository extends JpaRepository<MentorProfileEntity, UUID> {

    Optional<MentorProfileEntity> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from MentorProfileEntity profile where profile.userId = :userId")
    Optional<MentorProfileEntity> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from MentorProfileEntity profile where profile.id = :profileId")
    Optional<MentorProfileEntity> findByIdForUpdate(@Param("profileId") UUID profileId);

    boolean existsByUserId(UUID userId);

    List<MentorProfileEntity> findByMentorStatus(MentorStatus mentorStatus);
}
