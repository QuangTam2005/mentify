package vn.mentify.be;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vn.mentify.be.modules.auth.security.JwtAccessToken;
import vn.mentify.be.modules.auth.security.JwtProperties;
import vn.mentify.be.modules.auth.security.JwtTokenService;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserType;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTests {

    private static final Instant NOW = Instant.parse("2026-10-03T10:00:00Z");
    private static KeyPair keyPair;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void issuesAndValidatesTokenWithExpectedClaims() {
        JwtTokenService service = service();
        UserEntity user = new UserEntity("user@example.com", UserType.UNIVERSITY_STUDENT);
        UUID userId = UUID.randomUUID();
        setId(user, userId);

        JwtAccessToken accessToken = service.issueAccessToken(user);
        var jwt = service.validate(accessToken.value());

        assertEquals("Bearer", accessToken.tokenType());
        assertEquals(userId.toString(), jwt.getSubject());
        assertEquals("mentify-be", jwt.getClaimAsString("iss"));
        assertEquals("USER", jwt.getClaimAsString("role"));
        assertEquals("UNIVERSITY_STUDENT", jwt.getClaimAsString("userType"));
        assertNotNull(jwt.getId());
        assertEquals(NOW.plus(Duration.ofMinutes(15)), accessToken.expiresAt());
        assertEquals(NOW, jwt.getIssuedAt());
    }

    @Test
    void rejectsTokenSignedByAnotherKey() throws Exception {
        JwtTokenService service = service();
        KeyPair otherKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        JwtProperties otherProperties = properties(otherKeyPair);
        JwtAccessToken token = new JwtTokenService(otherProperties, Clock.fixed(NOW, ZoneOffset.UTC))
                .issueAccessToken(userWithId());

        assertThrows(Exception.class, () -> service.validate(token.value()));
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenService issuer = service(Duration.ofMinutes(15), NOW.minus(Duration.ofMinutes(20)));
        JwtTokenService validator = service(Duration.ofMinutes(15), NOW);
        JwtAccessToken token = issuer.issueAccessToken(userWithId());

        assertThrows(Exception.class, () -> validator.validate(token.value()));
    }

    @Test
    void rejectsTokenWithWrongIssuer() {
        JwtTokenService service = service();
        JwtProperties wrongIssuer = properties(keyPair);
        wrongIssuer.setIssuer("another-service");
        JwtAccessToken token = new JwtTokenService(wrongIssuer, Clock.fixed(NOW, ZoneOffset.UTC))
                .issueAccessToken(userWithId());

        assertThrows(Exception.class, () -> service.validate(token.value()));
    }

    @Test
    void doesNotAcceptBlankToken() {
        assertThrows(Exception.class, () -> service().validate(" "));
    }

    private static JwtTokenService service() {
        return service(Duration.ofMinutes(15));
    }

    private static JwtTokenService service(Duration ttl) {
        return service(ttl, NOW);
    }

    private static JwtTokenService service(Duration ttl, Instant clockInstant) {
        JwtProperties properties = properties(keyPair);
        properties.setAccessTokenTtl(ttl);
        return new JwtTokenService(properties, Clock.fixed(clockInstant, ZoneOffset.UTC));
    }

    private static JwtProperties properties(KeyPair keys) {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("mentify-be");
        properties.setPrivateKey(toPem("PRIVATE KEY", keys.getPrivate().getEncoded()));
        properties.setPublicKey(toPem("PUBLIC KEY", keys.getPublic().getEncoded()));
        return properties;
    }

    private static UserEntity userWithId() {
        UserEntity user = new UserEntity("user@example.com", UserType.WORKING_PROFESSIONAL);
        setId(user, UUID.randomUUID());
        return user;
    }

    private static String toPem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded)
                + "\n-----END " + type + "-----";
    }

    private static void setId(UserEntity user, UUID id) {
        try {
            var field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
