package vn.mentify.be;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.modules.masterdata.domain.MajorGroupEntity;
import vn.mentify.be.modules.masterdata.domain.SchoolEntity;
import vn.mentify.be.modules.masterdata.persistence.MajorGroupRepository;
import vn.mentify.be.modules.masterdata.persistence.SchoolRepository;
import vn.mentify.be.modules.mentor.entity.MentorApplicationEntity;
import vn.mentify.be.modules.mentor.entity.MentorApplicationStatus;
import vn.mentify.be.modules.mentor.entity.MentorProfileEntity;
import vn.mentify.be.modules.mentor.entity.MentorStatus;
import vn.mentify.be.modules.mentor.repository.MentorApplicationRepository;
import vn.mentify.be.modules.mentor.repository.MentorProfileRepository;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.modules.user.persistence.UserRole;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.support.AbstractPostgresIntegrationTest;
import vn.mentify.be.support.TestFixtures;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MentorPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private MajorGroupRepository majorGroupRepository;

    @Autowired
    private MentorProfileRepository mentorProfileRepository;

    @Autowired
    private MentorApplicationRepository mentorApplicationRepository;

    @Autowired
    private vn.mentify.be.modules.mentor.service.MentorApplicationService mentorApplicationService;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanData() {
        mentorApplicationRepository.deleteAll();
        mentorProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Flyway V8 migration created tables, columns, indexes, and constraints successfully")
    void flywayMigrationV8_createdExpectedTablesAndConstraints() {
        Integer profileTableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'mentor_profiles'",
                Integer.class);
        assertThat(profileTableCount).isEqualTo(1);

        Integer applicationTableCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'mentor_applications'",
                Integer.class);
        assertThat(applicationTableCount).isEqualTo(1);

        Integer profileIndexes = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE tablename = 'mentor_profiles'",
                Integer.class);
        assertThat(profileIndexes).isGreaterThanOrEqualTo(4);

        Integer applicationIndexes = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE tablename = 'mentor_applications'",
                Integer.class);
        assertThat(applicationIndexes).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("MentorProfile persistence works with scalar references and defaults")
    void persistsAndRetrievesMentorProfile() {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "mentor-owner@mentify.vn",
                UserType.WORKING_PROFESSIONAL,
                UserRole.USER);

        SchoolEntity school = schoolRepository.findAll().stream().findFirst().orElse(null);
        MajorGroupEntity majorGroup = majorGroupRepository.findAll().stream().findFirst().orElse(null);

        MentorProfileEntity profile = new MentorProfileEntity(user.getId());
        profile.setBio("Senior Software Engineer with 8 years of experience");
        profile.setExpertiseAreas("Java, Spring Boot, PostgreSQL, Microservices");
        profile.setDefaultMeetingLink("https://meet.google.com/abc-defg-hij");
        if (school != null) {
            profile.setSchoolId(school.getId());
        }
        if (majorGroup != null) {
            profile.setMajorGroupCode(majorGroup.getCode());
        }

        MentorProfileEntity saved = mentorProfileRepository.saveAndFlush(profile);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getMentorStatus()).isEqualTo(MentorStatus.NOT_APPLIED);
        assertThat(saved.isAcceptingBookings()).isFalse();

        Optional<MentorProfileEntity> found = mentorProfileRepository.findByUserId(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getBio()).isEqualTo("Senior Software Engineer with 8 years of experience");
        assertThat(found.get().getDefaultMeetingLink()).isEqualTo("https://meet.google.com/abc-defg-hij");
        assertThat(mentorProfileRepository.existsByUserId(user.getId())).isTrue();
    }

    @Test
    @DisplayName("Unique constraint uk_mentor_profiles_user_id enforces 1:1 user to mentor profile")
    void enforcesUniqueMentorProfilePerUser() {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "mentor-unique@mentify.vn",
                UserType.UNIVERSITY_STUDENT,
                UserRole.USER);

        MentorProfileEntity profile1 = new MentorProfileEntity(user.getId());
        mentorProfileRepository.saveAndFlush(profile1);

        MentorProfileEntity profile2 = new MentorProfileEntity(user.getId());
        assertThatThrownBy(() -> mentorProfileRepository.saveAndFlush(profile2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Foreign key fk_mentor_profiles_user enforces valid user ID")
    void enforcesForeignKeyConstraintOnUserId() {
        UUID nonExistentUserId = UUID.randomUUID();
        MentorProfileEntity profile = new MentorProfileEntity(nonExistentUserId);

        assertThatThrownBy(() -> mentorProfileRepository.saveAndFlush(profile))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("MentorApplication persistence and retrieval by status and user")
    void persistsAndRetrievesMentorApplication() {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "applicant@mentify.vn",
                UserType.UNIVERSITY_STUDENT,
                UserRole.USER);

        MentorProfileEntity profile = mentorProfileRepository.saveAndFlush(new MentorProfileEntity(user.getId()));

        MentorApplicationEntity application = new MentorApplicationEntity(
                profile.getId(),
                user.getId(),
                UserType.UNIVERSITY_STUDENT.name(),
                1);
        application.setBio("Computer Science senior passionate about AI");

        MentorApplicationEntity savedApp = mentorApplicationRepository.saveAndFlush(application);

        assertThat(savedApp.getId()).isNotNull();
        assertThat(savedApp.getStatus()).isEqualTo(MentorApplicationStatus.PENDING);
        assertThat(savedApp.getAttemptNumber()).isEqualTo(1);
        assertThat(savedApp.getSubmittedAt()).isNotNull();
        assertThat(savedApp.getReviewedAt()).isNull();

        List<MentorApplicationEntity> userApps = mentorApplicationRepository.findByUserIdOrderBySubmittedAtDesc(user.getId());
        assertThat(userApps).hasSize(1);
        assertThat(userApps.getFirst().getId()).isEqualTo(savedApp.getId());

        List<MentorApplicationEntity> pendingApps = mentorApplicationRepository.findByStatus(MentorApplicationStatus.PENDING);
        assertThat(pendingApps).isNotEmpty();
    }

    @Test
    @DisplayName("Supports sequential application attempts history and retrieval order")
    void supportsSequentialApplicationAttempts() {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "resubmit-applicant@mentify.vn",
                UserType.WORKING_PROFESSIONAL,
                UserRole.USER);

        MentorProfileEntity profile = mentorProfileRepository.saveAndFlush(new MentorProfileEntity(user.getId()));

        // Attempt 1: Submitted and rejected
        MentorApplicationEntity app1 = new MentorApplicationEntity(
                profile.getId(),
                user.getId(),
                UserType.WORKING_PROFESSIONAL.name(),
                1);
        app1.setStatus(MentorApplicationStatus.REJECTED);
        app1.setRejectionReason("Need clearer proof of graduation or professional certification");
        mentorApplicationRepository.saveAndFlush(app1);

        // Attempt 2: Resubmitted and pending
        MentorApplicationEntity app2 = new MentorApplicationEntity(
                profile.getId(),
                user.getId(),
                UserType.WORKING_PROFESSIONAL.name(),
                2);
        app2.setStatus(MentorApplicationStatus.PENDING);
        mentorApplicationRepository.saveAndFlush(app2);

        List<MentorApplicationEntity> history = mentorApplicationRepository
                .findByMentorProfileIdOrderByAttemptNumberDesc(profile.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getAttemptNumber()).isEqualTo(2);
        assertThat(history.get(0).getStatus()).isEqualTo(MentorApplicationStatus.PENDING);
        assertThat(history.get(1).getAttemptNumber()).isEqualTo(1);
        assertThat(history.get(1).getStatus()).isEqualTo(MentorApplicationStatus.REJECTED);

        Optional<MentorApplicationEntity> latest = mentorApplicationRepository
                .findFirstByMentorProfileIdOrderByAttemptNumberDesc(profile.getId());
        assertThat(latest).isPresent();
        assertThat(latest.get().getAttemptNumber()).isEqualTo(2);

        Optional<MentorApplicationEntity> specificAttempt = mentorApplicationRepository
                .findByMentorProfileIdAndAttemptNumber(profile.getId(), 1);
        assertThat(specificAttempt).isPresent();
        assertThat(specificAttempt.get().getRejectionReason()).contains("proof of graduation");
    }

    @Test
    @DisplayName("Unique constraint uk_mentor_applications_attempt prevents duplicate attempt number for same profile")
    void enforcesUniqueAttemptNumberPerMentorProfile() {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "attempt-conflict@mentify.vn",
                UserType.WORKING_PROFESSIONAL,
                UserRole.USER);

        MentorProfileEntity profile = mentorProfileRepository.saveAndFlush(new MentorProfileEntity(user.getId()));

        MentorApplicationEntity app1 = new MentorApplicationEntity(
                profile.getId(),
                user.getId(),
                UserType.WORKING_PROFESSIONAL.name(),
                1);
        mentorApplicationRepository.saveAndFlush(app1);

        MentorApplicationEntity duplicateAttempt = new MentorApplicationEntity(
                profile.getId(),
                user.getId(),
                UserType.WORKING_PROFESSIONAL.name(),
                1);
        assertThatThrownBy(() -> mentorApplicationRepository.saveAndFlush(duplicateAttempt))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Cascade deletion removes mentor profile and applications when user is deleted")
    void cascadesDeletionOnUserDelete() {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "cascade-user@mentify.vn",
                UserType.WORKING_PROFESSIONAL,
                UserRole.USER);

        MentorProfileEntity profile = mentorProfileRepository.saveAndFlush(new MentorProfileEntity(user.getId()));

        MentorApplicationEntity app = new MentorApplicationEntity(
                profile.getId(),
                user.getId(),
                UserType.WORKING_PROFESSIONAL.name(),
                1);
        mentorApplicationRepository.saveAndFlush(app);

        assertThat(mentorProfileRepository.count()).isEqualTo(1);
        assertThat(mentorApplicationRepository.count()).isEqualTo(1);

        // Delete user directly via userRepository
        userRepository.deleteById(user.getId());
        userRepository.flush();

        assertThat(mentorProfileRepository.count()).isZero();
        assertThat(mentorApplicationRepository.count()).isZero();
    }

    @Test
    @DisplayName("Service: Successful initial submission persists profile, single application with attempt 1, PENDING status, and acceptingBookings=false")
    void service_successfulInitialSubmissionPersistsExpectedState() {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "submit-success@mentify.vn",
                UserType.UNIVERSITY_STUDENT,
                UserRole.USER);

        SchoolEntity school = schoolRepository.findAll().stream().findFirst().orElse(null);
        MajorGroupEntity majorGroup = majorGroupRepository.findAll().stream().findFirst().orElse(null);

        var command = new vn.mentify.be.modules.mentor.dto.request.SubmitMentorApplicationRequest(
                school != null ? school.getId() : null,
                majorGroup != null ? majorGroup.getCode() : null,
                "Kỹ thuật phần mềm",
                "Sinh viên năm 4 đam mê chia sẻ định hướng",
                "Java, Spring, Clean Architecture",
                "https://meet.google.com/test-meet"
        );

        var result = mentorApplicationService.submitInitialApplication(user.getId(), command);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.mentorStatus()).isEqualTo(MentorStatus.PENDING);
        assertThat(result.applicationStatus()).isEqualTo(MentorApplicationStatus.PENDING);
        assertThat(result.attemptNumber()).isEqualTo(1);
        assertThat(result.appliedUserType()).isEqualTo("UNIVERSITY_STUDENT");
        assertThat(result.acceptingBookings()).isFalse();

        // Verify DB persistence
        MentorProfileEntity profile = mentorProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(profile.getMentorStatus()).isEqualTo(MentorStatus.PENDING);
        assertThat(profile.isAcceptingBookings()).isFalse();
        assertThat(profile.getBio()).isEqualTo("Sinh viên năm 4 đam mê chia sẻ định hướng");
        assertThat(profile.getDefaultMeetingLink()).isEqualTo("https://meet.google.com/test-meet");

        List<MentorApplicationEntity> apps = mentorApplicationRepository
                .findByMentorProfileIdOrderByAttemptNumberDesc(profile.getId());
        assertThat(apps).hasSize(1);
        MentorApplicationEntity app = apps.getFirst();
        assertThat(app.getStatus()).isEqualTo(MentorApplicationStatus.PENDING);
        assertThat(app.getAttemptNumber()).isEqualTo(1);
        assertThat(app.getAppliedUserType()).isEqualTo("UNIVERSITY_STUDENT");
        assertThat(app.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("Service: Ineligible SCHOOL_STUDENT user creates no mentor profile or application")
    void service_ineligibleUserCreatesNoRecords() {
        UserEntity student = TestFixtures.saveUser(
                userRepository,
                "high-schooler@mentify.vn",
                UserType.SCHOOL_STUDENT,
                UserRole.USER);

        var command = new vn.mentify.be.modules.mentor.dto.request.SubmitMentorApplicationRequest(
                null, null, null, "Học sinh THPT muốn làm mentor", null, null);

        assertThatThrownBy(() -> mentorApplicationService.submitInitialApplication(student.getId(), command))
                .isInstanceOf(vn.mentify.be.shared.api.ApiException.class)
                .satisfies(ex -> {
                    var apiEx = (vn.mentify.be.shared.api.ApiException) ex;
                    assertThat(apiEx.getErrorCode()).isEqualTo(vn.mentify.be.shared.api.ApiErrorCode.FORBIDDEN);
                });

        assertThat(mentorProfileRepository.findByUserId(student.getId())).isEmpty();
        assertThat(mentorApplicationRepository.count()).isZero();
    }

    @Test
    @DisplayName("Service: Duplicate submission does not create additional records or attempts")
    void service_duplicateSubmissionDoesNotCreateAdditionalRecords() {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "dup-submitter@mentify.vn",
                UserType.WORKING_PROFESSIONAL,
                UserRole.USER);

        var command = new vn.mentify.be.modules.mentor.dto.request.SubmitMentorApplicationRequest(
                null, null, null, "Chuyên viên phần mềm", "Backend", null);

        mentorApplicationService.submitInitialApplication(user.getId(), command);
        assertThat(mentorProfileRepository.count()).isEqualTo(1);
        assertThat(mentorApplicationRepository.count()).isEqualTo(1);

        // Second submission attempt must fail with ConflictException
        assertThatThrownBy(() -> mentorApplicationService.submitInitialApplication(user.getId(), command))
                .isInstanceOf(vn.mentify.be.shared.api.ConflictException.class);

        assertThat(mentorProfileRepository.count()).isEqualTo(1);
        assertThat(mentorApplicationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Service: Transaction rollback prevents partial persistence on failure")
    void service_transactionRollbackPreventsPartialPersistence() {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "rollback-test@mentify.vn",
                UserType.WORKING_PROFESSIONAL,
                UserRole.USER);

        var command = new vn.mentify.be.modules.mentor.dto.request.SubmitMentorApplicationRequest(
                null, null, null, "Rollback candidate", null, null);

        var txTemplate = new org.springframework.transaction.support.TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> txTemplate.executeWithoutResult(status -> {
            mentorApplicationService.submitInitialApplication(user.getId(), command);
            throw new IllegalStateException("Simulated business transaction failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(mentorProfileRepository.findByUserId(user.getId())).isEmpty();
        assertThat(mentorApplicationRepository.count()).isZero();
    }
}
