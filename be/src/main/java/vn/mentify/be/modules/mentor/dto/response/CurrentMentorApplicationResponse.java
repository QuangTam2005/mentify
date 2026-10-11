package vn.mentify.be.modules.mentor.dto.response;

import vn.mentify.be.modules.mentor.entity.MentorApplicationStatus;
import vn.mentify.be.modules.mentor.entity.MentorStatus;

import java.time.Instant;
import java.util.UUID;

/** Applicant-visible snapshot of the current mentor application. */
public record CurrentMentorApplicationResponse(
        UUID mentorProfileId,
        UUID mentorApplicationId,
        MentorStatus mentorStatus,
        MentorApplicationStatus applicationStatus,
        int attemptNumber,
        String appliedUserType,
        UUID schoolId,
        String majorGroupCode,
        String customMajorName,
        String bio,
        boolean acceptingBookings,
        Instant submittedAt
) {
}
