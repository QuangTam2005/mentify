package vn.mentify.be.infra.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import vn.mentify.be.shared.api.ApiErrorCode;
import vn.mentify.be.shared.api.ApiErrorDetail;
import vn.mentify.be.shared.api.ApiErrorResponse;
import vn.mentify.be.shared.api.ApiException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<ApiErrorDetail> details = new ArrayList<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            details.add(new ApiErrorDetail(error.getField(), defaultMessage(error)));
        }
        for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
            details.add(new ApiErrorDetail("_global", defaultMessage(error)));
        }
        return validationResponse(details, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request) {
        List<ApiErrorDetail> details = new ArrayList<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors errors) {
                for (FieldError error : errors.getFieldErrors()) {
                    details.add(new ApiErrorDetail(error.getField(), defaultMessage(error)));
                }
                for (ObjectError error : errors.getGlobalErrors()) {
                    details.add(new ApiErrorDetail("_global", defaultMessage(error)));
                }
            } else {
                String field = result.getMethodParameter().getParameterName();
                String message = result.getResolvableErrors().stream()
                        .map(this::defaultMessage)
                        .findFirst()
                        .orElse("Giá trị không hợp lệ");
                details.add(new ApiErrorDetail(field == null ? "_parameter" : field, message));
            }
        }
        return validationResponse(details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<ApiErrorDetail> details = exception.getConstraintViolations().stream()
                .map(this::toDetail)
                .sorted(Comparator.comparing(ApiErrorDetail::field)
                        .thenComparing(ApiErrorDetail::message))
                .toList();
        return validationResponse(details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return validationResponse(List.of(), request);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        return response(exception.getErrorCode(), exception.getMessage(), exception.getDetails(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> handleMissingResource(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        return response(ApiErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài nguyên", List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        String requestId = RequestIdFilter.requestId(request);
        log.error("Unhandled API exception requestId={}", requestId, exception);
        return response(ApiErrorCode.INTERNAL_ERROR, "Đã xảy ra lỗi hệ thống", List.of(), request);
    }

    private ResponseEntity<ApiErrorResponse> validationResponse(
            List<ApiErrorDetail> details,
            HttpServletRequest request) {
        List<ApiErrorDetail> sortedDetails = details.stream()
                .sorted(Comparator.comparing(ApiErrorDetail::field)
                        .thenComparing(ApiErrorDetail::message))
                .toList();
        return response(ApiErrorCode.VALIDATION_ERROR, "Dữ liệu không hợp lệ", sortedDetails, request);
    }

    private ResponseEntity<ApiErrorResponse> response(
            ApiErrorCode code,
            String message,
            List<ApiErrorDetail> details,
            HttpServletRequest request) {
        String requestId = RequestIdFilter.requestId(request);
        OffsetDateTime timestamp = OffsetDateTime.ofInstant(clock.instant(), BUSINESS_ZONE);
        return ResponseEntity.status(code.httpStatus())
                .body(ApiErrorResponse.failure(code, message, details, timestamp, requestId));
    }

    private ApiErrorDetail toDetail(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastSeparator = path.lastIndexOf('.');
        String field = lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
        return new ApiErrorDetail(field, violation.getMessage());
    }

    private String defaultMessage(ObjectError error) {
        return defaultMessage((MessageSourceResolvable) error);
    }

    private String defaultMessage(MessageSourceResolvable error) {
        return error.getDefaultMessage() == null ? "Giá trị không hợp lệ" : error.getDefaultMessage();
    }
}
