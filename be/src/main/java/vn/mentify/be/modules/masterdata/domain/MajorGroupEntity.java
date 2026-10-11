package vn.mentify.be.modules.masterdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;
import vn.mentify.be.shared.audit.AuditableEntity;

import java.util.UUID;

@Entity
@Table(name = "major_groups")
public class MajorGroupEntity extends AuditableEntity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 4)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "field_code", length = 2)
    private String fieldCode;

    @Column(name = "regulation_version", nullable = false, length = 32)
    private String regulationVersion = "TT_74_2026";

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    protected MajorGroupEntity() {
    }

    public MajorGroupEntity(
            String code,
            String name,
            String fieldCode,
            String regulationVersion,
            String description,
            boolean isActive) {
        this.code = code;
        this.name = name;
        this.fieldCode = fieldCode;
        this.regulationVersion = regulationVersion != null ? regulationVersion : "TT_74_2026";
        this.description = description;
        this.isActive = isActive;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public String getRegulationVersion() {
        return regulationVersion;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return isActive;
    }
}
