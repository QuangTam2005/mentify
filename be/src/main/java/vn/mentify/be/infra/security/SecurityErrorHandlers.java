package vn.mentify.be.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import vn.mentify.be.shared.api.ApiErrorCode;

import java.io.IOException;

final class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final SecurityErrorResponseWriter responseWriter;

    SecurityErrorHandlers(SecurityErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        responseWriter.write(request, response, ApiErrorCode.UNAUTHENTICATED, "Yêu cầu xác thực");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        responseWriter.write(request, response, ApiErrorCode.FORBIDDEN, "Bạn không có quyền truy cập");
    }
}
