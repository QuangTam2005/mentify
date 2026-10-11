package vn.mentify.be;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.modules.auth.dto.DevLoginRequest;
import vn.mentify.be.modules.masterdata.domain.MajorGroupEntity;
import vn.mentify.be.modules.masterdata.domain.SchoolEntity;
import vn.mentify.be.modules.masterdata.persistence.MajorGroupRepository;
import vn.mentify.be.modules.masterdata.persistence.SchoolRepository;
import vn.mentify.be.modules.mentor.dto.request.SubmitMentorApplicationRequest;
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
import vn.mentify.be.support.TestFixtures;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MentorApplicationContractTests {

    private static final String ISO_OFFSET_REGEX = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?\\+07:00$";
    private static final KeyPair JWT_KEY_PAIR = generateJwtKeyPair();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mentify")
            .withUsername("mentify")
            .withPassword("mentify_test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private ObjectMapper objectMapper;

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

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.security.jwt.enabled", () -> true);
        registry.add("app.security.jwt.private-key", () -> toPem("PRIVATE KEY", JWT_KEY_PAIR.getPrivate().getEncoded()));
        registry.add("app.security.jwt.public-key", () -> toPem("PUBLIC KEY", JWT_KEY_PAIR.getPublic().getEncoded()));
    }

    private static KeyPair generateJwtKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String toPem(String type, byte[] encoded) {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----";
    }

    @BeforeEach
    void cleanData() {
        mentorApplicationRepository.deleteAll();
        mentorProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Unauthenticated request to POST /api/v1/mentor/applications returns 401 UNAUTHENTICATED")
    void unauthenticatedRequest_returns401() throws Exception {
        SubmitMentorApplicationRequest requestDto = new SubmitMentorApplicationRequest(
                null, null, null, "Bio", "Expertise", null);

        mockMvc.perform(post("/api/v1/mentor/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(ISO_OFFSET_REGEX)));
    }

    @Test
    @DisplayName("Legacy mentor application paths are unmapped and do not redirect")
    void legacyPaths_areUnmappedAndDoNotRedirect() throws Exception {
        assertThat(requestMappingHandlerMapping.getHandler(
                new MockHttpServletRequest("POST", "/api/mentor/applications"))).isNull();
        assertThat(requestMappingHandlerMapping.getHandler(
                new MockHttpServletRequest("GET", "/api/mentor/applications/me"))).isNull();

        mockMvc.perform(post("/api/mentor/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitMentorApplicationRequest(null, null, null, "Bio", "Expertise", null))))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(result.getResponse().getStatus() / 100).isNotEqualTo(3));

        mockMvc.perform(get("/api/mentor/applications/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(result.getResponse().getStatus() / 100).isNotEqualTo(3));
    }

    @Test
    @DisplayName("Eligible UNIVERSITY_STUDENT with authenticated session receives 201 Created and success envelope")
    void eligibleUniversityStudent_submitsApplication_returns201Created() throws Exception {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "student-mentor@mentify.vn",
                UserType.UNIVERSITY_STUDENT,
                UserRole.USER);

        SchoolEntity school = schoolRepository.findAll().stream().findFirst().orElse(null);
        MajorGroupEntity majorGroup = majorGroupRepository.findAll().stream().findFirst().orElse(null);

        SubmitMentorApplicationRequest requestDto = new SubmitMentorApplicationRequest(
                school != null ? school.getId() : null,
                majorGroup != null ? majorGroup.getCode() : null,
                "Khoa học máy tính",
                "Sinh viên năm 4 muốn chia sẻ kinh nghiệm đồ án",
                "Java, Spring Boot, Microservices",
                "https://meet.google.com/test-room"
        );

        mockMvc.perform(post("/api/v1/mentor/applications")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(ISO_OFFSET_REGEX)))
                .andExpect(jsonPath("$.data.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.data.mentorProfileId").isNotEmpty())
                .andExpect(jsonPath("$.data.mentorApplicationId").isNotEmpty())
                .andExpect(jsonPath("$.data.mentorStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.applicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.attemptNumber").value(1))
                .andExpect(jsonPath("$.data.appliedUserType").value("UNIVERSITY_STUDENT"))
                .andExpect(jsonPath("$.data.acceptingBookings").value(false))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());

        // Verify database persistence
        MentorProfileEntity profile = mentorProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(profile.getMentorStatus()).isEqualTo(MentorStatus.PENDING);
        assertThat(profile.isAcceptingBookings()).isFalse();
        assertThat(profile.getBio()).isEqualTo("Sinh viên năm 4 muốn chia sẻ kinh nghiệm đồ án");

        List<MentorApplicationEntity> applications =
                mentorApplicationRepository.findByMentorProfileIdOrderByAttemptNumberDesc(profile.getId());
        assertThat(applications).hasSize(1);
        assertThat(applications.getFirst().getStatus()).isEqualTo(MentorApplicationStatus.PENDING);
        assertThat(applications.getFirst().getAttemptNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Eligible WORKING_PROFESSIONAL using real JWT token receives 201 Created")
    void eligibleWorkingProfessional_withRealJwtToken_returns201Created() throws Exception {
        // 1. Authenticate via dev-login to obtain real Bearer JWT
        DevLoginRequest loginRequest = new DevLoginRequest(
                "pro-mentor@mentify.vn",
                UserType.WORKING_PROFESSIONAL,
                UserRole.USER,
                "Nguyễn Văn Chuyên Gia"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String jwtToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").path("value").asText();
        assertThat(jwtToken).isNotEmpty();

        // 2. Submit initial application using Bearer token
        SubmitMentorApplicationRequest requestDto = new SubmitMentorApplicationRequest(
                null,
                null,
                null,
                "Senior Backend Engineer tại Tiki",
                "System Design, Distributed Systems",
                "https://meet.google.com/pro-room"
        );

        mockMvc.perform(post("/api/v1/mentor/applications")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appliedUserType").value("WORKING_PROFESSIONAL"))
                .andExpect(jsonPath("$.data.mentorStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.applicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.attemptNumber").value(1))
                .andExpect(jsonPath("$.data.acceptingBookings").value(false));
    }

    @Test
    @DisplayName("Ineligible SCHOOL_STUDENT applicant receives 403 FORBIDDEN")
    void ineligibleSchoolStudent_returns403Forbidden() throws Exception {
        UserEntity student = TestFixtures.saveUser(
                userRepository,
                "pupil@mentify.vn",
                UserType.SCHOOL_STUDENT,
                UserRole.USER);

        SubmitMentorApplicationRequest requestDto = new SubmitMentorApplicationRequest(
                null, null, null, "Học sinh lớp 12", "Math, Physics", null);

        mockMvc.perform(post("/api/v1/mentor/applications")
                        .with(SecurityMockMvcRequestPostProcessors.user(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(ISO_OFFSET_REGEX)));

        assertThat(mentorProfileRepository.findByUserId(student.getId())).isEmpty();
        assertThat(mentorApplicationRepository.count()).isZero();
    }

    @Test
    @DisplayName("Duplicate initial submission receives 409 CONFLICT")
    void duplicateSubmission_returns409Conflict() throws Exception {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "dup-mentor@mentify.vn",
                UserType.UNIVERSITY_STUDENT,
                UserRole.USER);

        SubmitMentorApplicationRequest requestDto = new SubmitMentorApplicationRequest(
                null, null, null, "First attempt", "Software", null);

        // First submission succeeds: 201
        mockMvc.perform(post("/api/v1/mentor/applications")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // Duplicate submission fails: 409
        mockMvc.perform(post("/api/v1/mentor/applications")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(ISO_OFFSET_REGEX)));

        assertThat(mentorProfileRepository.count()).isEqualTo(1);
        assertThat(mentorApplicationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Malformed JSON returns standard 400 VALIDATION_ERROR")
    void malformedJson_returns400ValidationError() throws Exception {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "json-error@mentify.vn",
                UserType.UNIVERSITY_STUDENT,
                UserRole.USER);

        mockMvc.perform(post("/api/v1/mentor/applications")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json-body"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Bean Validation violation exceeding maximum length returns 400 VALIDATION_ERROR")
    void validationError_exceedingMaxLength_returns400() throws Exception {
        UserEntity user = TestFixtures.saveUser(
                userRepository,
                "validation-user@mentify.vn",
                UserType.UNIVERSITY_STUDENT,
                UserRole.USER);

        String oversizedBio = "x".repeat(1001);
        SubmitMentorApplicationRequest badRequest = new SubmitMentorApplicationRequest(
                null, "7480", null, oversizedBio, null, null);

        mockMvc.perform(post("/api/v1/mentor/applications")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("bio"));
    }

    @Test
    @DisplayName("Caller cannot spoof identity via body: identity is strictly resolved from security principal")
    void callerCannotSpoofIdentityViaBody() throws Exception {
        UserEntity victim = TestFixtures.saveUser(
                userRepository,
                "victim@mentify.vn",
                UserType.UNIVERSITY_STUDENT,
                UserRole.USER);

        UserEntity attacker = TestFixtures.saveUser(
                userRepository,
                "attacker@mentify.vn",
                UserType.UNIVERSITY_STUDENT,
                UserRole.USER);

        // Submit body containing attacker's desired info
        SubmitMentorApplicationRequest requestDto = new SubmitMentorApplicationRequest(
                null, null, null, "Attacker submission", "Hacking", null);

        // Authenticated as attacker
        mockMvc.perform(post("/api/v1/mentor/applications")
                        .with(SecurityMockMvcRequestPostProcessors.user(attacker.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(attacker.getId().toString()));

        // Confirm victim has no mentor profile created
        assertThat(mentorProfileRepository.findByUserId(victim.getId())).isEmpty();
        assertThat(mentorProfileRepository.findByUserId(attacker.getId())).isPresent();
    }

    @Test
    @DisplayName("Current application endpoint requires authentication")
    void currentApplication_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/mentor/applications/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Current application endpoint returns 404 when applicant has no application")
    void currentApplication_missing_returns404() throws Exception {
        UserEntity user = TestFixtures.saveUser(userRepository, "no-application@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);

        mockMvc.perform(get("/api/v1/mentor/applications/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Applicant receives their latest persisted application and no internal review fields")
    void currentApplication_returnsLatestOwnedAttemptAndApplicantFields() throws Exception {
        UserEntity user = TestFixtures.saveUser(userRepository, "current-application@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity profile = mentorProfileRepository.saveAndFlush(new MentorProfileEntity(user.getId()));
        profile.setMentorStatus(MentorStatus.REJECTED);
        mentorProfileRepository.saveAndFlush(profile);

        MentorApplicationEntity older = new MentorApplicationEntity(profile.getId(), user.getId(), UserType.UNIVERSITY_STUDENT.name(), 1);
        older.setStatus(MentorApplicationStatus.REJECTED);
        older.setBio("Previous application");
        older.setRejectionReason("Internal review detail");
        mentorApplicationRepository.saveAndFlush(older);

        MentorApplicationEntity latest = new MentorApplicationEntity(profile.getId(), user.getId(), UserType.UNIVERSITY_STUDENT.name(), 2);
        latest.setStatus(MentorApplicationStatus.PENDING);
        SchoolEntity school = schoolRepository.findAll().stream().findFirst().orElseThrow();
        latest.setSchoolId(school.getId());
        latest.setCustomMajorName("Computer Science");
        latest.setBio("Current application");
        latest.setRejectionReason("Confidential review detail");
        mentorApplicationRepository.saveAndFlush(latest);

        mockMvc.perform(get("/api/v1/mentor/applications/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mentorApplicationId").value(latest.getId().toString()))
                .andExpect(jsonPath("$.data.mentorStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.applicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.attemptNumber").value(2))
                .andExpect(jsonPath("$.data.appliedUserType").value("UNIVERSITY_STUDENT"))
                .andExpect(jsonPath("$.data.schoolId").value(school.getId().toString()))
                .andExpect(jsonPath("$.data.customMajorName").value("Computer Science"))
                .andExpect(jsonPath("$.data.bio").value("Current application"))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.rejectionReason").doesNotExist())
                .andExpect(jsonPath("$.data.reviewedAt").doesNotExist())
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @Test
    @DisplayName("Current application is scoped to authenticated user")
    void currentApplication_userCannotReadAnotherUsersApplication() throws Exception {
        UserEntity userA = TestFixtures.saveUser(userRepository, "applicant-a@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        UserEntity userB = TestFixtures.saveUser(userRepository, "applicant-b@mentify.vn", UserType.WORKING_PROFESSIONAL, UserRole.USER);
        MentorProfileEntity profileB = mentorProfileRepository.saveAndFlush(new MentorProfileEntity(userB.getId()));
        MentorApplicationEntity applicationB = new MentorApplicationEntity(profileB.getId(), userB.getId(), UserType.WORKING_PROFESSIONAL.name(), 1);
        applicationB.setBio("User B private application");
        mentorApplicationRepository.saveAndFlush(applicationB);

        mockMvc.perform(get("/api/v1/mentor/applications/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(userA.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/mentor/applications/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(userB.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mentorApplicationId").value(applicationB.getId().toString()))
                .andExpect(jsonPath("$.data.bio").value("User B private application"));
    }

    @Test
    @DisplayName("Rejected application can be resubmitted as a new pending attempt")
    void resubmitRejectedApplication_createsNewAttemptAndPreservesHistory() throws Exception {
        UserEntity user = TestFixtures.saveUser(
                userRepository, "resubmit-success@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity profile = new MentorProfileEntity(user.getId());
        profile.setMentorStatus(MentorStatus.REJECTED);
        profile.setAcceptingBookings(false);
        profile = mentorProfileRepository.saveAndFlush(profile);

        MentorApplicationEntity firstAttempt = new MentorApplicationEntity(
                profile.getId(), user.getId(), UserType.UNIVERSITY_STUDENT.name(), 1);
        firstAttempt.setStatus(MentorApplicationStatus.REJECTED);
        firstAttempt.setSchoolId(schoolRepository.findAll().stream().findFirst().orElseThrow().getId());
        firstAttempt.setBio("Rejected bio");
        firstAttempt.setRejectionReason("Previous review decision");
        mentorApplicationRepository.saveAndFlush(firstAttempt);

        SubmitMentorApplicationRequest requestDto = new SubmitMentorApplicationRequest(
                null, null, "Computer Science", "Updated bio", "Java, Spring", "https://meet.example/new");
        MvcResult result = mockMvc.perform(post("/api/v1/mentor/applications/resubmit")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mentorProfileId").value(profile.getId().toString()))
                .andExpect(jsonPath("$.data.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.data.attemptNumber").value(2))
                .andExpect(jsonPath("$.data.mentorStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.applicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.acceptingBookings").value(false))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty())
                .andReturn();

        String newApplicationId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("mentorApplicationId").asText();
        assertThat(newApplicationId).isNotBlank().isNotEqualTo(firstAttempt.getId().toString());

        List<MentorApplicationEntity> attempts = mentorApplicationRepository
                .findByMentorProfileIdOrderByAttemptNumberDesc(profile.getId());
        assertThat(attempts).hasSize(2);
        assertThat(attempts.get(0).getAttemptNumber()).isEqualTo(2);
        assertThat(attempts.get(0).getStatus()).isEqualTo(MentorApplicationStatus.PENDING);
        assertThat(attempts.get(0).getCustomMajorName()).isEqualTo("Computer Science");
        assertThat(attempts.get(0).getBio()).isEqualTo("Updated bio");
        assertThat(attempts.get(0).getId().toString()).isEqualTo(newApplicationId);
        assertThat(attempts.get(0).getRejectionReason()).isNull();
        assertThat(attempts.get(1).getId()).isEqualTo(firstAttempt.getId());
        assertThat(attempts.get(1).getStatus()).isEqualTo(MentorApplicationStatus.REJECTED);
        assertThat(attempts.get(1).getRejectionReason()).isEqualTo("Previous review decision");

        MentorProfileEntity updatedProfile = mentorProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(updatedProfile.getMentorStatus()).isEqualTo(MentorStatus.PENDING);
        assertThat(updatedProfile.isAcceptingBookings()).isFalse();
        assertThat(updatedProfile.getBio()).isEqualTo("Updated bio");
        assertThat(updatedProfile.getExpertiseAreas()).isEqualTo("Java, Spring");
        assertThat(updatedProfile.getDefaultMeetingLink()).isEqualTo("https://meet.example/new");

        mockMvc.perform(get("/api/v1/mentor/applications/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mentorApplicationId").value(newApplicationId))
                .andExpect(jsonPath("$.data.attemptNumber").value(2))
                .andExpect(jsonPath("$.data.applicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.bio").value("Updated bio"))
                .andExpect(jsonPath("$.data.rejectionReason").doesNotExist());
    }

    @Test
    @DisplayName("Resubmission is allowed only for the latest rejected attempt")
    void resubmitApplication_rejectsPendingApprovedAndCancelledLatestAttempts() throws Exception {
        UserEntity user = TestFixtures.saveUser(
                userRepository, "resubmit-statuses@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity profile = mentorProfileRepository.saveAndFlush(new MentorProfileEntity(user.getId()));

        MentorApplicationEntity olderRejected = new MentorApplicationEntity(
                profile.getId(), user.getId(), UserType.UNIVERSITY_STUDENT.name(), 1);
        olderRejected.setStatus(MentorApplicationStatus.REJECTED);
        mentorApplicationRepository.saveAndFlush(olderRejected);

        MentorApplicationEntity latest = new MentorApplicationEntity(
                profile.getId(), user.getId(), UserType.UNIVERSITY_STUDENT.name(), 2);
        mentorApplicationRepository.saveAndFlush(latest);

        for (MentorApplicationStatus latestStatus : List.of(
                MentorApplicationStatus.PENDING,
                MentorApplicationStatus.APPROVED,
                MentorApplicationStatus.CANCELLED)) {
            latest.setStatus(latestStatus);
            mentorApplicationRepository.saveAndFlush(latest);

            mockMvc.perform(post("/api/v1/mentor/applications/resubmit")
                            .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new SubmitMentorApplicationRequest(null, null, null, "Bio", "Expertise", null))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("CONFLICT"));
        }

        assertThat(mentorApplicationRepository.findByMentorProfileIdOrderByAttemptNumberDesc(profile.getId()))
                .hasSize(2);
        assertThat(olderRejected.getStatus()).isEqualTo(MentorApplicationStatus.REJECTED);
    }

    @Test
    @DisplayName("Resubmission without an application returns 404")
    void resubmitApplication_withoutPreviousAttempt_returns404() throws Exception {
        UserEntity user = TestFixtures.saveUser(
                userRepository, "resubmit-no-attempt@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        mentorProfileRepository.saveAndFlush(new MentorProfileEntity(user.getId()));

        mockMvc.perform(post("/api/v1/mentor/applications/resubmit")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitMentorApplicationRequest(null, null, null, "Bio", "Expertise", null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Suspended mentor profile cannot be resubmitted")
    void resubmitApplication_suspendedProfile_returns403() throws Exception {
        UserEntity user = TestFixtures.saveUser(
                userRepository, "resubmit-suspended@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity profile = new MentorProfileEntity(user.getId());
        profile.setMentorStatus(MentorStatus.SUSPENDED);
        profile = mentorProfileRepository.saveAndFlush(profile);
        MentorApplicationEntity rejected = new MentorApplicationEntity(
                profile.getId(), user.getId(), UserType.UNIVERSITY_STUDENT.name(), 1);
        rejected.setStatus(MentorApplicationStatus.REJECTED);
        mentorApplicationRepository.saveAndFlush(rejected);

        mockMvc.perform(post("/api/v1/mentor/applications/resubmit")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitMentorApplicationRequest(null, null, null, "Bio", "Expertise", null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        assertThat(mentorApplicationRepository.findByMentorProfileIdOrderByAttemptNumberDesc(profile.getId()))
                .hasSize(1);
    }

    @Test
    @DisplayName("Anonymous resubmission returns 401")
    void resubmitApplication_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/mentor/applications/resubmit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitMentorApplicationRequest(null, null, null, "Bio", "Expertise", null))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Resubmission request validation is enforced")
    void resubmitApplication_oversizedBio_returns400() throws Exception {
        UserEntity user = TestFixtures.saveUser(
                userRepository, "resubmit-invalid@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity profile = new MentorProfileEntity(user.getId());
        profile.setMentorStatus(MentorStatus.REJECTED);
        profile = mentorProfileRepository.saveAndFlush(profile);
        MentorApplicationEntity rejected = new MentorApplicationEntity(
                profile.getId(), user.getId(), UserType.UNIVERSITY_STUDENT.name(), 1);
        rejected.setStatus(MentorApplicationStatus.REJECTED);
        mentorApplicationRepository.saveAndFlush(rejected);

        mockMvc.perform(post("/api/v1/mentor/applications/resubmit")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitMentorApplicationRequest(
                                null, null, null, "x".repeat(1001), null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        assertThat(mentorApplicationRepository.findByMentorProfileIdOrderByAttemptNumberDesc(profile.getId()))
                .hasSize(1);
        assertThat(mentorProfileRepository.findByUserId(user.getId()).orElseThrow().getMentorStatus())
                .isEqualTo(MentorStatus.REJECTED);
    }

    @Test
    @DisplayName("Ineligible account type cannot resubmit a rejected application")
    void resubmitApplication_ineligibleUserType_returns403() throws Exception {
        UserEntity user = TestFixtures.saveUser(
                userRepository, "resubmit-school-student@mentify.vn", UserType.SCHOOL_STUDENT, UserRole.USER);
        MentorProfileEntity profile = new MentorProfileEntity(user.getId());
        profile.setMentorStatus(MentorStatus.REJECTED);
        profile = mentorProfileRepository.saveAndFlush(profile);
        MentorApplicationEntity rejected = new MentorApplicationEntity(
                profile.getId(), user.getId(), UserType.UNIVERSITY_STUDENT.name(), 1);
        rejected.setStatus(MentorApplicationStatus.REJECTED);
        mentorApplicationRepository.saveAndFlush(rejected);

        mockMvc.perform(post("/api/v1/mentor/applications/resubmit")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitMentorApplicationRequest(null, null, null, "Bio", "Expertise", null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        assertThat(mentorApplicationRepository.findByMentorProfileIdOrderByAttemptNumberDesc(profile.getId()))
                .hasSize(1);
    }

    @Test
    @DisplayName("Resubmission ownership comes from the authenticated principal")
    void resubmitApplication_cannotSelectAnotherUsersApplication() throws Exception {
        UserEntity applicant = TestFixtures.saveUser(
                userRepository, "resubmit-owner@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        UserEntity otherUser = TestFixtures.saveUser(
                userRepository, "resubmit-other@mentify.vn", UserType.WORKING_PROFESSIONAL, UserRole.USER);
        MentorProfileEntity applicantProfile = new MentorProfileEntity(applicant.getId());
        applicantProfile.setMentorStatus(MentorStatus.REJECTED);
        applicantProfile = mentorProfileRepository.saveAndFlush(applicantProfile);
        MentorProfileEntity otherProfile = new MentorProfileEntity(otherUser.getId());
        otherProfile.setMentorStatus(MentorStatus.REJECTED);
        otherProfile = mentorProfileRepository.saveAndFlush(otherProfile);

        MentorApplicationEntity applicantAttempt = new MentorApplicationEntity(
                applicantProfile.getId(), applicant.getId(), UserType.UNIVERSITY_STUDENT.name(), 1);
        applicantAttempt.setStatus(MentorApplicationStatus.REJECTED);
        mentorApplicationRepository.saveAndFlush(applicantAttempt);
        MentorApplicationEntity otherAttempt = new MentorApplicationEntity(
                otherProfile.getId(), otherUser.getId(), UserType.WORKING_PROFESSIONAL.name(), 1);
        otherAttempt.setStatus(MentorApplicationStatus.REJECTED);
        mentorApplicationRepository.saveAndFlush(otherAttempt);

        ObjectNode requestBody = objectMapper.valueToTree(new SubmitMentorApplicationRequest(
                null, null, null, "Applicant update", "Java", null));
        requestBody.put("userId", otherUser.getId().toString());
        requestBody.put("mentorProfileId", otherProfile.getId().toString());
        requestBody.put("mentorApplicationId", otherAttempt.getId().toString());
        requestBody.put("attemptNumber", 99);
        requestBody.put("mentorStatus", "APPROVED");
        requestBody.put("applicationStatus", "APPROVED");
        requestBody.put("reviewedAt", "2026-01-01T00:00:00Z");
        requestBody.put("rejectionReason", "client supplied review data");
        MvcResult response = mockMvc.perform(post("/api/v1/mentor/applications/resubmit")
                        .with(SecurityMockMvcRequestPostProcessors.user(applicant.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andReturn();
        int responseStatus = response.getResponse().getStatus();
        assertThat(responseStatus).isIn(201, 400);
        if (responseStatus == 201) {
            var responseData = objectMapper.readTree(response.getResponse().getContentAsString()).path("data");
            assertThat(responseData.path("userId").asText()).isEqualTo(applicant.getId().toString());
            assertThat(responseData.path("mentorProfileId").asText()).isEqualTo(applicantProfile.getId().toString());
            assertThat(responseData.path("attemptNumber").asInt()).isEqualTo(2);
            assertThat(responseData.path("mentorStatus").asText()).isEqualTo("PENDING");
            assertThat(responseData.path("applicationStatus").asText()).isEqualTo("PENDING");
        } else {
            assertThat(objectMapper.readTree(response.getResponse().getContentAsString())
                    .path("error").path("code").asText()).isEqualTo("VALIDATION_ERROR");
            assertThat(mentorApplicationRepository.findByMentorProfileIdOrderByAttemptNumberDesc(applicantProfile.getId()))
                    .hasSize(1);
        }

        assertThat(mentorApplicationRepository.findByMentorProfileIdOrderByAttemptNumberDesc(otherProfile.getId()))
                .hasSize(1)
                .first()
                .extracting(MentorApplicationEntity::getId)
                .isEqualTo(otherAttempt.getId());
    }

    @Test
    @DisplayName("Concurrent resubmissions create exactly one next attempt")
    void resubmitApplication_concurrentRequests_createOneAttempt() throws Exception {
        UserEntity user = TestFixtures.saveUser(
                userRepository, "resubmit-race@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity profile = new MentorProfileEntity(user.getId());
        profile.setMentorStatus(MentorStatus.REJECTED);
        profile = mentorProfileRepository.saveAndFlush(profile);
        MentorApplicationEntity rejected = new MentorApplicationEntity(
                profile.getId(), user.getId(), UserType.UNIVERSITY_STUDENT.name(), 1);
        rejected.setStatus(MentorApplicationStatus.REJECTED);
        mentorApplicationRepository.saveAndFlush(rejected);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        SubmitMentorApplicationRequest requestDto = new SubmitMentorApplicationRequest(
                null, null, null, "Concurrent update", "Java", null);
        try {
            Future<Integer> first = submitConcurrentResubmission(executor, ready, start, user, requestDto);
            Future<Integer> second = submitConcurrentResubmission(executor, ready, start, user, requestDto);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statuses = List.of(
                    first.get(30, TimeUnit.SECONDS),
                    second.get(30, TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        List<MentorApplicationEntity> attempts = mentorApplicationRepository
                .findByMentorProfileIdOrderByAttemptNumberDesc(profile.getId());
        assertThat(attempts).hasSize(2);
        assertThat(attempts.get(0).getAttemptNumber()).isEqualTo(2);
        assertThat(attempts.get(0).getStatus()).isEqualTo(MentorApplicationStatus.PENDING);
        assertThat(attempts.get(1).getId()).isEqualTo(rejected.getId());
        assertThat(attempts.get(1).getStatus()).isEqualTo(MentorApplicationStatus.REJECTED);
    }

    @Test
    @DisplayName("Admin pending queue is paginated and contains latest pending attempts only")
    void adminPendingQueue_returnsLatestPendingOnly() throws Exception {
        UserEntity admin = TestFixtures.saveUser(
                userRepository, "review-admin@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.ADMIN);
        UserEntity oldPendingUser = TestFixtures.saveUser(
                userRepository, "old-pending@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity oldPendingProfile = pendingProfile(oldPendingUser);
        MentorApplicationEntity oldPending = application(oldPendingProfile, oldPendingUser, 1,
                MentorApplicationStatus.PENDING);
        MentorApplicationEntity laterRejected = application(oldPendingProfile, oldPendingUser, 2,
                MentorApplicationStatus.REJECTED);

        UserEntity currentPendingUser = TestFixtures.saveUser(
                userRepository, "current-pending@mentify.vn", UserType.WORKING_PROFESSIONAL, UserRole.USER);
        MentorProfileEntity currentPendingProfile = pendingProfile(currentPendingUser);
        MentorApplicationEntity currentPending = application(currentPendingProfile, currentPendingUser, 2,
                MentorApplicationStatus.PENDING);
        application(currentPendingProfile, currentPendingUser, 1, MentorApplicationStatus.REJECTED);
        UserEntity secondPendingUser = TestFixtures.saveUser(
                userRepository, "second-pending@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity secondPendingProfile = pendingProfile(secondPendingUser);
        MentorApplicationEntity secondPending = application(secondPendingProfile, secondPendingUser, 1,
                MentorApplicationStatus.PENDING);

        mockMvc.perform(get("/api/v1/admin/mentor/applications")
                        .param("page", "0").param("size", "1")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(1))
                .andExpect(jsonPath("$.meta.totalItems").value(2))
                .andExpect(jsonPath("$.data[0].applicationId").value(currentPending.getId().toString()))
                .andExpect(jsonPath("$.data[0].applicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist());
        MvcResult pageTwo = mockMvc.perform(get("/api/v1/admin/mentor/applications")
                        .param("page", "1").param("size", "1")
                        .with(SecurityMockMvcRequestPostProcessors.user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andReturn();
        assertThat(objectMapper.readTree(pageTwo.getResponse().getContentAsString())
                .path("data").get(0).path("applicationId").asText())
                .isEqualTo(secondPending.getId().toString());
        assertThat(oldPending.getId()).isNotEqualTo(laterRejected.getId());
    }

    @Test
    @DisplayName("Admin can approve and reject latest pending applications with synchronized profile state")
    void adminReview_approvesAndRejects() throws Exception {
        UserEntity admin = TestFixtures.saveUser(
                userRepository, "review-admin-actions@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.ADMIN);
        UserEntity approveUser = TestFixtures.saveUser(
                userRepository, "review-approve@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity approvedProfile = pendingProfile(approveUser);
        MentorApplicationEntity toApprove = application(approvedProfile, approveUser, 1,
                MentorApplicationStatus.PENDING);
        approvedProfile.setAcceptingBookings(true);
        mentorProfileRepository.saveAndFlush(approvedProfile);

        UserEntity rejectUser = TestFixtures.saveUser(
                userRepository, "review-reject@mentify.vn", UserType.WORKING_PROFESSIONAL, UserRole.USER);
        MentorProfileEntity rejectedProfile = pendingProfile(rejectUser);
        MentorApplicationEntity toReject = application(rejectedProfile, rejectUser, 1,
                MentorApplicationStatus.PENDING);
        var adminPrincipal = SecurityMockMvcRequestPostProcessors.user(admin.getEmail()).roles("ADMIN");

        mockMvc.perform(get("/api/v1/admin/mentor/applications/{id}", toApprove.getId()).with(adminPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(toApprove.getId().toString()))
                .andExpect(jsonPath("$.data.applicationStatus").value("PENDING"));
        mockMvc.perform(get("/api/v1/admin/mentor/applications/{id}", UUID.randomUUID()).with(adminPrincipal))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/admin/mentor/applications/{id}/approve", toApprove.getId())
                        .with(adminPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.mentorStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.acceptingBookings").value(false));
        assertThat(mentorApplicationRepository.findById(toApprove.getId()).orElseThrow().getReviewedAt()).isNotNull();
        assertThat(mentorProfileRepository.findById(approvedProfile.getId()).orElseThrow().isAcceptingBookings()).isFalse();
        mockMvc.perform(post("/api/v1/admin/mentor/applications/{id}/approve", toApprove.getId())
                        .with(adminPrincipal))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/admin/mentor/applications/{id}/reject", toReject.getId())
                        .with(adminPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"Please add more mentoring experience.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.mentorStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Please add more mentoring experience."));
        assertThat(mentorProfileRepository.findById(rejectedProfile.getId()).orElseThrow().isAcceptingBookings()).isFalse();
        mockMvc.perform(post("/api/v1/admin/mentor/applications/{id}/reject", UUID.randomUUID())
                        .with(adminPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin review routes reject anonymous and USER principals")
    void adminReviewRoutes_requireAdmin() throws Exception {
        UserEntity applicant = TestFixtures.saveUser(
                userRepository, "review-protected@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        UserEntity normalUser = TestFixtures.saveUser(
                userRepository, "review-normal-user@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity profile = pendingProfile(applicant);
        MentorApplicationEntity application = application(profile, applicant, 1, MentorApplicationStatus.PENDING);
        String id = application.getId().toString();
        for (String path : List.of(
                "/api/v1/admin/mentor/applications",
                "/api/v1/admin/mentor/applications/" + id,
                "/api/v1/admin/mentor/applications/" + id + "/approve",
                "/api/v1/admin/mentor/applications/" + id + "/reject")) {
            var anonymous = path.endsWith("/approve") || path.endsWith("/reject")
                    ? mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"rejectionReason\":\"reason\"}"))
                    : mockMvc.perform(get(path));
            anonymous.andExpect(status().isUnauthorized());
            var asUser = path.endsWith("/approve") || path.endsWith("/reject")
                    ? mockMvc.perform(post(path).with(SecurityMockMvcRequestPostProcessors.user(normalUser.getEmail()).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"rejectionReason\":\"reason\"}"))
                    : mockMvc.perform(get(path).with(SecurityMockMvcRequestPostProcessors.user(normalUser.getEmail()).roles("USER")));
            asUser.andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Concurrent admin decisions produce one decision and one conflict")
    void adminReview_concurrentApproveAndReject_onlyOneWins() throws Exception {
        UserEntity firstAdmin = TestFixtures.saveUser(
                userRepository, "review-race-a@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.ADMIN);
        UserEntity secondAdmin = TestFixtures.saveUser(
                userRepository, "review-race-b@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.ADMIN);
        UserEntity applicant = TestFixtures.saveUser(
                userRepository, "review-race-applicant@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity profile = pendingProfile(applicant);
        MentorApplicationEntity application = application(profile, applicant, 1, MentorApplicationStatus.PENDING);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> approve = concurrentReview(executor, ready, start, firstAdmin, application.getId(), true);
            Future<Integer> reject = concurrentReview(executor, ready, start, secondAdmin, application.getId(), false);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(approve.get(30, TimeUnit.SECONDS), reject.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        MentorApplicationEntity reviewed = mentorApplicationRepository.findById(application.getId()).orElseThrow();
        MentorProfileEntity reviewedProfile = mentorProfileRepository.findById(profile.getId()).orElseThrow();
        assertThat(reviewed.getStatus()).isIn(MentorApplicationStatus.APPROVED, MentorApplicationStatus.REJECTED);
        assertThat(reviewed.getReviewedAt()).isNotNull();
        assertThat(reviewedProfile.getMentorStatus().name()).isEqualTo(reviewed.getStatus().name());
        assertThat(reviewedProfile.isAcceptingBookings()).isFalse();
    }

    @Test
    @DisplayName("Historical attempts and suspended profiles cannot be approved")
    void adminReview_rejectsHistoricalAndSuspendedApplications() throws Exception {
        UserEntity admin = TestFixtures.saveUser(
                userRepository, "review-guard-admin@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.ADMIN);
        UserEntity historicalUser = TestFixtures.saveUser(
                userRepository, "review-historical@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity historicalProfile = pendingProfile(historicalUser);
        MentorApplicationEntity historical = application(historicalProfile, historicalUser, 1, MentorApplicationStatus.PENDING);
        application(historicalProfile, historicalUser, 2, MentorApplicationStatus.PENDING);
        UserEntity suspendedUser = TestFixtures.saveUser(
                userRepository, "review-suspended@mentify.vn", UserType.UNIVERSITY_STUDENT, UserRole.USER);
        MentorProfileEntity suspendedProfile = pendingProfile(suspendedUser);
        suspendedProfile.setMentorStatus(MentorStatus.SUSPENDED);
        mentorProfileRepository.saveAndFlush(suspendedProfile);
        MentorApplicationEntity suspended = application(suspendedProfile, suspendedUser, 1,
                MentorApplicationStatus.PENDING);
        var adminPrincipal = SecurityMockMvcRequestPostProcessors.user(admin.getEmail()).roles("ADMIN");
        mockMvc.perform(post("/api/v1/admin/mentor/applications/{id}/approve", historical.getId())
                        .with(adminPrincipal))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/admin/mentor/applications/{id}/approve", suspended.getId())
                        .with(adminPrincipal))
                .andExpect(status().isForbidden());
    }

    private MentorProfileEntity pendingProfile(UserEntity user) {
        MentorProfileEntity profile = new MentorProfileEntity(user.getId());
        profile.setMentorStatus(MentorStatus.PENDING);
        return mentorProfileRepository.saveAndFlush(profile);
    }

    private MentorApplicationEntity application(
            MentorProfileEntity profile,
            UserEntity user,
            int attemptNumber,
            MentorApplicationStatus status) {
        MentorApplicationEntity application = new MentorApplicationEntity(
                profile.getId(), user.getId(), user.getUserType().name(), attemptNumber);
        application.setStatus(status);
        return mentorApplicationRepository.saveAndFlush(application);
    }

    private Future<Integer> concurrentReview(
            ExecutorService executor,
            CountDownLatch ready,
            CountDownLatch start,
            UserEntity admin,
            UUID applicationId,
            boolean approve) {
        return executor.submit(() -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent review test start timed out");
            }
            var builder = approve
                    ? post("/api/v1/admin/mentor/applications/{id}/approve", applicationId)
                    : post("/api/v1/admin/mentor/applications/{id}/reject", applicationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"rejectionReason\":\"Not enough detail\"}");
            return mockMvc.perform(builder.with(SecurityMockMvcRequestPostProcessors.user(admin.getEmail()).roles("ADMIN")))
                    .andReturn().getResponse().getStatus();
        });
    }

    private Future<Integer> submitConcurrentResubmission(
            ExecutorService executor,
            CountDownLatch ready,
            CountDownLatch start,
            UserEntity user,
            SubmitMentorApplicationRequest requestDto) {
        return executor.submit(() -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent resubmission test start timed out");
            }
            return mockMvc.perform(post("/api/v1/mentor/applications/resubmit")
                            .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        });
    }
}
