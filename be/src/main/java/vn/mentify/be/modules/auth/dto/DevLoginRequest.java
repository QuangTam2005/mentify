package vn.mentify.be.modules.auth.dto;

import jakarta.validation.constraints.Email;
import vn.mentify.be.modules.user.persistence.UserRole;
import vn.mentify.be.modules.user.persistence.UserType;

public record DevLoginRequest(
        @Email(message = "Email không hợp lệ")
        String email,

        UserType userType,

        UserRole role,

        String fullName
) {}
