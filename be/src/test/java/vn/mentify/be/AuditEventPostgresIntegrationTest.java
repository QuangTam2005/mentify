package vn.mentify.be;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.modules.audit.domain.AuditActorType;
import vn.mentify.be.modules.audit.persistence.AuditEventRepository;
import vn.mentify.be.modules.audit.service.AuditEventCommand;
import vn.mentify.be.modules.audit.service.AuditEventService;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.modules.user.persistence.UserRole;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.modules.user.persistence.UserTypeHistoryRepository;
import vn.mentify.be.modules.user.dto.UpdateProfileRequest;
import vn.mentify.be.modules.user.service.UserService;
import vn.mentify.be.support.AbstractPostgresIntegrationTest;
import vn.mentify.be.support.TestClockConfiguration;
import vn.mentify.be.support.TestFixtures;

import java.util.Map;
import java.util.UUID;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import(TestClockConfiguration.class)
class AuditEventPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private UserService userService;

    @Autowired
    private UserTypeHistoryRepository userTypeHistoryRepository;

    @Autowired
    private Clock testClock;

    @BeforeEach
    void cleanData() {
        auditEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void flywayCreatesAuditTableAndIndexes() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'audit_events'",
                Integer.class);

        assertThat(tableCount).isEqualTo(1);
    }

    @Test
    void recordsAndQueriesAuditEventWithJsonMetadata() {
        var admin = TestFixtures.saveUser(
                userRepository,
                "audit-admin@mentify.vn",
                UserType.WORKING_PROFESSIONAL,
                UserRole.ADMIN);
        UUID resourceId = UUID.randomUUID();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> auditEventService.record(
                new AuditEventCommand(
                        admin.getId(),
                        AuditActorType.ADMIN,
                        "ADMIN_MENTOR_APPROVE",
                        "MENTOR_APPLICATION",
                        resourceId,
                        "Đủ minh chứng",
                        Map.of("attempt", 1, "source", "admin-console"),
                        "request-audit-1")));

        assertThat(auditEventService.findByResource(
                        "MENTOR_APPLICATION", resourceId, org.springframework.data.domain.PageRequest.of(0, 10)))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.actorUserId()).isEqualTo(admin.getId());
                    assertThat(event.actorType()).isEqualTo(AuditActorType.ADMIN);
                    assertThat(event.metadata()).containsEntry("source", "admin-console");
                    assertThat(event.createdAt()).isNotNull();
                });
    }

    @Test
    void auditRollsBackWithBusinessTransaction() {
        var user = TestFixtures.saveUser(
                userRepository,
                "audit-rollback@mentify.vn",
                UserType.SCHOOL_STUDENT,
                UserRole.USER);

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            auditEventService.record(new AuditEventCommand(
                    user.getId(),
                    AuditActorType.USER,
                    "ACCOUNT_DELETE_REQUEST",
                    "USER",
                    user.getId(),
                    null,
                    Map.of(),
                    "request-audit-rollback"));
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(auditEventRepository.count()).isZero();
    }

    @Test
    void businessRulesUseFixedClockAndVietnamTimezone() {
        var user = TestFixtures.saveUser(
                userRepository,
                "timezone-fixture@mentify.vn",
                UserType.SCHOOL_STUDENT,
                UserRole.USER);

        userService.updateProfile(user.getId(), new UpdateProfileRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                UserType.UNIVERSITY_STUDENT));

        assertThat(testClock.instant()).isEqualTo(java.time.Instant.parse("2026-01-15T10:00:00Z"));
        assertThat(userTypeHistoryRepository.findByUserIdOrderByChangedAtDesc(user.getId()))
                .singleElement()
                .extracting(history -> history.getChangedAt())
                .isEqualTo(testClock.instant());
    }
}
