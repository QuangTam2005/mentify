package vn.mentify.be.modules.user.persistence;

public enum UserType {
    SCHOOL_STUDENT,
    UNIVERSITY_STUDENT,
    WORKING_PROFESSIONAL;

    public boolean canTransitionTo(UserType target) {
        if (target == null) {
            return false;
        }
        return target.ordinal() >= this.ordinal();
    }
}
