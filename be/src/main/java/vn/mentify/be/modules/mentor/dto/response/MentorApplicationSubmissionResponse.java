package vn.mentify.be.modules.mentor.dto.response;

import vn.mentify.be.modules.mentor.entity.MentorApplicationStatus;
import vn.mentify.be.modules.mentor.entity.MentorStatus;

import java.time.Instant;
import java.util.UUID;

public record MentorApplicationSubmissionResponse(
        UUID mentorProfileId,
        UUID mentorApplicationId,
        UUID userId,
        MentorStatus mentorStatus,
        MentorApplicationStatus applicationStatus,
        int attemptNumber,
        String appliedUserType,
        boolean acceptingBookings,
        Instant submittedAt
) {
}
