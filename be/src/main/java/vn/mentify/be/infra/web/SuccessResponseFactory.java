package vn.mentify.be.infra.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import vn.mentify.be.shared.api.ApiSuccessResponse;
import vn.mentify.be.shared.api.PageMetadata;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Component
public class SuccessResponseFactory {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Clock clock;

    public SuccessResponseFactory(Clock clock) {
        this.clock = clock;
    }

    public <T> ApiSuccessResponse<T> success(T data, HttpServletRequest request) {
        return success(data, null, request);
    }

    public <T> ApiSuccessResponse<T> success(
            T data,
            PageMetadata meta,
            HttpServletRequest request) {
        return success(data, meta, RequestIdFilter.requestId(request));
    }

    public <T> ApiSuccessResponse<T> success(T data, PageMetadata meta, String requestId) {
        OffsetDateTime timestamp = OffsetDateTime.ofInstant(clock.instant(), BUSINESS_ZONE);
        String effectiveRequestId = requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString()
                : requestId;
        return new ApiSuccessResponse<>(true, data, meta, timestamp, effectiveRequestId);
    }
}
