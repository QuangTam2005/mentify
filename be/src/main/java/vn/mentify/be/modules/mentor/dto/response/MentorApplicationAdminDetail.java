package vn.mentify.be.modules.mentor.dto.response;

import vn.mentify.be.modules.mentor.entity.MentorApplicationStatus;
import vn.mentify.be.modules.mentor.entity.MentorStatus;

import java.time.Instant;
import java.util.UUID;

public record MentorApplicationAdminDetail(
        UUID applicationId,
        UUID mentorProfileId,
        UUID userId,
        String email,
        String applicantName,
        String appliedUserType,
        UUID schoolId,
        String majorGroupCode,
        String customMajorName,
        String bio,
        String expertiseAreas,
        String defaultMeetingLink,
        int attemptNumber,
        MentorApplicationStatus applicationStatus,
        MentorStatus mentorStatus,
        boolean acceptingBookings,
        Instant submittedAt,
        Instant reviewedAt,
        String rejectionReason) {
}
