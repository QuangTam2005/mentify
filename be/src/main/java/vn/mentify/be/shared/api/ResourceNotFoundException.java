package vn.mentify.be.shared.api;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(ApiErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
