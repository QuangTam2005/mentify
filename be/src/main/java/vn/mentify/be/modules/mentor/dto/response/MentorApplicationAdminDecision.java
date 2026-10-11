package vn.mentify.be.modules.mentor.dto.response;

import vn.mentify.be.modules.mentor.entity.MentorApplicationStatus;
import vn.mentify.be.modules.mentor.entity.MentorStatus;

import java.time.Instant;
import java.util.UUID;

public record MentorApplicationAdminDecision(
        UUID applicationId,
        UUID mentorProfileId,
        UUID userId,
        MentorApplicationStatus applicationStatus,
        MentorStatus mentorStatus,
        int attemptNumber,
        boolean acceptingBookings,
        Instant reviewedAt,
        String rejectionReason) {
}
