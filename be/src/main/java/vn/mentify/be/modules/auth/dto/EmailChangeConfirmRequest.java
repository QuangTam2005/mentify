package vn.mentify.be.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailChangeConfirmRequest(
        @NotBlank(message = "Token không được để trống")
        String token) {
}
