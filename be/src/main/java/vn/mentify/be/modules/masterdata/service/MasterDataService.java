package vn.mentify.be.modules.masterdata.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.mentify.be.modules.masterdata.domain.CampusType;
import vn.mentify.be.modules.masterdata.dto.MajorGroupResponse;
import vn.mentify.be.modules.masterdata.dto.SchoolResponse;

import java.util.List;

public interface MasterDataService {

    Page<SchoolResponse> getSchools(
            String search,
            String region,
            CampusType campusType,
            String status,
            Pageable pageable);

    List<MajorGroupResponse> getMajorGroups(
            String search,
            Boolean isActive);
}
