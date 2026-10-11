package vn.mentify.be.modules.masterdata.domain;

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
import vn.mentify.be.shared.audit.AuditableEntity;

import java.util.UUID;

@Entity
@Table(name = "schools")
public class SchoolEntity extends AuditableEntity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "admission_code", nullable = false, length = 32)
    private String admissionCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "short_name", length = 128)
    private String shortName;

    @Column(name = "common_abbreviation", length = 64)
    private String commonAbbreviation;

    @Enumerated(EnumType.STRING)
    @Column(name = "campus_type", nullable = false, length = 32)
    private CampusType campusType = CampusType.MAIN_CAMPUS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_school_id")
    private SchoolEntity parentSchool;

    @Column(nullable = false, length = 128)
    private String region;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "data_version", nullable = false, length = 32)
    private String dataVersion = "2026";

    protected SchoolEntity() {
    }

    public SchoolEntity(
            String code,
            String admissionCode,
            String name,
            String shortName,
            String commonAbbreviation,
            CampusType campusType,
            SchoolEntity parentSchool,
            String region,
            String status,
            String dataVersion) {
        this.code = code;
        this.admissionCode = admissionCode;
        this.name = name;
        this.shortName = shortName;
        this.commonAbbreviation = commonAbbreviation;
        this.campusType = campusType != null ? campusType : CampusType.MAIN_CAMPUS;
        this.parentSchool = parentSchool;
        this.region = region;
        this.status = status != null ? status : "ACTIVE";
        this.dataVersion = dataVersion != null ? dataVersion : "2026";
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getAdmissionCode() {
        return admissionCode;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getCommonAbbreviation() {
        return commonAbbreviation;
    }

    public CampusType getCampusType() {
        return campusType;
    }

    public SchoolEntity getParentSchool() {
        return parentSchool;
    }

    public UUID getParentSchoolId() {
        return parentSchool != null ? parentSchool.getId() : null;
    }

    public String getRegion() {
        return region;
    }

    public String getStatus() {
        return status;
    }

    public String getDataVersion() {
        return dataVersion;
    }
}
