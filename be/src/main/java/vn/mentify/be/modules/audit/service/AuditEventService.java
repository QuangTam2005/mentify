package vn.mentify.be.modules.audit.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditEventService {

    AuditEventResponse record(AuditEventCommand command);

    Page<AuditEventResponse> findByActor(UUID actorUserId, Pageable pageable);

    Page<AuditEventResponse> findByResource(String resourceType, UUID resourceId, Pageable pageable);

    Page<AuditEventResponse> findByAction(String action, Pageable pageable);
}
