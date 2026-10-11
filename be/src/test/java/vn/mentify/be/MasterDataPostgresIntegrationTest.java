package vn.mentify.be;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.modules.masterdata.domain.CampusType;
import vn.mentify.be.modules.masterdata.domain.MajorGroupEntity;
import vn.mentify.be.modules.masterdata.domain.SchoolEntity;
import vn.mentify.be.modules.masterdata.persistence.MajorGroupRepository;
import vn.mentify.be.modules.masterdata.persistence.SchoolRepository;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MasterDataPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mentify")
            .withUsername("mentify")
            .withPassword("mentify_test");

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void flywayAppliesV2Migration() {
        Integer v2Count = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '2'",
                Integer.class);
        assertThat(v2Count).isEqualTo(1);
    }

    @Test
    void schoolsTableHasExactSeededCountOf110() {
        Integer schoolCount = jdbcTemplate.queryForObject(
                "select count(*) from schools",
                Integer.class);
        assertThat(schoolCount).isEqualTo(110);
    }

    @Test
    void majorGroupsTableHasExactSeededCountOf94() {
        Integer majorCount = jdbcTemplate.queryForObject(
                "select count(*) from major_groups",
                Integer.class);
        assertThat(majorCount).isEqualTo(94);
    }

    @Test
    void branchCampusesAndParentRelationshipsAreConsistent() {
        Integer branchCount = jdbcTemplate.queryForObject(
                "select count(*) from schools where campus_type = 'BRANCH_CAMPUS'",
                Integer.class);
        assertThat(branchCount).isEqualTo(4);

        // Check Thủy lợi cơ sở 2 (TLS-HCM) parent is Thủy lợi cơ sở 1 (TLA)
        Map<String, Object> tlsHcm = jdbcTemplate.queryForMap(
                "select s.code, s.parent_school_id, p.code as parent_code from schools s " +
                "left join schools p on s.parent_school_id = p.id " +
                "where s.code = 'TLS-HCM'");

        assertThat(tlsHcm.get("parent_code")).isEqualTo("TLA");
    }

    @Test
    void databaseRejectsDuplicateSchoolCode() {
        SchoolEntity duplicate = new SchoolEntity(
                "TCT", // already exists
                "TCT2",
                "Trường Đại học Cần Thơ Duplicate",
                null,
                null,
                CampusType.MAIN_CAMPUS,
                null,
                "Cần Thơ",
                "ACTIVE",
                "2026");

        assertThatThrownBy(() -> schoolRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsInvalidMajorGroupCodeLength() {
        MajorGroupEntity invalidCode = new MajorGroupEntity(
                "140", // length 3, violates length(code) = 4
                "Invalid Group",
                "14",
                "TT_74_2026",
                "Description",
                true);

        assertThatThrownBy(() -> majorGroupRepository.saveAndFlush(invalidCode))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
