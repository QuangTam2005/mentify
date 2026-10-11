package vn.mentify.be.shared.api;

import java.util.List;

public class ApiException extends RuntimeException {

    private final ApiErrorCode errorCode;
    private final List<ApiErrorDetail> details;

    public ApiException(ApiErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public ApiException(ApiErrorCode errorCode, String message, List<ApiErrorDetail> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = List.copyOf(details);
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ApiErrorDetail> getDetails() {
        return details;
    }
}
