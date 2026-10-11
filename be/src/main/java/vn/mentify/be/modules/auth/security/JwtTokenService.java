package vn.mentify.be.modules.auth.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.stereotype.Service;
import vn.mentify.be.modules.user.persistence.UserEntity;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.security.jwt.enabled", havingValue = "true")
public class JwtTokenService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String ROLE_CLAIM = "role";
    private static final String USER_TYPE_CLAIM = "userType";

    private final JwtProperties properties;
    private final Clock clock;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clock = Objects.requireNonNull(clock, "clock");

        if (properties.getIssuer() == null || properties.getIssuer().isBlank()) {
            throw new IllegalArgumentException("JWT issuer must not be blank");
        }
        if (properties.getAccessTokenTtl() == null || properties.getAccessTokenTtl().isZero()
                || properties.getAccessTokenTtl().isNegative()) {
            throw new IllegalArgumentException("JWT access token TTL must be positive");
        }

        PrivateKey privateKey = parsePrivateKey(properties.getPrivateKey());
        PublicKey publicKey = parsePublicKey(properties.getPublicKey());
        RSAKey signingKey = new RSAKey.Builder((RSAPublicKey) publicKey)
                .privateKey(privateKey)
                .keyID("mentify-jwt")
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(signingKey));
        this.encoder = new NimbusJwtEncoder(jwkSource);
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) publicKey).build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(clock);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                new JwtIssuerValidator(properties.getIssuer()));
        jwtDecoder.setJwtValidator(validator);
        this.decoder = jwtDecoder;
    }

    public JwtAccessToken issueAccessToken(UserEntity user) {
        Objects.requireNonNull(user, "user");
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id must not be null");
        }

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getAccessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim(ROLE_CLAIM, user.getRole().name())
                .claim(USER_TYPE_CLAIM, user.getUserType().name())
                .build();

        String value = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
        return new JwtAccessToken(value, TOKEN_TYPE, expiresAt);
    }

    public Jwt validate(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("JWT must not be blank") {
            };
        }
        return decoder.decode(token);
    }

    public JwtDecoder decoder() {
        return decoder;
    }

    private static PrivateKey parsePrivateKey(String pem) {
        byte[] der = decodePem(pem, "PRIVATE KEY");
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception exception) {
            throw new IllegalArgumentException("JWT_PRIVATE_KEY must be a PKCS#8 RSA private key", exception);
        }
    }

    private static PublicKey parsePublicKey(String pem) {
        byte[] der = decodePem(pem, "PUBLIC KEY");
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception exception) {
            throw new IllegalArgumentException("JWT_PUBLIC_KEY must be an X.509 RSA public key", exception);
        }
    }

    private static byte[] decodePem(String pem, String type) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("JWT key must be configured");
        }
        String normalized = pem.replace("\\n", "\n").trim();
        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        if (!normalized.startsWith(begin) || !normalized.endsWith(end)) {
            throw new IllegalArgumentException("JWT key must be PEM encoded as " + type);
        }
        String encoded = normalized.substring(begin.length(), normalized.length() - end.length())
                .replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("JWT key contains invalid Base64 data", exception);
        }
    }
}
