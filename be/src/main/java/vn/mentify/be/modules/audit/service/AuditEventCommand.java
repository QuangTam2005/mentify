package vn.mentify.be.modules.audit.service;

import vn.mentify.be.modules.audit.domain.AuditActorType;

import java.util.Map;
import java.util.UUID;

public record AuditEventCommand(
        UUID actorUserId,
        AuditActorType actorType,
        String action,
        String resourceType,
        UUID resourceId,
        String reason,
        Map<String, Object> metadata,
        String requestId) {
}
