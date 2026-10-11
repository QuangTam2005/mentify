package vn.mentify.be.modules.mentor.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.mentify.be.modules.mentor.domain.MentorApplicationEntity;
import vn.mentify.be.modules.mentor.domain.MentorApplicationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MentorApplicationRepository extends JpaRepository<MentorApplicationEntity, UUID> {

    List<MentorApplicationEntity> findByMentorProfileIdOrderByAttemptNumberDesc(UUID mentorProfileId);

    List<MentorApplicationEntity> findByUserIdOrderBySubmittedAtDesc(UUID userId);

    Optional<MentorApplicationEntity> findFirstByMentorProfileIdOrderByAttemptNumberDesc(UUID mentorProfileId);

    List<MentorApplicationEntity> findByStatus(MentorApplicationStatus status);

    Optional<MentorApplicationEntity> findByMentorProfileIdAndAttemptNumber(UUID mentorProfileId, int attemptNumber);
}
