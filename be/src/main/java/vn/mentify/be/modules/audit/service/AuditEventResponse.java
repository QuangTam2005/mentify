package vn.mentify.be.modules.audit.service;

import vn.mentify.be.modules.audit.domain.AuditActorType;
import vn.mentify.be.modules.audit.domain.AuditEventEntity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID actorUserId,
        AuditActorType actorType,
        String action,
        String resourceType,
        UUID resourceId,
        String reason,
        Map<String, Object> metadata,
        String requestId,
        Instant createdAt) {

    public static AuditEventResponse from(AuditEventEntity entity) {
        return new AuditEventResponse(
                entity.getId(),
                entity.getActorUserId(),
                entity.getActorType(),
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getReason(),
                entity.getMetadata(),
                entity.getRequestId(),
                entity.getCreatedAt());
    }
}
