package vn.mentify.be.modules.masterdata.dto;

import vn.mentify.be.modules.masterdata.domain.CampusType;
import vn.mentify.be.modules.masterdata.domain.SchoolEntity;

import java.util.UUID;

public record SchoolResponse(
        UUID id,
        String code,
        String admissionCode,
        String name,
        String shortName,
        String commonAbbreviation,
        CampusType campusType,
        UUID parentSchoolId,
        String region,
        String status,
        String dataVersion
) {
    public static SchoolResponse from(SchoolEntity entity) {
        return new SchoolResponse(
                entity.getId(),
                entity.getCode(),
                entity.getAdmissionCode(),
                entity.getName(),
                entity.getShortName(),
                entity.getCommonAbbreviation(),
                entity.getCampusType(),
                entity.getParentSchoolId(),
                entity.getRegion(),
                entity.getStatus(),
                entity.getDataVersion()
        );
    }
}
