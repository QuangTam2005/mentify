package vn.mentify.be.modules.masterdata.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.mentify.be.infra.config.CacheConfig;
import vn.mentify.be.modules.masterdata.domain.CampusType;
import vn.mentify.be.modules.masterdata.dto.MajorGroupResponse;
import vn.mentify.be.modules.masterdata.dto.SchoolResponse;
import vn.mentify.be.modules.masterdata.persistence.MajorGroupRepository;
import vn.mentify.be.modules.masterdata.persistence.SchoolRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MasterDataServiceImpl implements MasterDataService {

    private final SchoolRepository schoolRepository;
    private final MajorGroupRepository majorGroupRepository;

    public MasterDataServiceImpl(
            SchoolRepository schoolRepository,
            MajorGroupRepository majorGroupRepository) {
        this.schoolRepository = schoolRepository;
        this.majorGroupRepository = majorGroupRepository;
    }

    @Override
    @Cacheable(
            value = CacheConfig.CACHE_SCHOOLS,
            key = "{#search, #region, #campusType, #status, #pageable.pageNumber, #pageable.pageSize, #pageable.sort}")
    public Page<SchoolResponse> getSchools(
            String search,
            String region,
            CampusType campusType,
            String status,
            Pageable pageable) {
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String trimmedRegion = (region != null && !region.isBlank()) ? region.trim() : null;
        String queryStatus = (status != null && !status.isBlank()) ? status.trim() : "ACTIVE";

        return schoolRepository
                .searchSchools(trimmedSearch, trimmedRegion, campusType, queryStatus, pageable)
                .map(SchoolResponse::from);
    }

    @Override
    @Cacheable(
            value = CacheConfig.CACHE_MAJOR_GROUPS,
            key = "{#search, #isActive}")
    public List<MajorGroupResponse> getMajorGroups(
            String search,
            Boolean isActive) {
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Boolean queryIsActive = isActive != null ? isActive : true;

        return majorGroupRepository
                .searchMajorGroups(trimmedSearch, queryIsActive)
                .stream()
                .map(MajorGroupResponse::from)
                .toList();
    }
}
