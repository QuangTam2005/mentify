package vn.mentify.be;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
