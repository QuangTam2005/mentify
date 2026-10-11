package vn.mentify.be.modules.mentor.dto.response;

import vn.mentify.be.modules.mentor.entity.MentorApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record MentorApplicationAdminListItem(
        UUID applicationId,
        UUID mentorProfileId,
        UUID userId,
        String email,
        String applicantName,
        MentorApplicationStatus applicationStatus,
        int attemptNumber,
        String appliedUserType,
        Instant submittedAt) {
}
