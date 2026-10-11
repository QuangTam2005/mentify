package vn.mentify.be.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "Token không được để trống")
        String token,
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 12, max = 128, message = "Mật khẩu phải có từ 12 đến 128 ký tự")
        String newPassword) {
}
