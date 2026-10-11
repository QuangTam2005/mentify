package vn.mentify.be.modules.mentor.dto.request;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubmitMentorApplicationRequest(
        UUID schoolId,

        @Size(max = 4, message = "Mã nhóm ngành không được vượt quá 4 ký tự")
        String majorGroupCode,

        @Size(max = 255, message = "Tên ngành đào tạo không được vượt quá 255 ký tự")
        String customMajorName,

        @Size(max = 1000, message = "Tiểu sử không được vượt quá 1000 ký tự")
        String bio,

        @Size(max = 500, message = "Lĩnh vực chuyên môn không được vượt quá 500 ký tự")
        String expertiseAreas,

        @Size(max = 512, message = "Liên kết phòng họp mặc định không được vượt quá 512 ký tự")
        String defaultMeetingLink
) {
}
