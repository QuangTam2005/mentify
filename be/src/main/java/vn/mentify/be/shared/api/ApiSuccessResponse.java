package vn.mentify.be.shared.api;

import java.time.OffsetDateTime;

public record ApiSuccessResponse<T>(
        boolean success,
        T data,
        PageMetadata meta,
        OffsetDateTime timestamp,
        String requestId) {
}
