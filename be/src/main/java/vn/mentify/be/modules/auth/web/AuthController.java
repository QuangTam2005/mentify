package vn.mentify.be.modules.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import vn.mentify.be.infra.web.SuccessResponseFactory;
import vn.mentify.be.modules.auth.dto.AuthMeResponse;
import vn.mentify.be.modules.auth.dto.AuthTokenResponse;
import vn.mentify.be.modules.auth.dto.EmailChangeConfirmRequest;
import vn.mentify.be.modules.auth.dto.EmailChangeRequest;
import vn.mentify.be.modules.auth.dto.ForgotPasswordRequest;
import vn.mentify.be.modules.auth.dto.OAuth2CodeExchangeRequest;
import vn.mentify.be.modules.auth.dto.PasswordLoginRequest;
import vn.mentify.be.modules.auth.dto.PasswordResetRequest;
import vn.mentify.be.modules.auth.dto.SetPasswordRequest;
import vn.mentify.be.modules.auth.security.JwtAccessToken;
import vn.mentify.be.modules.auth.security.JwtTokenService;
import vn.mentify.be.modules.auth.service.AuthService;
import vn.mentify.be.modules.auth.service.OAuth2LoginCodeService;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.service.CurrentUserResolver;
import vn.mentify.be.shared.api.ApiSuccessResponse;
import vn.mentify.be.shared.api.ApiErrorCode;
import vn.mentify.be.shared.api.ApiException;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
public class AuthController {

    private final AuthService authService;
    private final CurrentUserResolver currentUserResolver;
    private final SuccessResponseFactory responseFactory;
    private final ObjectProvider<JwtTokenService> jwtTokenServiceProvider;
    private final OAuth2LoginCodeService loginCodeService;

    public AuthController(
            AuthService authService,
            CurrentUserResolver currentUserResolver,
            SuccessResponseFactory responseFactory,
            ObjectProvider<JwtTokenService> jwtTokenServiceProvider,
            OAuth2LoginCodeService loginCodeService) {
        this.authService = authService;
        this.currentUserResolver = currentUserResolver;
        this.responseFactory = responseFactory;
        this.jwtTokenServiceProvider = jwtTokenServiceProvider;
        this.loginCodeService = loginCodeService;
    }

    @GetMapping("/me")
    public ApiSuccessResponse<AuthMeResponse> me(HttpServletRequest request) {
        UserEntity currentUser = currentUserResolver.resolveCurrentUser();
        AuthMeResponse response = authService.getCurrentUserMe(currentUser);
        return responseFactory.success(response, request);
    }

    @PostMapping("/logout")
    public ApiSuccessResponse<Void> logout(HttpServletRequest request) {
        return responseFactory.success(null, request);
    }

    @PostMapping("/password/login")
    public ApiSuccessResponse<AuthMeResponse> passwordLogin(
            @Valid @RequestBody PasswordLoginRequest loginRequest,
            HttpServletRequest request) {
        UserEntity user = authService.passwordLogin(loginRequest);
        return responseFactory.success(loginResponse(user), request);
    }

    @PostMapping("/oauth2/exchange")
    public ApiSuccessResponse<AuthMeResponse> exchangeOAuth2Code(
            @Valid @RequestBody OAuth2CodeExchangeRequest exchangeRequest,
            HttpServletRequest request) {
        UserEntity user = loginCodeService.consume(exchangeRequest.code());
        return responseFactory.success(loginResponse(user), request);
    }

    @PutMapping("/password")
    public ApiSuccessResponse<Void> setOrChangePassword(
            @Valid @RequestBody SetPasswordRequest passwordRequest,
            HttpServletRequest request) {
        UserEntity user = currentUserResolver.resolveCurrentUser();
        authService.setOrChangePassword(user.getId(), passwordRequest);
        return responseFactory.success(null, request);
    }

    @PostMapping("/password/forgot")
    public ApiSuccessResponse<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest forgotRequest,
            HttpServletRequest request) {
        authService.requestPasswordReset(forgotRequest);
        return responseFactory.success(null, request);
    }

    @PostMapping("/password/reset")
    public ApiSuccessResponse<Void> resetPassword(
            @Valid @RequestBody PasswordResetRequest resetRequest,
            HttpServletRequest request) {
        authService.resetPassword(resetRequest);
        return responseFactory.success(null, request);
    }

    @PostMapping("/email-change")
    public ApiSuccessResponse<Void> requestEmailChange(
            @Valid @RequestBody EmailChangeRequest emailRequest,
            HttpServletRequest request) {
        UserEntity user = currentUserResolver.resolveCurrentUser();
        authService.requestEmailChange(user.getId(), emailRequest);
        return responseFactory.success(null, request);
    }

    @PostMapping("/email-change/confirm")
    public ApiSuccessResponse<Void> confirmEmailChange(
            @Valid @RequestBody EmailChangeConfirmRequest confirmRequest,
            HttpServletRequest request) {
        authService.confirmEmailChange(confirmRequest.token());
        return responseFactory.success(null, request);
    }

    @GetMapping("/google/link")
    public void startGoogleLink(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        UserEntity user = currentUserResolver.resolveCurrentUser();
        request.getSession(true).setAttribute("OAUTH_LINK_USER_ID", user.getId().toString());
        response.sendRedirect("/oauth2/authorization/google");
    }

    private AuthMeResponse loginResponse(UserEntity user) {
        AuthMeResponse response = authService.getCurrentUserMe(user);
        JwtTokenService jwtTokenService = jwtTokenServiceProvider.getIfAvailable();
        if (jwtTokenService == null) {
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR, "JWT authentication chưa được cấu hình");
        }
        JwtAccessToken token = jwtTokenService.issueAccessToken(user);
        return response.withToken(new AuthTokenResponse(
                token.value(),
                token.tokenType(),
                token.expiresAt()));
    }
}
