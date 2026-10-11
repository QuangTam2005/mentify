package vn.mentify.be;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.modules.masterdata.domain.SchoolEntity;
import vn.mentify.be.modules.masterdata.persistence.MajorGroupRepository;
import vn.mentify.be.modules.masterdata.persistence.SchoolRepository;
import vn.mentify.be.modules.user.persistence.UserConsentEntity;
import vn.mentify.be.modules.user.persistence.UserConsentRepository;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserProfileEntity;
import vn.mentify.be.modules.user.persistence.UserProfileRepository;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.modules.user.persistence.UserTypeHistoryEntity;
import vn.mentify.be.modules.user.persistence.UserTypeHistoryRepository;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class UserPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mentify")
            .withUsername("mentify")
            .withPassword("mentify_test");

    @Autowired
    private JdbcClient jdbcClient;

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
    void flywayMigrationV3_createdExpectedTablesAndConstraints() {
        Integer profileTableCount = jdbcClient.sql("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_name = 'user_profiles'
                """).query(Integer.class).single();
        assertThat(profileTableCount).isEqualTo(1);

        Integer consentTableCount = jdbcClient.sql("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_name = 'user_consents'
                """).query(Integer.class).single();
        assertThat(consentTableCount).isEqualTo(1);

        Integer historyTableCount = jdbcClient.sql("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_name = 'user_type_histories'
                """).query(Integer.class).single();
        assertThat(historyTableCount).isEqualTo(1);
    }

    @Test
    void userProfile_oneToOneUniqueConstraintEnforced() {
        UserEntity user = userRepository.save(new UserEntity("unique-test@mentify.vn", UserType.SCHOOL_STUDENT));

        UserProfileEntity profile1 = new UserProfileEntity(user);
        profile1.update("First Profile", "Bio 1", null, null, null, null);
        userProfileRepository.saveAndFlush(profile1);

        // Attempting to create a second profile for the same user should fail unique constraint
        UserProfileEntity profile2 = new UserProfileEntity(user);
        profile2.update("Second Profile", "Bio 2", null, null, null, null);

        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(profile2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userCascadeDelete_deletesProfileConsentsAndHistory() {
        UserEntity user = userRepository.save(new UserEntity("cascade-test@mentify.vn", UserType.SCHOOL_STUDENT));
        SchoolEntity school = schoolRepository.findAll().getFirst();
        var majorGroup = majorGroupRepository.findAll().getFirst();

        UserProfileEntity profile = new UserProfileEntity(user);
        profile.update("Cascade User", "Bio", "avatar.png", school, majorGroup, "Computer Science");
        userProfileRepository.saveAndFlush(profile);

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        UserConsentEntity consent = new UserConsentEntity(
                user, "TOS", "1.0", now, "127.0.0.1", "Agent", null, null
        );
        userConsentRepository.saveAndFlush(consent);

        UserTypeHistoryEntity history = new UserTypeHistoryEntity(
                user, UserType.SCHOOL_STUDENT, UserType.UNIVERSITY_STUDENT, now.toInstant()
        );
        userTypeHistoryRepository.saveAndFlush(history);

        // Verify entities exist
        assertThat(userProfileRepository.findByUserId(user.getId())).isPresent();
        assertThat(userConsentRepository.findByUserIdOrderByAcceptedAtDesc(user.getId())).hasSize(1);
        assertThat(userTypeHistoryRepository.findByUserIdOrderByChangedAtDesc(user.getId())).hasSize(1);

        // Delete user
        userRepository.deleteById(user.getId());
        userRepository.flush();

        // Verify cascade deletion
        assertThat(userProfileRepository.findByUserId(user.getId())).isEmpty();
        assertThat(userConsentRepository.findByUserIdOrderByAcceptedAtDesc(user.getId())).isEmpty();
        assertThat(userTypeHistoryRepository.findByUserIdOrderByChangedAtDesc(user.getId())).isEmpty();
    }
}
