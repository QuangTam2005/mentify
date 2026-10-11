package vn.mentify.be.modules.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import vn.mentify.be.modules.auth.service.AuthService;
import vn.mentify.be.modules.auth.service.OAuth2LoginCodeService;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.shared.api.ApiErrorCode;
import vn.mentify.be.shared.api.ApiException;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final OAuth2LoginCodeService loginCodeService;
    private final UserRepository userRepository;
    private final String redirectUri;

    public OAuth2LoginSuccessHandler(
            AuthService authService,
            OAuth2LoginCodeService loginCodeService,
            UserRepository userRepository,
            @Value("${app.security.oauth2.redirect-uri:http://localhost:3000/auth/callback}") String redirectUri) {
        this.authService = authService;
        this.loginCodeService = loginCodeService;
        this.userRepository = userRepository;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        UserEntity user = null;
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oauth2User = oauthToken.getPrincipal();
            String provider = oauthToken.getAuthorizedClientRegistrationId();
            HttpSession existingSession = request.getSession(false);
            Object linkUserId = existingSession != null ? existingSession.getAttribute("OAUTH_LINK_USER_ID") : null;
            if (linkUserId instanceof String value) {
                user = userRepository.findById(java.util.UUID.fromString(value))
                        .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHENTICATED, "Phiên liên kết không hợp lệ"));
                user = authService.linkOAuth2Identity(provider, oauth2User, user);
                existingSession.removeAttribute("OAUTH_LINK_USER_ID");
            } else {
                user = authService.processOAuth2Login(provider, oauth2User);
            }
        }

        if (user == null) {
            throw new ApiException(ApiErrorCode.UNAUTHENTICATED, "Không thể hoàn tất đăng nhập OAuth2");
        }

        String code = loginCodeService.issue(user);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        String callback = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code)
                .build()
                .toUriString();
        response.sendRedirect(callback);
    }
}
