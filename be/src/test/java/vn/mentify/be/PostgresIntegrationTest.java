package vn.mentify.be;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.modules.user.persistence.UserType;

import java.util.Map;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mentify")
            .withUsername("mentify")
            .withPassword("mentify_test");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void flywayAndHibernateValidateAgainstCleanPostgresDatabase() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '1'",
                Integer.class);

        assertThat(migrationCount).isEqualTo(1);
    }

    @Test
    void savesAndFindsUserByNormalizedEmail() {
        UserEntity saved = userRepository.saveAndFlush(
                new UserEntity("  STUDENT@Example.COM ", UserType.SCHOOL_STUDENT));

        assertThat(saved.getEmail()).isEqualTo("student@example.com");
        assertThat(userRepository.findByEmail("student@example.com"))
                .get()
                .extracting(UserEntity::getEmail)
                .isEqualTo("student@example.com");
        assertThat(userRepository.existsByEmail("student@example.com")).isTrue();
    }

    @Test
    void auditingSetsAndUpdatesTimestampsWithoutChangingCreationTime() throws InterruptedException {
        UserEntity saved = userRepository.saveAndFlush(
                new UserEntity("audit@example.com", UserType.SCHOOL_STUDENT));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        var createdAt = saved.getCreatedAt();
        var firstUpdatedAt = saved.getUpdatedAt();

        Thread.sleep(10);
        saved.changeEmail("audit-updated@example.com");
        saved = userRepository.saveAndFlush(saved);

        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        assertThat(saved.getUpdatedAt()).isAfter(firstUpdatedAt);
    }

    @Test
    void postgresSessionTimezoneDoesNotChangeStoredInstant() {
        UserEntity saved = userRepository.saveAndFlush(
                new UserEntity("timezone@example.com", UserType.SCHOOL_STUDENT));

        jdbcTemplate.execute("set time zone 'Asia/Ho_Chi_Minh'");
        BigDecimal vietnamEpoch = jdbcTemplate.queryForObject(
                "select extract(epoch from created_at) from users where id = ?",
                BigDecimal.class, saved.getId());

        jdbcTemplate.execute("set time zone 'UTC'");
        BigDecimal utcEpoch = jdbcTemplate.queryForObject(
                "select extract(epoch from created_at) from users where id = ?",
                BigDecimal.class, saved.getId());

        assertThat(vietnamEpoch).isEqualByComparingTo(utcEpoch);
        BigDecimal expectedEpoch = BigDecimal.valueOf(saved.getCreatedAt().getEpochSecond())
                .add(BigDecimal.valueOf(saved.getCreatedAt().getNano(), 9));
        assertThat(utcEpoch.subtract(expectedEpoch).abs())
                .isLessThan(BigDecimal.valueOf(0.000001));
    }

    @Test
    void databaseRejectsDuplicateEmail() {
        userRepository.saveAndFlush(new UserEntity("duplicate@example.com", UserType.UNIVERSITY_STUDENT));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new UserEntity("duplicate@example.com", UserType.WORKING_PROFESSIONAL)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseDefaultsRoleAndAccountStatus() {
        jdbcTemplate.update(
                "insert into users (id, email, user_type, created_at, updated_at) values (gen_random_uuid(), ?, ?, now(), now())",
                "defaults@example.com", UserType.SCHOOL_STUDENT.name());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select role, account_status from users where email = ?",
                "defaults@example.com");

        assertThat(row.get("role")).isEqualTo("USER");
        assertThat(row.get("account_status")).isEqualTo("ACTIVE");
    }
}
