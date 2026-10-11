package vn.mentify.be.modules.mentor.service.impl;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationAdminDecision;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationAdminDetail;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationAdminListItem;
import vn.mentify.be.modules.mentor.entity.MentorApplicationEntity;
import vn.mentify.be.modules.mentor.entity.MentorApplicationStatus;
import vn.mentify.be.modules.mentor.entity.MentorProfileEntity;
import vn.mentify.be.modules.mentor.entity.MentorStatus;
import vn.mentify.be.modules.mentor.repository.MentorApplicationRepository;
import vn.mentify.be.modules.mentor.repository.MentorProfileRepository;
import vn.mentify.be.modules.mentor.service.MentorApplicationAdminService;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserProfileEntity;
import vn.mentify.be.modules.user.persistence.UserProfileRepository;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.shared.api.ApiErrorCode;
import vn.mentify.be.shared.api.ApiException;
import vn.mentify.be.shared.api.ConflictException;
import vn.mentify.be.shared.api.ResourceNotFoundException;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@Service
public class MentorApplicationAdminServiceImpl implements MentorApplicationAdminService {

    private final MentorApplicationRepository applications;
    private final MentorProfileRepository profiles;
    private final UserRepository users;
    private final UserProfileRepository userProfiles;
    private final Clock clock;

    public MentorApplicationAdminServiceImpl(
            MentorApplicationRepository applications,
            MentorProfileRepository profiles,
            UserRepository users,
            UserProfileRepository userProfiles,
            Clock clock) {
        this.applications = Objects.requireNonNull(applications);
        this.profiles = Objects.requireNonNull(profiles);
        this.users = Objects.requireNonNull(users);
        this.userProfiles = Objects.requireNonNull(userProfiles);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MentorApplicationAdminListItem> listPending(Pageable pageable) {
        return applications.findLatestByStatus(MentorApplicationStatus.PENDING, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorApplicationAdminDetail getDetails(UUID applicationId) {
        MentorApplicationEntity application = findApplication(applicationId);
        MentorProfileEntity profile = profiles.findById(application.getMentorProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found"));
        UserEntity user = users.findById(application.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found"));
        String applicantName = userProfiles.findByUserId(user.getId())
                .map(UserProfileEntity::getFullName)
                .orElse(null);
        return new MentorApplicationAdminDetail(
                application.getId(), application.getMentorProfileId(), application.getUserId(),
                user.getEmail(), applicantName, application.getAppliedUserType(), application.getSchoolId(),
                application.getMajorGroupCode(), application.getCustomMajorName(), application.getBio(),
                profile.getExpertiseAreas(), profile.getDefaultMeetingLink(), application.getAttemptNumber(),
                application.getStatus(), profile.getMentorStatus(), profile.isAcceptingBookings(),
                application.getSubmittedAt(), application.getReviewedAt(), application.getRejectionReason());
    }

    @Override
    @Transactional
    public MentorApplicationAdminDecision approve(UUID applicationId) {
        return decide(applicationId, MentorApplicationStatus.APPROVED, null);
    }

    @Override
    @Transactional
    public MentorApplicationAdminDecision reject(UUID applicationId, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "A rejection reason is required");
        }
        String normalizedReason = rejectionReason.trim();
        if (normalizedReason.length() > 500) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "Rejection reason must not exceed 500 characters");
        }
        return decide(applicationId, MentorApplicationStatus.REJECTED, normalizedReason);
    }

    private MentorApplicationAdminDecision decide(
            UUID applicationId,
            MentorApplicationStatus decision,
            String rejectionReason) {
        MentorApplicationEntity requested = findApplication(applicationId);
        MentorProfileEntity profile;
        try {
            // Match BE-B02 lock order: lock the profile before reading its latest attempt.
            profile = profiles.findByIdForUpdate(requested.getMentorProfileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found"));
        } catch (PessimisticLockingFailureException exception) {
            throw new ConflictException("This mentor application is already being reviewed");
        }

        MentorApplicationEntity latest = applications
                .findFirstByMentorProfileIdOrderByAttemptNumberDesc(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mentor application not found"));
        if (!latest.getId().equals(applicationId) || latest.getStatus() != MentorApplicationStatus.PENDING) {
            throw new ConflictException("Only the latest pending application can be reviewed");
        }
        if (profile.getMentorStatus() == MentorStatus.SUSPENDED) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Suspended mentor profiles cannot be reviewed");
        }
        if (profile.getMentorStatus() != MentorStatus.PENDING) {
            throw new ConflictException("Mentor profile is not pending review");
        }

        UserEntity applicant = users.findById(latest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found"));
        if (applicant.getUserType() != UserType.UNIVERSITY_STUDENT
                && applicant.getUserType() != UserType.WORKING_PROFESSIONAL) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Applicant is no longer eligible to become a mentor");
        }

        var reviewedAt = clock.instant();
        latest.setStatus(decision);
        latest.setReviewedAt(reviewedAt);
        latest.setRejectionReason(rejectionReason);
        profile.setMentorStatus(decision == MentorApplicationStatus.APPROVED
                ? MentorStatus.APPROVED : MentorStatus.REJECTED);
        profile.setAcceptingBookings(false);
        applications.saveAndFlush(latest);
        profiles.saveAndFlush(profile);
        return new MentorApplicationAdminDecision(
                latest.getId(), profile.getId(), latest.getUserId(), latest.getStatus(),
                profile.getMentorStatus(), latest.getAttemptNumber(), profile.isAcceptingBookings(),
                latest.getReviewedAt(), latest.getRejectionReason());
    }

    private MentorApplicationEntity findApplication(UUID applicationId) {
        return applications.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor application not found"));
    }
}
