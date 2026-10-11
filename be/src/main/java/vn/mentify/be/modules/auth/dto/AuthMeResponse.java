package vn.mentify.be.modules.auth.dto;

import vn.mentify.be.modules.auth.domain.MentorStatus;
import vn.mentify.be.modules.user.persistence.AccountStatus;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserProfileEntity;
import vn.mentify.be.modules.user.persistence.UserRole;
import vn.mentify.be.modules.user.persistence.UserType;

import java.util.UUID;

public record AuthMeResponse(
        UUID id,
        String email,
        UserType userType,
        UserRole role,
        AccountStatus accountStatus,
        MentorStatus mentorStatus,
        boolean acceptingBookings,
        String fullName,
        String avatarUrl,
        AuthTokenResponse token
) {
    public static AuthMeResponse from(UserEntity user, UserProfileEntity profile, MentorStatus mentorStatus, boolean acceptingBookings) {
        return new AuthMeResponse(
                user.getId(),
                user.getEmail(),
                user.getUserType(),
                user.getRole(),
                user.getAccountStatus(),
                mentorStatus != null ? mentorStatus : MentorStatus.NOT_APPLIED,
                acceptingBookings,
                profile != null ? profile.getFullName() : null,
                profile != null ? profile.getAvatarUrl() : null,
                null
        );
    }

    public AuthMeResponse withToken(AuthTokenResponse token) {
        return new AuthMeResponse(
                id,
                email,
                userType,
                role,
                accountStatus,
                mentorStatus,
                acceptingBookings,
                fullName,
                avatarUrl,
                token
        );
    }
}
