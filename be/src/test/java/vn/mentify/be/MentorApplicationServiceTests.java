package vn.mentify.be;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mentify.be.modules.mentor.domain.MentorApplicationEntity;
import vn.mentify.be.modules.mentor.domain.MentorApplicationStatus;
import vn.mentify.be.modules.mentor.domain.MentorProfileEntity;
import vn.mentify.be.modules.mentor.domain.MentorStatus;
import vn.mentify.be.modules.mentor.dto.MentorApplicationSubmissionResult;
import vn.mentify.be.modules.mentor.dto.SubmitMentorApplicationCommand;
import vn.mentify.be.modules.mentor.persistence.MentorApplicationRepository;
import vn.mentify.be.modules.mentor.persistence.MentorProfileRepository;
import vn.mentify.be.modules.mentor.service.MentorApplicationServiceImpl;
import vn.mentify.be.modules.user.dto.UserProfileResponse;
import vn.mentify.be.modules.user.persistence.AccountStatus;
import vn.mentify.be.modules.user.persistence.UserRole;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.modules.user.service.UserService;
import vn.mentify.be.shared.api.ApiErrorCode;
import vn.mentify.be.shared.api.ApiException;
import vn.mentify.be.shared.api.ConflictException;
import vn.mentify.be.shared.api.ResourceNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorApplicationServiceTests {

    @Mock
    private UserService userService;

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @Mock
    private MentorApplicationRepository mentorApplicationRepository;

    private Clock fixedClock;
    private MentorApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);
        service = new MentorApplicationServiceImpl(
                userService,
                mentorProfileRepository,
                mentorApplicationRepository,
                fixedClock);
    }

    private UserProfileResponse buildUserProfile(UUID userId, UserType userType) {
        return new UserProfileResponse(
                userId,
                "test@mentify.vn",
                userType,
                UserRole.USER,
                AccountStatus.ACTIVE,
                "Test User",
                "Bio",
                null,
                null,
                null,
                null);
    }

    @Test
    @DisplayName("Successful initial submission by UNIVERSITY_STUDENT")
    void successfulSubmissionByUniversityStudent() {
        UUID userId = UUID.randomUUID();
        when(userService.getProfile(userId)).thenReturn(buildUserProfile(userId, UserType.UNIVERSITY_STUDENT));
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        when(mentorProfileRepository.saveAndFlush(any(MentorProfileEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mentorApplicationRepository.saveAndFlush(any(MentorApplicationEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubmitMentorApplicationCommand command = new SubmitMentorApplicationCommand(
                UUID.randomUUID(),
                "7480",
                "Khoa học máy tính",
                "Senior CS student eager to mentor",
                "Algorithms, Python, AI",
                "https://meet.google.com/test-meet"
        );

        MentorApplicationSubmissionResult result = service.submitInitialApplication(userId, command);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.mentorStatus()).isEqualTo(MentorStatus.PENDING);
        assertThat(result.applicationStatus()).isEqualTo(MentorApplicationStatus.PENDING);
        assertThat(result.attemptNumber()).isEqualTo(1);
        assertThat(result.appliedUserType()).isEqualTo(UserType.UNIVERSITY_STUDENT.name());
        assertThat(result.acceptingBookings()).isFalse();
        assertThat(result.submittedAt()).isEqualTo(Instant.parse("2026-03-01T10:00:00Z"));

        ArgumentCaptor<MentorProfileEntity> profileCaptor = ArgumentCaptor.forClass(MentorProfileEntity.class);
        verify(mentorProfileRepository).saveAndFlush(profileCaptor.capture());
        MentorProfileEntity capturedProfile = profileCaptor.getValue();
        assertThat(capturedProfile.getMentorStatus()).isEqualTo(MentorStatus.PENDING);
        assertThat(capturedProfile.isAcceptingBookings()).isFalse();
        assertThat(capturedProfile.getBio()).isEqualTo("Senior CS student eager to mentor");
        assertThat(capturedProfile.getMajorGroupCode()).isEqualTo("7480");

        ArgumentCaptor<MentorApplicationEntity> appCaptor = ArgumentCaptor.forClass(MentorApplicationEntity.class);
        verify(mentorApplicationRepository).saveAndFlush(appCaptor.capture());
        MentorApplicationEntity capturedApp = appCaptor.getValue();
        assertThat(capturedApp.getStatus()).isEqualTo(MentorApplicationStatus.PENDING);
        assertThat(capturedApp.getAttemptNumber()).isEqualTo(1);
        assertThat(capturedApp.getAppliedUserType()).isEqualTo("UNIVERSITY_STUDENT");
        assertThat(capturedApp.getSubmittedAt()).isEqualTo(Instant.parse("2026-03-01T10:00:00Z"));
    }

    @Test
    @DisplayName("Successful initial submission by WORKING_PROFESSIONAL")
    void successfulSubmissionByWorkingProfessional() {
        UUID userId = UUID.randomUUID();
        when(userService.getProfile(userId)).thenReturn(buildUserProfile(userId, UserType.WORKING_PROFESSIONAL));
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        when(mentorProfileRepository.saveAndFlush(any(MentorProfileEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mentorApplicationRepository.saveAndFlush(any(MentorApplicationEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubmitMentorApplicationCommand command = new SubmitMentorApplicationCommand(
                null,
                null,
                null,
                "Tech Lead at FinTech company",
                "System Architecture, Java",
                null
        );

        MentorApplicationSubmissionResult result = service.submitInitialApplication(userId, command);

        assertThat(result.appliedUserType()).isEqualTo(UserType.WORKING_PROFESSIONAL.name());
        assertThat(result.mentorStatus()).isEqualTo(MentorStatus.PENDING);
        assertThat(result.applicationStatus()).isEqualTo(MentorApplicationStatus.PENDING);
        assertThat(result.acceptingBookings()).isFalse();
    }

    @Test
    @DisplayName("Rejection of SCHOOL_STUDENT as ineligible")
    void rejectsSchoolStudentAsIneligible() {
        UUID userId = UUID.randomUUID();
        when(userService.getProfile(userId)).thenReturn(buildUserProfile(userId, UserType.SCHOOL_STUDENT));

        SubmitMentorApplicationCommand command = new SubmitMentorApplicationCommand(null, null, null, null, null, null);

        assertThatThrownBy(() -> service.submitInitialApplication(userId, command))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getErrorCode()).isEqualTo(ApiErrorCode.FORBIDDEN);
                    assertThat(apiEx.getMessage()).contains("Chỉ sinh viên đại học/cao đẳng hoặc người đi làm");
                });

        verify(mentorProfileRepository, never()).saveAndFlush(any());
        verify(mentorApplicationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Rejection of nonexistent users propagates ResourceNotFoundException")
    void rejectsNonexistentUser() {
        UUID userId = UUID.randomUUID();
        when(userService.getProfile(userId))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        SubmitMentorApplicationCommand command = new SubmitMentorApplicationCommand(null, null, null, null, null, null);

        assertThatThrownBy(() -> service.submitInitialApplication(userId, command))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy người dùng với id: " + userId);

        verify(mentorProfileRepository, never()).saveAndFlush(any());
        verify(mentorApplicationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Rejection of duplicate initial submission when profile is already PENDING")
    void rejectsDuplicateSubmissionWhenProfileAlreadyPending() {
        UUID userId = UUID.randomUUID();
        when(userService.getProfile(userId)).thenReturn(buildUserProfile(userId, UserType.UNIVERSITY_STUDENT));

        MentorProfileEntity existingProfile = new MentorProfileEntity(userId);
        existingProfile.setMentorStatus(MentorStatus.PENDING);
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));

        SubmitMentorApplicationCommand command = new SubmitMentorApplicationCommand(null, null, null, null, null, null);

        assertThatThrownBy(() -> service.submitInitialApplication(userId, command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Hồ sơ mentor đã tồn tại với trạng thái: PENDING");

        verify(mentorProfileRepository, never()).saveAndFlush(any());
        verify(mentorApplicationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Prevention of resubmission through this use case when profile was REJECTED")
    void preventsResubmissionWhenProfileWasRejected() {
        UUID userId = UUID.randomUUID();
        when(userService.getProfile(userId)).thenReturn(buildUserProfile(userId, UserType.WORKING_PROFESSIONAL));

        MentorProfileEntity existingProfile = new MentorProfileEntity(userId);
        existingProfile.setMentorStatus(MentorStatus.REJECTED);
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));

        SubmitMentorApplicationCommand command = new SubmitMentorApplicationCommand(null, null, null, null, null, null);

        assertThatThrownBy(() -> service.submitInitialApplication(userId, command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Hồ sơ mentor đã tồn tại với trạng thái: REJECTED");

        verify(mentorProfileRepository, never()).saveAndFlush(any());
        verify(mentorApplicationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Rejection of duplicate initial submission when previous application exists")
    void rejectsDuplicateWhenPreviousApplicationExists() {
        UUID userId = UUID.randomUUID();
        when(userService.getProfile(userId)).thenReturn(buildUserProfile(userId, UserType.UNIVERSITY_STUDENT));

        MentorProfileEntity existingProfile = new MentorProfileEntity(userId);
        // Force profile to have an ID to simulate existing profile with NOT_APPLIED
        org.springframework.test.util.ReflectionTestUtils.setField(existingProfile, "id", UUID.randomUUID());
        existingProfile.setMentorStatus(MentorStatus.NOT_APPLIED);

        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));

        MentorApplicationEntity existingApp = new MentorApplicationEntity(
                existingProfile.getId(),
                userId,
                UserType.UNIVERSITY_STUDENT.name(),
                1);
        when(mentorApplicationRepository.findFirstByMentorProfileIdOrderByAttemptNumberDesc(existingProfile.getId()))
                .thenReturn(Optional.of(existingApp));

        SubmitMentorApplicationCommand command = new SubmitMentorApplicationCommand(null, null, null, null, null, null);

        assertThatThrownBy(() -> service.submitInitialApplication(userId, command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Người dùng đã có đơn ứng tuyển mentor trước đó");

        verify(mentorProfileRepository, never()).saveAndFlush(any());
        verify(mentorApplicationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Verification that successful submission does not enable bookings")
    void verifiesAcceptingBookingsRemainsFalse() {
        UUID userId = UUID.randomUUID();
        when(userService.getProfile(userId)).thenReturn(buildUserProfile(userId, UserType.UNIVERSITY_STUDENT));
        when(mentorProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        when(mentorProfileRepository.saveAndFlush(any(MentorProfileEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mentorApplicationRepository.saveAndFlush(any(MentorApplicationEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MentorApplicationSubmissionResult result = service.submitInitialApplication(userId, null);

        assertThat(result.acceptingBookings()).isFalse();
    }
}
