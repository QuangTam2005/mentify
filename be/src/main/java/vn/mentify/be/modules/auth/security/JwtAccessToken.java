package vn.mentify.be.modules.auth.security;

import java.time.Instant;

public record JwtAccessToken(
        String value,
        String tokenType,
        Instant expiresAt) {
}
