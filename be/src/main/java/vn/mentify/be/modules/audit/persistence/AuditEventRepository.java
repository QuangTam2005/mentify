package vn.mentify.be.modules.audit.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mentify.be.modules.audit.domain.AuditEventEntity;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    Page<AuditEventEntity> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId, Pageable pageable);

    Page<AuditEventEntity> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            String resourceType,
            UUID resourceId,
            Pageable pageable);

    Page<AuditEventEntity> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
}
