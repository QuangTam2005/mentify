package vn.mentify.be.modules.mentor.dto;

import java.util.UUID;

public record SubmitMentorApplicationCommand(
        UUID schoolId,
        String majorGroupCode,
        String customMajorName,
        String bio,
        String expertiseAreas,
        String defaultMeetingLink
) {
}
