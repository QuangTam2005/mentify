package vn.mentify.be.modules.auth.dto;

import jakarta.validation.constraints.Size;

public record SetPasswordRequest(
        String currentPassword,
        @Size(min = 12, max = 128, message = "Mật khẩu phải có từ 12 đến 128 ký tự")
        String newPassword) {
}
