package vn.mentify.be.modules.masterdata.dto;

import vn.mentify.be.modules.masterdata.domain.MajorGroupEntity;

import java.util.UUID;

public record MajorGroupResponse(
        UUID id,
        String code,
        String name,
        String fieldCode,
        String regulationVersion,
        String description,
        boolean isActive
) {
    public static MajorGroupResponse from(MajorGroupEntity entity) {
        return new MajorGroupResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getFieldCode(),
                entity.getRegulationVersion(),
                entity.getDescription(),
                entity.isActive()
        );
    }
}
