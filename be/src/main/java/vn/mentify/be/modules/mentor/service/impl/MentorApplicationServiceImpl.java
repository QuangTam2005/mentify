package vn.mentify.be.modules.mentor.service.impl;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.mentify.be.modules.mentor.entity.MentorApplicationEntity;
import vn.mentify.be.modules.mentor.entity.MentorApplicationStatus;
import vn.mentify.be.modules.mentor.entity.MentorProfileEntity;
import vn.mentify.be.modules.mentor.entity.MentorStatus;
import vn.mentify.be.modules.mentor.dto.request.SubmitMentorApplicationRequest;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationSubmissionResponse;
import vn.mentify.be.modules.mentor.dto.response.CurrentMentorApplicationResponse;
import vn.mentify.be.modules.mentor.repository.MentorApplicationRepository;
import vn.mentify.be.modules.mentor.repository.MentorProfileRepository;
import vn.mentify.be.modules.mentor.service.MentorApplicationService;
import vn.mentify.be.modules.user.dto.UserProfileResponse;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.modules.user.service.UserService;
import vn.mentify.be.shared.api.ApiErrorCode;
import vn.mentify.be.shared.api.ApiException;
import vn.mentify.be.shared.api.ConflictException;
import vn.mentify.be.shared.api.ResourceNotFoundException;

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
        applyRequestToProfile(profile, safeRequest);

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

    @Override
    @Transactional
    public MentorApplicationSubmissionResponse resubmitApplication(UUID userId, SubmitMentorApplicationRequest request) {
        if (userId == null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "userId khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng");
        }

        MentorProfileEntity profile;
        try {
            profile = mentorProfileRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y há»“ sÆ¡ mentor"));
        } catch (PessimisticLockingFailureException exception) {
            throw new ConflictException("YÃªu cáº§u ná»™p láº¡i Ä‘Æ¡n mentor Ä‘ang Ä‘Æ°á»£c xá»­ lÃ½");
        }

        if (profile.getMentorStatus() == MentorStatus.SUSPENDED) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "HÃ  sÆ¡ mentor Ä‘ang bá»‹ táº¡m Ä‘Ã¬nh chá»‰");
        }

        MentorApplicationEntity latestApplication = mentorApplicationRepository
                .findFirstByMentorProfileIdOrderByAttemptNumberDesc(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n á»©ng tuyá»ƒn mentor Ä‘á»ƒ ná»™p láº¡i"));
        if (latestApplication.getStatus() != MentorApplicationStatus.REJECTED) {
            throw new ConflictException("Chá»‰ cÃ³ thá»ƒ ná»™p láº¡i Ä‘Æ¡n mentor bá»‹ tá»« chá»‘i");
        }

        UserProfileResponse userProfile = userService.getProfile(userId);
        UserType userType = userProfile.userType();
        if (userType != UserType.UNIVERSITY_STUDENT && userType != UserType.WORKING_PROFESSIONAL) {
            throw new ApiException(
                    ApiErrorCode.FORBIDDEN,
                    "Chá»‰ sinh viÃªn Ä‘áº¡i há»c/cao Ä‘áº³ng hoáº·c ngÆ°á»i Ä‘i lÃ m má»›i cÃ³ thá»ƒ Ä‘Äƒng kÃ½ lÃ m mentor");
        }

        SubmitMentorApplicationRequest safeRequest = request != null
                ? request
                : new SubmitMentorApplicationRequest(null, null, null, null, null, null);
        applyRequestToProfile(profile, safeRequest);
        profile.setMentorStatus(MentorStatus.PENDING);
        profile.setAcceptingBookings(false);
        mentorProfileRepository.save(profile);

        Instant submittedAt = clock.instant();
        MentorApplicationEntity nextApplication = new MentorApplicationEntity(
                profile.getId(), userId, userType.name(), latestApplication.getAttemptNumber() + 1);
        nextApplication.setStatus(MentorApplicationStatus.PENDING);
        nextApplication.setSubmittedAt(submittedAt);
        nextApplication.setSchoolId(profile.getSchoolId());
        nextApplication.setMajorGroupCode(profile.getMajorGroupCode());
        nextApplication.setCustomMajorName(profile.getCustomMajorName());
        nextApplication.setBio(profile.getBio());

        MentorApplicationEntity savedApplication;
        try {
            savedApplication = mentorApplicationRepository.saveAndFlush(nextApplication);
        } catch (DataIntegrityViolationException exception) {
            if (isAttemptNumberConflict(exception)) {
                throw new ConflictException("ÄÆ¡n mentor Ä‘Ã£ Ä‘Æ°á»£c ná»™p láº¡i bá»Ÿi má»™t yÃªu cáº§u khÃ¡c");
            }
            throw exception;
        }

        return new MentorApplicationSubmissionResponse(
                profile.getId(),
                savedApplication.getId(),
                userId,
                profile.getMentorStatus(),
                savedApplication.getStatus(),
                savedApplication.getAttemptNumber(),
                savedApplication.getAppliedUserType(),
                profile.isAcceptingBookings(),
                savedApplication.getSubmittedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentMentorApplicationResponse getCurrentApplication(UUID userId) {
        MentorApplicationEntity application = mentorApplicationRepository
                .findFirstByUserIdOrderByAttemptNumberDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n á»©ng tuyá»ƒn mentor"));
        MentorProfileEntity profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y há»“ sÆ¡ mentor"));

        return new CurrentMentorApplicationResponse(
                profile.getId(),
                application.getId(),
                profile.getMentorStatus(),
                application.getStatus(),
                application.getAttemptNumber(),
                application.getAppliedUserType(),
                application.getSchoolId(),
                application.getMajorGroupCode(),
                application.getCustomMajorName(),
                application.getBio(),
                profile.isAcceptingBookings(),
                application.getSubmittedAt()
        );
    }

    private void applyRequestToProfile(MentorProfileEntity profile, SubmitMentorApplicationRequest request) {
        if (request.schoolId() != null) {
            profile.setSchoolId(request.schoolId());
        }
        if (request.majorGroupCode() != null) {
            profile.setMajorGroupCode(request.majorGroupCode().trim());
        }
        if (request.customMajorName() != null) {
            profile.setCustomMajorName(request.customMajorName().trim());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio().trim());
        }
        if (request.expertiseAreas() != null) {
            profile.setExpertiseAreas(request.expertiseAreas().trim());
        }
        if (request.defaultMeetingLink() != null) {
            profile.setDefaultMeetingLink(request.defaultMeetingLink().trim());
        }
    }

    private boolean isAttemptNumberConflict(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && "uk_mentor_applications_attempt".equals(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
