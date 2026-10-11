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
