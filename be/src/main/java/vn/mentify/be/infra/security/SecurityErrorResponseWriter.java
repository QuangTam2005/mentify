package vn.mentify.be.infra.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import vn.mentify.be.infra.web.RequestIdFilter;
import vn.mentify.be.shared.api.ApiErrorCode;
import vn.mentify.be.shared.api.ApiErrorResponse;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

final class SecurityErrorResponseWriter {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ObjectMapper objectMapper;
    private final Clock clock;

    SecurityErrorResponseWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    void write(
            HttpServletRequest request,
            HttpServletResponse response,
            ApiErrorCode errorCode,
            String message) throws IOException {
        String requestId = RequestIdFilter.requestId(request);
        response.setStatus(errorCode.httpStatus().value());
        response.setHeader(RequestIdFilter.HEADER_NAME, requestId);
        if (errorCode == ApiErrorCode.UNAUTHENTICATED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = ApiErrorResponse.failure(
                errorCode,
                message,
                List.of(),
                OffsetDateTime.ofInstant(clock.instant(), BUSINESS_ZONE),
                requestId);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
