package vn.mentify.be;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.infra.config.CacheConfig;
import vn.mentify.be.modules.masterdata.domain.CampusType;
import vn.mentify.be.modules.masterdata.dto.MajorGroupResponse;
import vn.mentify.be.modules.masterdata.dto.SchoolResponse;
import vn.mentify.be.modules.masterdata.persistence.MajorGroupRepository;
import vn.mentify.be.modules.masterdata.persistence.SchoolRepository;
import vn.mentify.be.modules.masterdata.service.MasterDataService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MasterDataContractTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mentify")
            .withUsername("mentify")
            .withPassword("mentify_test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private MasterDataService masterDataService;

    @MockitoBean
    private SchoolRepository schoolRepository;

    @MockitoBean
    private MajorGroupRepository majorGroupRepository;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void unauthenticatedUserCanGetSchoolsWithApiV1() throws Exception {
        SchoolResponse mockSchool = new SchoolResponse(
                UUID.randomUUID(),
                "TCT",
                "TCT",
                "Trường Đại học Cần Thơ",
                "ĐH Cần Thơ",
                "CTU",
                CampusType.MAIN_CAMPUS,
                null,
                "Cần Thơ",
                "ACTIVE",
                "2026");

        Page<SchoolResponse> page = new PageImpl<>(List.of(mockSchool), PageRequest.of(0, 50), 1);
        when(masterDataService.getSchools(any(), any(), any(), eq("ACTIVE"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/master-data/schools"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", matchesPattern("[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("TCT"))
                .andExpect(jsonPath("$.data[0].admissionCode").value("TCT"))
                .andExpect(jsonPath("$.data[0].name").value("Trường Đại học Cần Thơ"))
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(50))
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.meta.totalPages").value(1))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(".*\\+07:00")));
    }

    @Test
    void unauthenticatedUserCanGetSchoolsWithAliasRoute() throws Exception {
        when(masterDataService.getSchools(any(), any(), any(), eq("ACTIVE"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/api/master-data/schools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.totalItems").value(0));
    }

    @Test
    void unauthenticatedUserCanGetMajorGroups() throws Exception {
        MajorGroupResponse mockGroup = new MajorGroupResponse(
                UUID.randomUUID(),
                "1401",
                "Khoa học giáo dục",
                "14",
                "TT_74_2026",
                "KHOA HỌC GIÁO DỤC VÀ ĐÀO TẠO GIÁO VIÊN",
                true);

        when(masterDataService.getMajorGroups(any(), eq(true)))
                .thenReturn(List.of(mockGroup));

        mockMvc.perform(get("/api/v1/master-data/major-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("1401"))
                .andExpect(jsonPath("$.data[0].name").value("Khoa học giáo dục"))
                .andExpect(jsonPath("$.data[0].fieldCode").value("14"))
                .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(get("/api/master-data/major-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cacheManagerRegistersMasterDataCaches() {
        assertThat(cacheManager.getCacheNames())
                .contains(CacheConfig.CACHE_SCHOOLS, CacheConfig.CACHE_MAJOR_GROUPS);
    }
}
