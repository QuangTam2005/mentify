package vn.mentify.be.modules.user.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.AccountStatus;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.shared.api.ApiErrorCode;
import vn.mentify.be.shared.api.ApiException;

import java.util.Locale;
import java.util.UUID;

@Component
public class CurrentUserResolver {

    private final UserRepository userRepository;

    public CurrentUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return resolveFromAuthentication(authentication);
    }

    public UserEntity resolveFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ApiException(ApiErrorCode.UNAUTHENTICATED, "Yêu cầu đăng nhập");
        }

        String principalName = authentication.getName();
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
            String oauthEmail = oauth2User.getAttribute("email");
            if (oauthEmail != null && !oauthEmail.isBlank()) {
                principalName = oauthEmail;
            }
        }

        if (principalName == null || principalName.isBlank()) {
            throw new ApiException(ApiErrorCode.UNAUTHENTICATED, "Yêu cầu đăng nhập");
        }

        try {
            UUID id = UUID.fromString(principalName);
            var user = userRepository.findById(id);
            if (user.isPresent()) {
                return ensureUsable(user.get());
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, fallback to email lookup
        }

        return ensureUsable(userRepository.findByEmail(principalName.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNAUTHENTICATED, "Không tìm thấy thông tin tài khoản người dùng")));
    }

    private UserEntity ensureUsable(UserEntity user) {
        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            throw new ApiException(ApiErrorCode.FORBIDDEN, "Tài khoản không thể truy cập");
        }
        return user;
    }
}
