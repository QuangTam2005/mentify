package vn.mentify.be;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.infra.config.CacheConfig;
import vn.mentify.be.modules.masterdata.persistence.MajorGroupRepository;
import vn.mentify.be.modules.masterdata.persistence.SchoolRepository;
import vn.mentify.be.modules.masterdata.service.MasterDataService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MasterDataCacheTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mentify")
            .withUsername("mentify")
            .withPassword("mentify_test");

    @Autowired
    private MasterDataService masterDataService;

    @Autowired
    private CacheManager cacheManager;

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

    @BeforeEach
    void clearCaches() {
        var schoolsCache = cacheManager.getCache(CacheConfig.CACHE_SCHOOLS);
        if (schoolsCache != null) {
            schoolsCache.clear();
        }
        var majorsCache = cacheManager.getCache(CacheConfig.CACHE_MAJOR_GROUPS);
        if (majorsCache != null) {
            majorsCache.clear();
        }
    }

    @Test
    void getSchoolsUsesCacheOnSubsequentCalls() {
        when(schoolRepository.searchSchools(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        PageRequest pageRequest = PageRequest.of(0, 50);

        // First call - cache miss, should hit repo
        masterDataService.getSchools("tech", "Hà Nội", null, "ACTIVE", pageRequest);
        verify(schoolRepository, times(1)).searchSchools("tech", "Hà Nội", null, "ACTIVE", pageRequest);

        // Second call with same arguments - cache hit, should NOT hit repo again
        masterDataService.getSchools("tech", "Hà Nội", null, "ACTIVE", pageRequest);
        verify(schoolRepository, times(1)).searchSchools("tech", "Hà Nội", null, "ACTIVE", pageRequest);
    }

    @Test
    void getMajorGroupsUsesCacheOnSubsequentCalls() {
        when(majorGroupRepository.searchMajorGroups(any(), any()))
                .thenReturn(List.of());

        // First call - cache miss
        masterDataService.getMajorGroups("CNTT", true);
        verify(majorGroupRepository, times(1)).searchMajorGroups("CNTT", true);

        // Second call - cache hit
        masterDataService.getMajorGroups("CNTT", true);
        verify(majorGroupRepository, times(1)).searchMajorGroups("CNTT", true);
    }
}
