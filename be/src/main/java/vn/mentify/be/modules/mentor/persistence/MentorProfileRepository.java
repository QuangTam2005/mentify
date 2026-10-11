package vn.mentify.be.modules.mentor.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.mentify.be.modules.mentor.domain.MentorProfileEntity;
import vn.mentify.be.modules.mentor.domain.MentorStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MentorProfileRepository extends JpaRepository<MentorProfileEntity, UUID> {

    Optional<MentorProfileEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<MentorProfileEntity> findByMentorStatus(MentorStatus mentorStatus);
}
