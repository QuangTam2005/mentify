package vn.mentify.be.modules.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.mentify.be.infra.web.SuccessResponseFactory;
import vn.mentify.be.modules.auth.dto.AuthMeResponse;
import vn.mentify.be.modules.auth.dto.AuthTokenResponse;
import vn.mentify.be.modules.auth.dto.DevLoginRequest;
import vn.mentify.be.modules.auth.service.AuthService;
import vn.mentify.be.modules.auth.security.JwtAccessToken;
import vn.mentify.be.modules.auth.security.JwtTokenService;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.shared.api.ApiSuccessResponse;

@RestController
@Profile({"local", "dev", "test"})
@RequestMapping({"/api/v1/auth", "/api/auth"})
public class DevAuthController {

    private final AuthService authService;
    private final SuccessResponseFactory responseFactory;
    private final ObjectProvider<JwtTokenService> jwtTokenServiceProvider;

    public DevAuthController(
            AuthService authService,
            SuccessResponseFactory responseFactory,
            ObjectProvider<JwtTokenService> jwtTokenServiceProvider) {
        this.authService = authService;
        this.responseFactory = responseFactory;
        this.jwtTokenServiceProvider = jwtTokenServiceProvider;
    }

    @PostMapping("/dev-login")
    public ApiSuccessResponse<AuthMeResponse> devLogin(
            @Valid @RequestBody(required = false) DevLoginRequest request,
            HttpServletRequest httpRequest) {
        DevLoginRequest effectiveRequest = request != null ? request : new DevLoginRequest(null, null, null, null);
        UserEntity user = authService.devLogin(effectiveRequest);

        AuthMeResponse response = authService.getCurrentUserMe(user);
        JwtTokenService jwtTokenService = jwtTokenServiceProvider.getIfAvailable();
        if (jwtTokenService == null) {
            throw new vn.mentify.be.shared.api.ApiException(
                    vn.mentify.be.shared.api.ApiErrorCode.INTERNAL_ERROR,
                    "JWT authentication chưa được cấu hình");
        }
        JwtAccessToken token = jwtTokenService.issueAccessToken(user);
        response = response.withToken(new AuthTokenResponse(
                token.value(),
                token.tokenType(),
                token.expiresAt()));
        return responseFactory.success(response, httpRequest);
    }
}
