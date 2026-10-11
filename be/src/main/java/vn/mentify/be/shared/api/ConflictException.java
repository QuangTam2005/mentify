package vn.mentify.be.shared.api;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ApiErrorCode.CONFLICT, message);
    }
}
