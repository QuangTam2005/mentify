package vn.mentify.be.shared.api;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        boolean success,
        ErrorBody error,
        OffsetDateTime timestamp,
        String requestId) {

    public record ErrorBody(String code, String message, List<ApiErrorDetail> details) {
    }

    public static ApiErrorResponse failure(
            ApiErrorCode code,
            String message,
            List<ApiErrorDetail> details,
            OffsetDateTime timestamp,
            String requestId) {
        return new ApiErrorResponse(
                false,
                new ErrorBody(code.name(), message, List.copyOf(details)),
                timestamp,
                requestId);
    }
}
