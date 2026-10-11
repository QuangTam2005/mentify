package vn.mentify.be.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailChangeRequest(
        @NotBlank(message = "Email mới không được để trống")
        @Email(message = "Email mới không hợp lệ")
        String newEmail,
        String currentPassword) {
}
