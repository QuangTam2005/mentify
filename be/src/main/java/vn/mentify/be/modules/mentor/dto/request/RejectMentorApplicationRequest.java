package vn.mentify.be.modules.mentor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectMentorApplicationRequest(
        @NotBlank
        @Size(max = 500)
        String rejectionReason) {
}
