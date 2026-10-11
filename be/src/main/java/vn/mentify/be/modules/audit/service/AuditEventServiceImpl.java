package vn.mentify.be.modules.audit.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.mentify.be.modules.audit.domain.AuditActorType;
import vn.mentify.be.modules.audit.domain.AuditEventEntity;
import vn.mentify.be.modules.audit.persistence.AuditEventRepository;
import vn.mentify.be.shared.api.ApiErrorCode;
import vn.mentify.be.shared.api.ApiException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuditEventServiceImpl implements AuditEventService {

    private static final Map<String, Set<String>> ALLOWED_METADATA_KEYS = Map.ofEntries(
            Map.entry("ADMIN_MENTOR_APPROVE", Set.of("attempt", "source")),
            Map.entry("ADMIN_MENTOR_REJECT", Set.of("attempt", "source")),
            Map.entry("ADMIN_MENTOR_SUSPEND", Set.of("source")),
            Map.entry("MODERATION_REVIEW", Set.of("reportCount", "source")),
            Map.entry("MODERATION_HIDE_REVIEW", Set.of("source")),
            Map.entry("MODERATION_HIDE_POST", Set.of("reportCount", "source")),
            Map.entry("FILE_EVIDENCE_VIEW", Set.of("fileType", "source")),
            Map.entry("REPORT_VIEW", Set.of("source")),
            Map.entry("NO_SHOW_REVIEW", Set.of("outcome", "source")),
            Map.entry("ACCOUNT_DELETE_REQUEST", Set.of("source")),
            Map.entry("ACCOUNT_ANONYMIZE", Set.of("source")),
            Map.entry("SYSTEM_CLEANUP", Set.of("deletedCount", "source")));

    private final AuditEventRepository auditEventRepository;

    public AuditEventServiceImpl(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public AuditEventResponse record(AuditEventCommand command) {
        validate(command);

        Map<String, Object> metadata = command.metadata() == null
                ? Map.of()
                : Map.copyOf(command.metadata());
        validateMetadata(command.action().trim(), metadata);

        AuditEventEntity saved = auditEventRepository.saveAndFlush(new AuditEventEntity(
                command.actorUserId(),
                command.actorType(),
                command.action().trim(),
                command.resourceType().trim(),
                command.resourceId(),
                normalizeOptional(command.reason()),
                metadata,
                normalizeOptional(command.requestId())));
        return AuditEventResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> findByActor(UUID actorUserId, Pageable pageable) {
        return auditEventRepository.findByActorUserIdOrderByCreatedAtDesc(actorUserId, pageable)
                .map(AuditEventResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> findByResource(String resourceType, UUID resourceId, Pageable pageable) {
        return auditEventRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
                        resourceType.trim(), resourceId, pageable)
                .map(AuditEventResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> findByAction(String action, Pageable pageable) {
        return auditEventRepository.findByActionOrderByCreatedAtDesc(action.trim(), pageable)
                .map(AuditEventResponse::from);
    }

    private void validate(AuditEventCommand command) {
        if (command == null || command.actorType() == null) {
            throw validation("actorType là bắt buộc");
        }
        if (command.actorType() != AuditActorType.SYSTEM && command.actorUserId() == null) {
            throw validation("actorUserId là bắt buộc với actor không phải SYSTEM");
        }
        if (command.action() == null || command.action().isBlank() || command.action().trim().length() > 96) {
            throw validation("action phải có từ 1 đến 96 ký tự");
        }
        if (command.resourceType() == null || command.resourceType().isBlank()
                || command.resourceType().trim().length() > 96) {
            throw validation("resourceType phải có từ 1 đến 96 ký tự");
        }
        if (command.reason() != null && command.reason().length() > 500) {
            throw validation("reason không được vượt quá 500 ký tự");
        }
        if (command.requestId() != null && command.requestId().length() > 64) {
            throw validation("requestId không được vượt quá 64 ký tự");
        }
    }

    private ApiException validation(String message) {
        return new ApiException(ApiErrorCode.VALIDATION_ERROR, message);
    }

    private void validateMetadata(String action, Map<String, Object> metadata) {
        Set<String> allowedKeys = ALLOWED_METADATA_KEYS.get(action);
        if (allowedKeys == null && !metadata.isEmpty()) {
            throw validation("metadata chưa được whitelist cho action: " + action);
        }
        if (allowedKeys != null && !allowedKeys.containsAll(metadata.keySet())) {
            throw validation("metadata chứa key không được phép cho action: " + action);
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
