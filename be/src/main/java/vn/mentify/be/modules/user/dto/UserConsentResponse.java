package vn.mentify.be.modules.user.dto;

import vn.mentify.be.modules.user.persistence.UserConsentEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserConsentResponse(
        UUID id,
        String consentType,
        String policyVersion,
        OffsetDateTime acceptedAt,
        String guardianName,
        String guardianContact
) {
    public static UserConsentResponse from(UserConsentEntity entity) {
        return new UserConsentResponse(
                entity.getId(),
                entity.getConsentType(),
                entity.getPolicyVersion(),
                entity.getAcceptedAt(),
                entity.getGuardianName(),
                entity.getGuardianContact()
        );
    }
}
