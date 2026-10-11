package vn.mentify.be.modules.auth.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.shared.audit.AuditableEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_action_tokens")
public class AccountActionTokenEntity extends AuditableEntity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountActionTokenPurpose purpose;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "requested_email", length = 320)
    private String requestedEmail;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected AccountActionTokenEntity() {
    }

    public AccountActionTokenEntity(
            UserEntity user,
            AccountActionTokenPurpose purpose,
            String tokenHash,
            String requestedEmail,
            Instant expiresAt) {
        this.user = user;
        this.purpose = purpose;
        this.tokenHash = tokenHash;
        this.requestedEmail = requestedEmail;
        this.expiresAt = expiresAt;
    }

    public UserEntity getUser() {
        return user;
    }

    public AccountActionTokenPurpose getPurpose() {
        return purpose;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getRequestedEmail() {
        return requestedEmail;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public boolean isUsableAt(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(Instant usedAt) {
        this.usedAt = usedAt;
    }
}
