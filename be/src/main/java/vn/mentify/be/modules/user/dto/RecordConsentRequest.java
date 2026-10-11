package vn.mentify.be.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecordConsentRequest(
        @NotBlank(message = "Loại consent không được để trống")
        @Size(max = 64, message = "Loại consent không được vượt quá 64 ký tự")
        String consentType,

        @NotBlank(message = "Phiên bản điều khoản không được để trống")
        @Size(max = 32, message = "Phiên bản điều khoản không được vượt quá 32 ký tự")
        String policyVersion,

        @Size(max = 128, message = "Tên người giám hộ không được vượt quá 128 ký tự")
        String guardianName,

        @Size(max = 128, message = "Thông tin liên hệ người giám hộ không được vượt quá 128 ký tự")
        String guardianContact
) {}
