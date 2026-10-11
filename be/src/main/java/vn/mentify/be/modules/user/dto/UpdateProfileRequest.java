package vn.mentify.be.modules.user.dto;

import jakarta.validation.constraints.Size;
import vn.mentify.be.modules.user.persistence.UserType;

import java.util.UUID;

public record UpdateProfileRequest(
        @Size(max = 128, message = "Họ tên không được vượt quá 128 ký tự")
        String fullName,

        @Size(max = 1000, message = "Tiểu sử không được vượt quá 1000 ký tự")
        String bio,

        @Size(max = 512, message = "Avatar URL không được vượt quá 512 ký tự")
        String avatarUrl,

        UUID schoolId,

        @Size(max = 4, message = "Mã nhóm ngành không được vượt quá 4 ký tự")
        String majorGroupCode,

        @Size(max = 255, message = "Tên ngành đào tạo không được vượt quá 255 ký tự")
        String customMajorName,

        UserType newUserType
) {}
