package vn.mentify.be.modules.masterdata.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.mentify.be.infra.web.SuccessResponseFactory;
import vn.mentify.be.modules.masterdata.domain.CampusType;
import vn.mentify.be.modules.masterdata.dto.MajorGroupResponse;
import vn.mentify.be.modules.masterdata.dto.SchoolResponse;
import vn.mentify.be.modules.masterdata.service.MasterDataService;
import vn.mentify.be.shared.api.ApiSuccessResponse;
import vn.mentify.be.shared.api.PageMetadata;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/master-data", "/api/master-data"})
public class MasterDataController {

    private final MasterDataService masterDataService;
    private final SuccessResponseFactory responseFactory;

    public MasterDataController(
            MasterDataService masterDataService,
            SuccessResponseFactory responseFactory) {
        this.masterDataService = masterDataService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/schools")
    public ApiSuccessResponse<List<SchoolResponse>> getSchools(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) CampusType campusType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {
        int effectivePage = Math.max(page, 0);
        int effectiveSize = (size <= 0 || size > 200) ? 50 : size;

        Page<SchoolResponse> schoolPage = masterDataService.getSchools(
                search,
                region,
                campusType,
                "ACTIVE",
                PageRequest.of(effectivePage, effectiveSize, Sort.by("name").ascending()));

        PageMetadata metadata = PageMetadata.of(
                effectivePage,
                effectiveSize,
                schoolPage.getTotalElements());

        return responseFactory.success(schoolPage.getContent(), metadata, request);
    }

    @GetMapping("/major-groups")
    public ApiSuccessResponse<List<MajorGroupResponse>> getMajorGroups(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "true") Boolean isActive,
            HttpServletRequest request) {
        List<MajorGroupResponse> majorGroups = masterDataService.getMajorGroups(search, isActive);
        return responseFactory.success(majorGroups, request);
    }
}
