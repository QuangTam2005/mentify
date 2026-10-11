package vn.mentify.be.modules.mentor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;
import vn.mentify.be.shared.audit.AuditableEntity;

import java.util.UUID;

@Entity
@Table(name = "mentor_profiles")
public class MentorProfileEntity extends AuditableEntity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mentor_status", nullable = false, length = 32)
    private MentorStatus mentorStatus = MentorStatus.NOT_APPLIED;

    @Column(name = "accepting_bookings", nullable = false)
    private boolean acceptingBookings = false;

    @Column(length = 1000)
    private String bio;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "major_group_code", length = 4)
    private String majorGroupCode;

    @Column(name = "custom_major_name", length = 255)
    private String customMajorName;

    @Column(name = "expertise_areas", length = 500)
    private String expertiseAreas;

    @Column(name = "default_meeting_link", length = 512)
    private String defaultMeetingLink;

    protected MentorProfileEntity() {
    }

    public MentorProfileEntity(UUID userId) {
        this.userId = userId;
        this.mentorStatus = MentorStatus.NOT_APPLIED;
        this.acceptingBookings = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public MentorStatus getMentorStatus() {
        return mentorStatus;
    }

    public void setMentorStatus(MentorStatus mentorStatus) {
        if (mentorStatus != null) {
            this.mentorStatus = mentorStatus;
        }
    }

    public boolean isAcceptingBookings() {
        return acceptingBookings;
    }

    public void setAcceptingBookings(boolean acceptingBookings) {
        this.acceptingBookings = acceptingBookings;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
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

    public String getExpertiseAreas() {
        return expertiseAreas;
    }

    public void setExpertiseAreas(String expertiseAreas) {
        this.expertiseAreas = expertiseAreas;
    }

    public String getDefaultMeetingLink() {
        return defaultMeetingLink;
    }

    public void setDefaultMeetingLink(String defaultMeetingLink) {
        this.defaultMeetingLink = defaultMeetingLink;
    }
}
