-- ============================================================================
-- Flyway Migration: V8__create_mentor_profiles_and_applications.sql
-- Tables: mentor_profiles, mentor_applications
-- ============================================================================

CREATE TABLE mentor_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    mentor_status VARCHAR(32) NOT NULL DEFAULT 'NOT_APPLIED',
    accepting_bookings BOOLEAN NOT NULL DEFAULT FALSE,
    bio VARCHAR(1000),
    school_id UUID,
    major_group_code VARCHAR(4),
    custom_major_name VARCHAR(255),
    expertise_areas VARCHAR(500),
    default_meeting_link VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_mentor_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_mentor_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_mentor_profiles_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE SET NULL,
    CONSTRAINT fk_mentor_profiles_major_group FOREIGN KEY (major_group_code) REFERENCES major_groups(code) ON DELETE SET NULL,
    CONSTRAINT ck_mentor_profiles_status CHECK (mentor_status IN ('NOT_APPLIED', 'PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'))
);

CREATE INDEX idx_mentor_profiles_status ON mentor_profiles(mentor_status);
CREATE INDEX idx_mentor_profiles_school_id ON mentor_profiles(school_id);
CREATE INDEX idx_mentor_profiles_major_group_code ON mentor_profiles(major_group_code);

CREATE TABLE mentor_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentor_profile_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_number INT NOT NULL DEFAULT 1,
    applied_user_type VARCHAR(32) NOT NULL,
    school_id UUID,
    major_group_code VARCHAR(4),
    custom_major_name VARCHAR(255),
    bio VARCHAR(1000),
    rejection_reason VARCHAR(500),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_mentor_applications_attempt UNIQUE (mentor_profile_id, attempt_number),
    CONSTRAINT fk_mentor_applications_profile FOREIGN KEY (mentor_profile_id) REFERENCES mentor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_mentor_applications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_mentor_applications_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE SET NULL,
    CONSTRAINT fk_mentor_applications_major_group FOREIGN KEY (major_group_code) REFERENCES major_groups(code) ON DELETE SET NULL,
    CONSTRAINT ck_mentor_applications_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_mentor_applications_user_type CHECK (applied_user_type IN ('UNIVERSITY_STUDENT', 'WORKING_PROFESSIONAL')),
    CONSTRAINT ck_mentor_applications_attempt_positive CHECK (attempt_number > 0)
);

CREATE INDEX idx_mentor_applications_profile_id ON mentor_applications(mentor_profile_id);
CREATE INDEX idx_mentor_applications_user_id ON mentor_applications(user_id);
CREATE INDEX idx_mentor_applications_status ON mentor_applications(status);
CREATE INDEX idx_mentor_applications_submitted_at ON mentor_applications(submitted_at);
