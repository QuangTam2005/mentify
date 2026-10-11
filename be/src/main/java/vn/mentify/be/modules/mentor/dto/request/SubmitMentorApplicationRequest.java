package vn.mentify.be.modules.mentor.dto.request;

import java.util.UUID;

public record SubmitMentorApplicationRequest(
        UUID schoolId,
        String majorGroupCode,
        String customMajorName,
        String bio,
        String expertiseAreas,
        String defaultMeetingLink
) {
}
