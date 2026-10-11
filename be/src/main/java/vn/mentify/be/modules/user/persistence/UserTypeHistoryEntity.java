package vn.mentify.be.modules.user.persistence;

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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_type_histories")
public class UserTypeHistoryEntity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_type", nullable = false, length = 32)
    private UserType oldType;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_type", nullable = false, length = 32)
    private UserType newType;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected UserTypeHistoryEntity() {
    }

    public UserTypeHistoryEntity(UserEntity user, UserType oldType, UserType newType, Instant changedAt) {
        this.user = user;
        this.oldType = oldType;
        this.newType = newType;
        this.changedAt = changedAt;
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public UserType getOldType() {
        return oldType;
    }

    public UserType getNewType() {
        return newType;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
