package vn.mentify.be.modules.auth.dto;

import java.time.Instant;

public record AuthTokenResponse(
        String value,
        String tokenType,
        Instant expiresAt) {
}
