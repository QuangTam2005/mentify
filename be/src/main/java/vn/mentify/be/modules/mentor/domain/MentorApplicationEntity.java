package vn.mentify.be.modules.mentor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;
import vn.mentify.be.shared.audit.AuditableEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mentor_applications")
public class MentorApplicationEntity extends AuditableEntity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "mentor_profile_id", nullable = false)
    private UUID mentorProfileId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MentorApplicationStatus status = MentorApplicationStatus.PENDING;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @Column(name = "applied_user_type", nullable = false, length = 32)
    private String appliedUserType;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "major_group_code", length = 4)
    private String majorGroupCode;

    @Column(name = "custom_major_name", length = 255)
    private String customMajorName;

    @Column(length = 1000)
    private String bio;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected MentorApplicationEntity() {
    }

    public MentorApplicationEntity(
            UUID mentorProfileId,
            UUID userId,
            String appliedUserType,
            int attemptNumber) {
        this.mentorProfileId = mentorProfileId;
        this.userId = userId;
        this.appliedUserType = appliedUserType;
        this.attemptNumber = attemptNumber;
        this.status = MentorApplicationStatus.PENDING;
        this.submittedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getMentorProfileId() {
        return mentorProfileId;
    }

    public UUID getUserId() {
        return userId;
    }

    public MentorApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(MentorApplicationStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getAppliedUserType() {
        return appliedUserType;
    }

    public void setAppliedUserType(String appliedUserType) {
        this.appliedUserType = appliedUserType;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public String getMajorGroupCode() {
        return majorGroupCode;
    }

    public void setMajorGroupCode(String majorGroupCode) {
        this.majorGroupCode = majorGroupCode;
    }

    public String getCustomMajorName() {
        return customMajorName;
    }

    public void setCustomMajorName(String customMajorName) {
        this.customMajorName = customMajorName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        if (submittedAt != null) {
            this.submittedAt = submittedAt;
        }
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
