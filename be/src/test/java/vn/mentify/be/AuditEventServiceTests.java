package vn.mentify.be;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mentify.be.modules.audit.domain.AuditActorType;
import vn.mentify.be.modules.audit.domain.AuditEventEntity;
import vn.mentify.be.modules.audit.persistence.AuditEventRepository;
import vn.mentify.be.modules.audit.service.AuditEventCommand;
import vn.mentify.be.modules.audit.service.AuditEventServiceImpl;
import vn.mentify.be.shared.api.ApiException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTests {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Test
    void rejectsNonSystemAuditWithoutActorUserId() {
        AuditEventServiceImpl service = new AuditEventServiceImpl(auditEventRepository);

        assertThatThrownBy(() -> service.record(new AuditEventCommand(
                null,
                AuditActorType.ADMIN,
                "ADMIN_MENTOR_APPROVE",
                "MENTOR_APPLICATION",
                null,
                null,
                Map.of(),
                null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("actorUserId là bắt buộc với actor không phải SYSTEM");
    }

    @Test
    void rejectsOversizedReason() {
        AuditEventServiceImpl service = new AuditEventServiceImpl(auditEventRepository);

        assertThatThrownBy(() -> service.record(new AuditEventCommand(
                null,
                AuditActorType.SYSTEM,
                "SYSTEM_CLEANUP",
                "NOTIFICATION",
                null,
                "x".repeat(501),
                Map.of(),
                null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("reason không được vượt quá 500 ký tự");
    }

    @Test
    void rejectsMetadataThatIsNotWhitelistedForAction() {
        AuditEventServiceImpl service = new AuditEventServiceImpl(auditEventRepository);

        assertThatThrownBy(() -> service.record(new AuditEventCommand(
                null,
                AuditActorType.SYSTEM,
                "SYSTEM_CLEANUP",
                "NOTIFICATION",
                null,
                null,
                Map.of("password", "must-not-be-stored"),
                null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("metadata chứa key không được phép cho action: SYSTEM_CLEANUP");
    }
}
