package vn.mentify.be.modules.mentor.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.mentify.be.infra.web.SuccessResponseFactory;
import vn.mentify.be.modules.mentor.dto.request.RejectMentorApplicationRequest;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationAdminDecision;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationAdminDetail;
import vn.mentify.be.modules.mentor.dto.response.MentorApplicationAdminListItem;
import vn.mentify.be.modules.mentor.service.MentorApplicationAdminService;
import vn.mentify.be.shared.api.ApiSuccessResponse;
import vn.mentify.be.shared.api.PageMetadata;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/admin/mentor/applications")
public class AdminMentorApplicationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final MentorApplicationAdminService service;
    private final SuccessResponseFactory responseFactory;

    public AdminMentorApplicationController(
            MentorApplicationAdminService service,
            SuccessResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiSuccessResponse<java.util.List<MentorApplicationAdminListItem>> listPending(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            HttpServletRequest request) {
        Page<MentorApplicationAdminListItem> results = service.listPending(PageRequest.of(
                page, size, Sort.by(Sort.Order.asc("submittedAt"), Sort.Order.asc("id"))));
        return responseFactory.success(
                results.getContent(),
                PageMetadata.of(results.getNumber(), results.getSize(), results.getTotalElements()),
                request);
    }

    @GetMapping("/{applicationId}")
    public ApiSuccessResponse<MentorApplicationAdminDetail> getDetails(
            @PathVariable UUID applicationId,
            HttpServletRequest request) {
        return responseFactory.success(service.getDetails(applicationId), request);
    }

    @PostMapping("/{applicationId}/approve")
    public ApiSuccessResponse<MentorApplicationAdminDecision> approve(
            @PathVariable UUID applicationId,
            HttpServletRequest request) {
        return responseFactory.success(service.approve(applicationId), request);
    }

    @PostMapping("/{applicationId}/reject")
    public ApiSuccessResponse<MentorApplicationAdminDecision> reject(
            @PathVariable UUID applicationId,
            @Valid @RequestBody RejectMentorApplicationRequest body,
            HttpServletRequest request) {
        return responseFactory.success(service.reject(applicationId, body.rejectionReason()), request);
    }
}
