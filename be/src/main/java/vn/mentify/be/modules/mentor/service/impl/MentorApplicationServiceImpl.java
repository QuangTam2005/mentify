package vn.mentify.be.modules.mentor.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.mentify.be.modules.mentor.entity.MentorApplicationEntity;
import vn.mentify.be.modules.mentor.entity.MentorApplicationStatus;
import vn.mentify.be.modules.mentor.entity.MentorProfileEntity;
import vn.mentify.be.modules.mentor.entity.MentorStatus;
import vn.mentify.be.modules.mentor.dto.request.SubmitMentorApplicationRequest;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationSubmissionResponse;
import vn.mentify.be.modules.mentor.repository.MentorApplicationRepository;
import vn.mentify.be.modules.mentor.repository.MentorProfileRepository;
import vn.mentify.be.modules.mentor.service.MentorApplicationService;
import vn.mentify.be.modules.user.dto.UserProfileResponse;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.modules.user.service.UserService;
import vn.mentify.be.shared.api.ApiErrorCode;
import vn.mentify.be.shared.api.ApiException;
import vn.mentify.be.shared.api.ConflictException;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class MentorApplicationServiceImpl implements MentorApplicationService {

    private final UserService userService;
    private final MentorProfileRepository mentorProfileRepository;
    private final MentorApplicationRepository mentorApplicationRepository;
    private final Clock clock;

    public MentorApplicationServiceImpl(
            UserService userService,
            MentorProfileRepository mentorProfileRepository,
            MentorApplicationRepository mentorApplicationRepository,
            Clock clock) {
        this.userService = Objects.requireNonNull(userService, "userService must not be null");
        this.mentorProfileRepository = Objects.requireNonNull(mentorProfileRepository, "mentorProfileRepository must not be null");
        this.mentorApplicationRepository = Objects.requireNonNull(mentorApplicationRepository, "mentorApplicationRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public MentorApplicationSubmissionResponse submitInitialApplication(UUID userId, SubmitMentorApplicationRequest request) {
        if (userId == null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "userId không được để trống");
        }
        SubmitMentorApplicationRequest safeRequest = request != null
                ? request
                : new SubmitMentorApplicationRequest(null, null, null, null, null, null);

        // 1. Verify user exists and check eligibility via public User boundary
        UserProfileResponse userProfile = userService.getProfile(userId);
        UserType userType = userProfile.userType();

        if (userType != UserType.UNIVERSITY_STUDENT && userType != UserType.WORKING_PROFESSIONAL) {
            throw new ApiException(
                    ApiErrorCode.FORBIDDEN,
                    "Chỉ sinh viên đại học/cao đẳng hoặc người đi làm mới có thể đăng ký làm mentor");
        }

        // 2. Check existing mentor profile and eligibility for initial submission
        MentorProfileEntity profile = mentorProfileRepository.findByUserId(userId).orElse(null);
        if (profile != null) {
            if (profile.getMentorStatus() != MentorStatus.NOT_APPLIED) {
                throw new ConflictException("Hồ sơ mentor đã tồn tại với trạng thái: " + profile.getMentorStatus());
            }
        } else {
            profile = new MentorProfileEntity(userId);
        }

        // 3. Check duplicate submission: must not have any prior application attempts
        if (profile.getId() != null) {
            boolean hasExistingApplications = mentorApplicationRepository
                    .findFirstByMentorProfileIdOrderByAttemptNumberDesc(profile.getId())
                    .isPresent();
            if (hasExistingApplications) {
                throw new ConflictException("Người dùng đã có đơn ứng tuyển mentor trước đó");
            }
        }

        // 4. Update profile fields from request and set status to PENDING
        if (safeRequest.schoolId() != null) {
            profile.setSchoolId(safeRequest.schoolId());
        }
        if (safeRequest.majorGroupCode() != null) {
            profile.setMajorGroupCode(safeRequest.majorGroupCode().trim());
        }
        if (safeRequest.customMajorName() != null) {
            profile.setCustomMajorName(safeRequest.customMajorName().trim());
        }
        if (safeRequest.bio() != null) {
            profile.setBio(safeRequest.bio().trim());
        }
        if (safeRequest.expertiseAreas() != null) {
            profile.setExpertiseAreas(safeRequest.expertiseAreas().trim());
        }
        if (safeRequest.defaultMeetingLink() != null) {
            profile.setDefaultMeetingLink(safeRequest.defaultMeetingLink().trim());
        }

        profile.setMentorStatus(MentorStatus.PENDING);
        profile.setAcceptingBookings(false);

        MentorProfileEntity savedProfile = mentorProfileRepository.saveAndFlush(profile);

        // 5. Create first mentor application record
        Instant now = clock.instant();
        MentorApplicationEntity application = new MentorApplicationEntity(
                savedProfile.getId(),
                userId,
                userType.name(),
                1);
        application.setStatus(MentorApplicationStatus.PENDING);
        application.setSubmittedAt(now);
        application.setSchoolId(savedProfile.getSchoolId());
        application.setMajorGroupCode(savedProfile.getMajorGroupCode());
        application.setCustomMajorName(savedProfile.getCustomMajorName());
        application.setBio(savedProfile.getBio());

        MentorApplicationEntity savedApplication = mentorApplicationRepository.saveAndFlush(application);

        return new MentorApplicationSubmissionResponse(
                savedProfile.getId(),
                savedApplication.getId(),
                userId,
                savedProfile.getMentorStatus(),
                savedApplication.getStatus(),
                savedApplication.getAttemptNumber(),
                savedApplication.getAppliedUserType(),
                savedProfile.isAcceptingBookings(),
                savedApplication.getSubmittedAt()
        );
    }
}
