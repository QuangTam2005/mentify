package vn.mentify.be.modules.user.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;
import vn.mentify.be.shared.api.ConflictException;
import vn.mentify.be.shared.audit.AuditableEntity;

import java.util.Locale;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserEntity extends AuditableEntity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 32)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 32)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    protected UserEntity() {
    }

    public UserEntity(String email, UserType userType) {
        this.email = email;
        this.userType = userType;
    }

    @PrePersist
    void normalizeBeforeWrite() {
        normalizeEmail();
    }

    @PreUpdate
    void updateAuditTimestamp() {
        normalizeEmail();
    }

    private void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }

    public void changeEmail(String email) {
        this.email = email;
        normalizeEmail();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public UserType getUserType() {
        return userType;
    }

    public UserRole getRole() {
        return role;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public void setPasswordHash(String passwordHash, Instant changedAt) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = changedAt;
    }

    public void markEmailVerified(Instant verifiedAt) {
        this.emailVerifiedAt = verifiedAt;
    }

    public boolean canLogin() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    public void promoteUserType(UserType newType) {
        if (newType == null || newType == this.userType) {
            return;
        }
        if (!this.userType.canTransitionTo(newType)) {
            throw new ConflictException("Không thể chuyển đổi lùi loại người dùng từ " + this.userType + " sang " + newType);
        }
        this.userType = newType;
    }

    public void setRole(UserRole role) {
        if (role != null) {
            this.role = role;
        }
    }

    public void setDevUserType(UserType userType) {
        if (userType != null) {
            this.userType = userType;
        }
    }

}
