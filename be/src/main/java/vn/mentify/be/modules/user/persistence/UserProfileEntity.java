package vn.mentify.be.modules.user.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;
import vn.mentify.be.modules.masterdata.domain.MajorGroupEntity;
import vn.mentify.be.modules.masterdata.domain.SchoolEntity;
import vn.mentify.be.shared.audit.AuditableEntity;

import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity extends AuditableEntity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "full_name", length = 128)
    private String fullName;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private SchoolEntity school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_group_code", referencedColumnName = "code")
    private MajorGroupEntity majorGroup;

    @Column(name = "custom_major_name", length = 255)
    private String customMajorName;

    protected UserProfileEntity() {
    }

    public UserProfileEntity(UserEntity user) {
        this.user = user;
    }

    public void update(
            String fullName,
            String bio,
            String avatarUrl,
            SchoolEntity school,
            MajorGroupEntity majorGroup,
            String customMajorName) {
        this.fullName = fullName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.school = school;
        this.majorGroup = majorGroup;
        this.customMajorName = customMajorName;
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public String getFullName() {
        return fullName;
    }

    public String getBio() {
        return bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public SchoolEntity getSchool() {
        return school;
    }

    public MajorGroupEntity getMajorGroup() {
        return majorGroup;
    }

    public String getCustomMajorName() {
        return customMajorName;
    }
}
