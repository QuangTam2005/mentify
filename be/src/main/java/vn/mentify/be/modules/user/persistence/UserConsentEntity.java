package vn.mentify.be.modules.user.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;
import vn.mentify.be.shared.audit.AuditableEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_consents")
public class UserConsentEntity extends AuditableEntity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "consent_type", nullable = false, length = 64)
    private String consentType;

    @Column(name = "policy_version", nullable = false, length = 32)
    private String policyVersion;

    @Column(name = "accepted_at", nullable = false)
    private OffsetDateTime acceptedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "guardian_name", length = 128)
    private String guardianName;

    @Column(name = "guardian_contact", length = 128)
    private String guardianContact;

    protected UserConsentEntity() {
    }

    public UserConsentEntity(
            UserEntity user,
            String consentType,
            String policyVersion,
            OffsetDateTime acceptedAt,
            String ipAddress,
            String userAgent,
            String guardianName,
            String guardianContact) {
        this.user = user;
        this.consentType = consentType;
        this.policyVersion = policyVersion;
        this.acceptedAt = acceptedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.guardianName = guardianName;
        this.guardianContact = guardianContact;
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public String getConsentType() {
        return consentType;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public OffsetDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public String getGuardianContact() {
        return guardianContact;
    }
}
