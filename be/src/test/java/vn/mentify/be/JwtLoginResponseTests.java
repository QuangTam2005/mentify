package vn.mentify.be;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import vn.mentify.be.infra.web.SuccessResponseFactory;
import vn.mentify.be.modules.auth.domain.MentorStatus;
import vn.mentify.be.modules.auth.dto.AuthMeResponse;
import vn.mentify.be.modules.auth.dto.DevLoginRequest;
import vn.mentify.be.modules.auth.dto.PasswordLoginRequest;
import vn.mentify.be.modules.auth.security.JwtTokenService;
import vn.mentify.be.modules.auth.service.AuthService;
import vn.mentify.be.modules.auth.service.OAuth2LoginCodeService;
import vn.mentify.be.modules.auth.web.AuthController;
import vn.mentify.be.modules.auth.web.DevAuthController;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.shared.api.ApiSuccessResponse;
import vn.mentify.be.modules.user.service.CurrentUserResolver;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtLoginResponseTests {

    private static final Instant NOW = Instant.parse("2026-10-03T10:00:00Z");
    private static KeyPair keyPair;

    @Mock
    private AuthService authService;

    @Mock
    private CurrentUserResolver currentUserResolver;

    @Mock
    private ObjectProvider<JwtTokenService> jwtTokenServiceProvider;

    @Mock
    private OAuth2LoginCodeService loginCodeService;

    private JwtTokenService jwtTokenService;
    private SuccessResponseFactory responseFactory;
    private UserEntity user;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @BeforeEach
    void setUp() {
        vn.mentify.be.modules.auth.security.JwtProperties properties = new vn.mentify.be.modules.auth.security.JwtProperties();
        properties.setPrivateKey(toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        properties.setPublicKey(toPem("PUBLIC KEY", keyPair.getPublic().getEncoded()));
        jwtTokenService = new JwtTokenService(properties, Clock.fixed(NOW, ZoneOffset.UTC));
        responseFactory = new SuccessResponseFactory(Clock.fixed(NOW, ZoneOffset.UTC));
        user = new UserEntity("jwt-user@example.com", UserType.UNIVERSITY_STUDENT);
        setId(user, UUID.randomUUID());
        when(authService.getCurrentUserMe(user)).thenReturn(
                AuthMeResponse.from(user, null, MentorStatus.NOT_APPLIED, false));
        when(jwtTokenServiceProvider.getIfAvailable()).thenReturn(jwtTokenService);
    }

    @Test
    void passwordLoginReturnsJwtAndStillEstablishesSession() {
        when(authService.passwordLogin(any(PasswordLoginRequest.class))).thenReturn(user);
        AuthController controller = new AuthController(
                authService,
                currentUserResolver,
                responseFactory,
                jwtTokenServiceProvider,
                loginCodeService);
        MockHttpServletRequest request = new MockHttpServletRequest();

        ApiSuccessResponse<AuthMeResponse> result = controller.passwordLogin(
                new PasswordLoginRequest(user.getEmail(), "password"), request);

        assertTokenIsValid(result.data());
    }

    @Test
    void devLoginReturnsJwtAndStillEstablishesSession() {
        when(authService.devLogin(any(DevLoginRequest.class))).thenReturn(user);
        DevAuthController controller = new DevAuthController(
                authService,
                responseFactory,
                jwtTokenServiceProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();

        ApiSuccessResponse<AuthMeResponse> result = controller.devLogin(
                new DevLoginRequest(null, null, null, null), request);

        assertTokenIsValid(result.data());
    }

    @Test
    void loginFailsClearlyWhenJwtIsDisabled() {
        when(authService.passwordLogin(any(PasswordLoginRequest.class))).thenReturn(user);
        when(jwtTokenServiceProvider.getIfAvailable()).thenReturn(null);
        AuthController controller = new AuthController(
                authService,
                currentUserResolver,
                responseFactory,
                jwtTokenServiceProvider,
                loginCodeService);

        assertThatThrownBy(() -> controller.passwordLogin(
                new PasswordLoginRequest(user.getEmail(), "password"),
                new MockHttpServletRequest()))
                .hasMessageContaining("JWT authentication");
    }

    private void assertTokenIsValid(AuthMeResponse response) {
        assertThat(response.token()).isNotNull();
        assertThat(response.token().tokenType()).isEqualTo("Bearer");
        assertThat(jwtTokenService.validate(response.token().value()).getSubject())
                .isEqualTo(user.getId().toString());
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
