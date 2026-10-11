package vn.mentify.be.modules.mentor.entity;

/**
 * Lifecycle status of a mentor profile.
 * Documented in mvp-contracts.md and business-description.md.
 */
public enum MentorStatus {
    NOT_APPLIED,
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED
}
