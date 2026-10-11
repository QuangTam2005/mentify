package vn.mentify.be.modules.user.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.mentify.be.infra.web.SuccessResponseFactory;
import vn.mentify.be.modules.user.dto.RecordConsentRequest;
import vn.mentify.be.modules.user.dto.UpdateProfileRequest;
import vn.mentify.be.modules.user.dto.UserConsentResponse;
import vn.mentify.be.modules.user.dto.UserProfileResponse;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.service.CurrentUserResolver;
import vn.mentify.be.modules.user.service.UserService;
import vn.mentify.be.shared.api.ApiSuccessResponse;

@RestController
@RequestMapping({"/api/v1/users", "/api/users"})
public class UserController {

    private final UserService userService;
    private final CurrentUserResolver currentUserResolver;
    private final SuccessResponseFactory responseFactory;

    public UserController(
            UserService userService,
            CurrentUserResolver currentUserResolver,
            SuccessResponseFactory responseFactory) {
        this.userService = userService;
        this.currentUserResolver = currentUserResolver;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/profile")
    public ApiSuccessResponse<UserProfileResponse> getProfile(HttpServletRequest request) {
        UserEntity currentUser = currentUserResolver.resolveCurrentUser();
        UserProfileResponse response = userService.getProfile(currentUser.getId());
        return responseFactory.success(response, request);
    }

    @PutMapping("/profile")
    public ApiSuccessResponse<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest updateRequest,
            HttpServletRequest request) {
        UserEntity currentUser = currentUserResolver.resolveCurrentUser();
        UserProfileResponse response = userService.updateProfile(currentUser.getId(), updateRequest);
        return responseFactory.success(response, request);
    }

    @PostMapping("/consent")
    public ApiSuccessResponse<UserConsentResponse> recordConsent(
            @Valid @RequestBody RecordConsentRequest consentRequest,
            HttpServletRequest request) {
        UserEntity currentUser = currentUserResolver.resolveCurrentUser();
        String ipAddress = extractIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        UserConsentResponse response = userService.recordConsent(
                currentUser.getId(),
                consentRequest,
                ipAddress,
                userAgent
        );
        return responseFactory.success(response, request);
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
