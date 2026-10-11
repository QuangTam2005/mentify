package vn.mentify.be.modules.mentor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationAdminListItem;
import vn.mentify.be.modules.mentor.entity.MentorApplicationEntity;
import vn.mentify.be.modules.mentor.entity.MentorApplicationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MentorApplicationRepository extends JpaRepository<MentorApplicationEntity, UUID> {

    List<MentorApplicationEntity> findByMentorProfileIdOrderByAttemptNumberDesc(UUID mentorProfileId);

    List<MentorApplicationEntity> findByUserIdOrderBySubmittedAtDesc(UUID userId);

    Optional<MentorApplicationEntity> findFirstByUserIdOrderByAttemptNumberDesc(UUID userId);

    Optional<MentorApplicationEntity> findFirstByMentorProfileIdOrderByAttemptNumberDesc(UUID mentorProfileId);

    List<MentorApplicationEntity> findByStatus(MentorApplicationStatus status);

    Optional<MentorApplicationEntity> findByMentorProfileIdAndAttemptNumber(UUID mentorProfileId, int attemptNumber);

    @Query(value = "select new vn.mentify.be.modules.mentor.dto.response.MentorApplicationAdminListItem(" +
            "a.id, a.mentorProfileId, a.userId, u.email, up.fullName, a.status, a.attemptNumber, " +
            "a.appliedUserType, a.submittedAt) " +
            "from MentorApplicationEntity a " +
            "join UserEntity u on u.id = a.userId " +
            "left join UserProfileEntity up on up.user.id = a.userId " +
            "where a.status = :status and a.attemptNumber = " +
            "(select max(newer.attemptNumber) from MentorApplicationEntity newer " +
            "where newer.mentorProfileId = a.mentorProfileId) " +
            "order by a.submittedAt asc, a.id asc",
            countQuery = "select count(a) from MentorApplicationEntity a " +
                    "where a.status = :status and a.attemptNumber = " +
                    "(select max(newer.attemptNumber) from MentorApplicationEntity newer " +
                    "where newer.mentorProfileId = a.mentorProfileId)")
    Page<MentorApplicationAdminListItem> findLatestByStatus(
            @Param("status") MentorApplicationStatus status,
            Pageable pageable);
}
