package vn.mentify.be;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.modules.masterdata.domain.SchoolEntity;
import vn.mentify.be.modules.masterdata.persistence.MajorGroupRepository;
import vn.mentify.be.modules.masterdata.persistence.SchoolRepository;
import vn.mentify.be.modules.user.dto.RecordConsentRequest;
import vn.mentify.be.modules.user.dto.UpdateProfileRequest;
import vn.mentify.be.modules.user.persistence.UserConsentRepository;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserProfileRepository;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.modules.user.persistence.UserTypeHistoryEntity;
import vn.mentify.be.modules.user.persistence.UserTypeHistoryRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class UserContractTests {

    private static final String ISO_OFFSET_REGEX = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?\\+07:00$";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mentify")
            .withUsername("mentify")
            .withPassword("mentify_test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserConsentRepository userConsentRepository;

    @Autowired
    private UserTypeHistoryRepository userTypeHistoryRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private MajorGroupRepository majorGroupRepository;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void cleanUserData() {
        userConsentRepository.deleteAll();
        userTypeHistoryRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void unauthenticatedRequestsReturn401() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(ISO_OFFSET_REGEX)));

        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        mockMvc.perform(put("/api/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        mockMvc.perform(post("/api/users/consent")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getProfile_whenUserExistsWithoutProfile_returnsNullProfileFields() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("student@mentify.vn", UserType.SCHOOL_STUDENT));

        mockMvc.perform(get("/api/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(ISO_OFFSET_REGEX)))
                .andExpect(jsonPath("$.data.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.data.email").value("student@mentify.vn"))
                .andExpect(jsonPath("$.data.userType").value("SCHOOL_STUDENT"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.fullName").doesNotExist())
                .andExpect(jsonPath("$.data.school").doesNotExist());

        // Alias check: /api/v1/users/profile
        mockMvc.perform(get("/api/v1/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(user.getId().toString()));
    }

    @Test
    void updateProfile_withValidMasterData_updatesProfileSuccessfully() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("learner@mentify.vn", UserType.SCHOOL_STUDENT));
        SchoolEntity school = schoolRepository.findAll().getFirst();
        var majorGroup = majorGroupRepository.findAll().getFirst();

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Nguyễn Văn An",
                "Học sinh THPT đam mê công nghệ phần mềm",
                "https://cdn.mentify.vn/avatars/an.png",
                school.getId(),
                majorGroup.getCode(),
                "Kỹ thuật phần mềm",
                null
        );

        mockMvc.perform(put("/api/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.data.bio").value("Học sinh THPT đam mê công nghệ phần mềm"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.mentify.vn/avatars/an.png"))
                .andExpect(jsonPath("$.data.customMajorName").value("Kỹ thuật phần mềm"))
                .andExpect(jsonPath("$.data.school.id").value(school.getId().toString()))
                .andExpect(jsonPath("$.data.school.code").value(school.getCode()))
                .andExpect(jsonPath("$.data.majorGroup.code").value(majorGroup.getCode()));

        // Verify database persistence
        var profile = userProfileRepository.findByUserId(user.getId());
        assertThat(profile).isPresent();
        assertThat(profile.get().getFullName()).isEqualTo("Nguyễn Văn An");
        assertThat(profile.get().getSchool().getId()).isEqualTo(school.getId());
        assertThat(profile.get().getMajorGroup().getCode()).isEqualTo(majorGroup.getCode());
    }

    @Test
    void updateProfile_promoteUserType_succeedsAndRecordsHistory() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("student2@mentify.vn", UserType.SCHOOL_STUDENT));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Trần Văn Bình",
                "Tân sinh viên",
                null,
                null,
                null,
                null,
                UserType.UNIVERSITY_STUDENT
        );

        mockMvc.perform(put("/api/v1/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userType").value("UNIVERSITY_STUDENT"));

        // Verify user updated in DB
        UserEntity updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getUserType()).isEqualTo(UserType.UNIVERSITY_STUDENT);

        // Verify history entry
        List<UserTypeHistoryEntity> histories = userTypeHistoryRepository.findByUserIdOrderByChangedAtDesc(user.getId());
        assertThat(histories).hasSize(1);
        assertThat(histories.getFirst().getOldType()).isEqualTo(UserType.SCHOOL_STUDENT);
        assertThat(histories.getFirst().getNewType()).isEqualTo(UserType.UNIVERSITY_STUDENT);
        assertThat(histories.getFirst().getChangedAt()).isNotNull();
    }

    @Test
    void updateProfile_downgradeUserType_returns409Conflict() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("pro@mentify.vn", UserType.WORKING_PROFESSIONAL));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Lê Hoàng Cường",
                "Đang đi làm",
                null,
                null,
                null,
                null,
                UserType.UNIVERSITY_STUDENT // Downgrade forbidden!
        );

        mockMvc.perform(put("/api/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(ISO_OFFSET_REGEX)));

        // Verify user type unchanged
        UserEntity refreshed = userRepository.findById(user.getId()).orElseThrow();
        assertThat(refreshed.getUserType()).isEqualTo(UserType.WORKING_PROFESSIONAL);

        // Verify no history entry
        List<UserTypeHistoryEntity> histories = userTypeHistoryRepository.findByUserIdOrderByChangedAtDesc(user.getId());
        assertThat(histories).isEmpty();
    }

    @Test
    void recordConsent_succeedsAndPersistsAudit() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("student3@mentify.vn", UserType.SCHOOL_STUDENT));

        RecordConsentRequest request = new RecordConsentRequest(
                "DATA_PROTECTION_AND_TOS",
                "2026.1",
                "Nguyễn Phụ Huynh",
                "0987654321"
        );

        mockMvc.perform(post("/api/users/consent")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .header("X-Forwarded-For", "192.168.1.50, 10.0.0.1")
                        .header("User-Agent", "Mentify-Mobile/1.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.consentType").value("DATA_PROTECTION_AND_TOS"))
                .andExpect(jsonPath("$.data.policyVersion").value("2026.1"))
                .andExpect(jsonPath("$.data.guardianName").value("Nguyễn Phụ Huynh"))
                .andExpect(jsonPath("$.data.guardianContact").value("0987654321"))
                .andExpect(jsonPath("$.data.acceptedAt").value(matchesPattern(ISO_OFFSET_REGEX)));

        // Verify repository
        var consents = userConsentRepository.findByUserIdOrderByAcceptedAtDesc(user.getId());
        assertThat(consents).hasSize(1);
        assertThat(consents.getFirst().getIpAddress()).isEqualTo("192.168.1.50");
        assertThat(consents.getFirst().getUserAgent()).isEqualTo("Mentify-Mobile/1.0");
        assertThat(consents.getFirst().getGuardianName()).isEqualTo("Nguyễn Phụ Huynh");
    }

    @Test
    void validationErrors_return400() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("valid@mentify.vn", UserType.SCHOOL_STUDENT));

        // Name exceeding 128 chars
        String longName = "A".repeat(129);
        UpdateProfileRequest badProfile = new UpdateProfileRequest(
                longName,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(put("/api/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badProfile)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("fullName"));

        // Consent with empty type
        RecordConsentRequest badConsent = new RecordConsentRequest(
                "",
                "v1.0",
                null,
                null
        );

        mockMvc.perform(post("/api/users/consent")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badConsent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateProfile_withNonExistentSchool_returns404() throws Exception {
        UserEntity user = userRepository.save(new UserEntity("student4@mentify.vn", UserType.SCHOOL_STUDENT));
        UUID nonExistentSchoolId = UUID.randomUUID();

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Name",
                null,
                null,
                nonExistentSchoolId,
                null,
                null,
                null
        );

        mockMvc.perform(put("/api/users/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(user.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}
