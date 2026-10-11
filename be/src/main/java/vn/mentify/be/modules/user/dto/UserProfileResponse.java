package vn.mentify.be.modules.user.dto;

import vn.mentify.be.modules.masterdata.dto.MajorGroupResponse;
import vn.mentify.be.modules.masterdata.dto.SchoolResponse;
import vn.mentify.be.modules.user.persistence.AccountStatus;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserProfileEntity;
import vn.mentify.be.modules.user.persistence.UserRole;
import vn.mentify.be.modules.user.persistence.UserType;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        UserType userType,
        UserRole role,
        AccountStatus accountStatus,
        String fullName,
        String bio,
        String avatarUrl,
        SchoolResponse school,
        MajorGroupResponse majorGroup,
        String customMajorName
) {
    public static UserProfileResponse from(UserEntity user, UserProfileEntity profile) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getUserType(),
                user.getRole(),
                user.getAccountStatus(),
                profile != null ? profile.getFullName() : null,
                profile != null ? profile.getBio() : null,
                profile != null ? profile.getAvatarUrl() : null,
                profile != null && profile.getSchool() != null ? SchoolResponse.from(profile.getSchool()) : null,
                profile != null && profile.getMajorGroup() != null ? MajorGroupResponse.from(profile.getMajorGroup()) : null,
                profile != null ? profile.getCustomMajorName() : null
        );
    }
}
