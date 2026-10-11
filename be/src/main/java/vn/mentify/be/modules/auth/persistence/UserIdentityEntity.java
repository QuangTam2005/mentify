package vn.mentify.be.modules.auth.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import vn.mentify.be.modules.user.persistence.UserEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_identities")
public class UserIdentityEntity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "provider_email", nullable = false, length = 320)
    private String providerEmail;

    @Column(name = "provider_email_verified", nullable = false)
    private boolean providerEmailVerified;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected UserIdentityEntity() {
    }

    public UserIdentityEntity(
            UserEntity user,
            String provider,
            String providerSubject,
            String providerEmail,
            boolean providerEmailVerified,
            Instant lastLoginAt) {
        this.user = user;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.providerEmail = providerEmail;
        this.providerEmailVerified = providerEmailVerified;
        this.lastLoginAt = lastLoginAt;
    }

    public UserEntity getUser() {
        return user;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public String getProviderEmail() {
        return providerEmail;
    }

    public boolean isProviderEmailVerified() {
        return providerEmailVerified;
    }

    public void recordLogin(String providerEmail, boolean providerEmailVerified, Instant loginAt) {
        this.providerEmail = providerEmail;
        this.providerEmailVerified = providerEmailVerified;
        this.lastLoginAt = loginAt;
    }
}
