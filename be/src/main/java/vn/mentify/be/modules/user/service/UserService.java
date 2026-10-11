package vn.mentify.be.modules.user.service;

import vn.mentify.be.modules.user.dto.RecordConsentRequest;
import vn.mentify.be.modules.user.dto.UpdateProfileRequest;
import vn.mentify.be.modules.user.dto.UserConsentResponse;
import vn.mentify.be.modules.user.dto.UserProfileResponse;

import java.util.UUID;

public interface UserService {

    UserProfileResponse getProfile(UUID userId);

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    UserConsentResponse recordConsent(UUID userId, RecordConsentRequest request, String ipAddress, String userAgent);
}
