package vn.mentify.be.modules.mentor.dto;

import vn.mentify.be.modules.mentor.domain.MentorApplicationStatus;
import vn.mentify.be.modules.mentor.domain.MentorStatus;

import java.time.Instant;
import java.util.UUID;

public record MentorApplicationSubmissionResult(
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
