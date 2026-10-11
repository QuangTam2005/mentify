package vn.mentify.be;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.modules.auth.security.JwtAccessToken;
import vn.mentify.be.modules.auth.security.JwtProperties;
import vn.mentify.be.modules.auth.security.JwtTokenService;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserType;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityContractTests.TestController.class)
@Testcontainers(disabledWithoutDocker = true)
class SecurityContractTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mentify")
            .withUsername("mentify")
            .withPassword("mentify_test");

    private static final KeyPair JWT_KEY_PAIR = generateJwtKeyPair();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.security.jwt.enabled", () -> true);
        registry.add("app.security.jwt.private-key", () -> toPem("PRIVATE KEY", JWT_KEY_PAIR.getPrivate().getEncoded()));
        registry.add("app.security.jwt.public-key", () -> toPem("PUBLIC KEY", JWT_KEY_PAIR.getPublic().getEncoded()));
    }

    @Test
    void healthRemainsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void unauthenticatedApiRequestUsesErrorContract() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", matchesPattern("[0-9a-f-]{36}")))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(".*\\+07:00")));
    }

    @Test
    void malformedBearerTokenUsesUnauthenticatedContract() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void expiredBearerTokenUsesUnauthenticatedContract() throws Exception {
        JwtAccessToken expired = new JwtTokenService(
                jwtProperties(JWT_KEY_PAIR, "mentify-be"),
                Clock.fixed(Instant.now().minusSeconds(3600), ZoneOffset.UTC))
                .issueAccessToken(userWithId());

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + expired.value()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void wrongIssuerBearerTokenUsesUnauthenticatedContract() throws Exception {
        JwtAccessToken token = new JwtTokenService(
                jwtProperties(JWT_KEY_PAIR, "another-service"),
                Clock.systemUTC())
                .issueAccessToken(userWithId());

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + token.value()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @WithMockUser(username = "test-user")
    void authenticatedPrincipalCanAccessProtectedApi() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test-user", roles = "USER")
    void nonAdminPrincipalUsesForbiddenContract() throws Exception {
        mockMvc.perform(get("/api/v1/admin/security-test")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Request-Id", matchesPattern("[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.requestId").isString());
    }

    @Test
    @WithMockUser(username = "test-user", roles = "USER")
    void mutationDoesNotRequireCsrfWithStatelessJwtSecurity() throws Exception {
        mockMvc.perform(post("/api/v1/test/mutation")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/test/mutation")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user").roles("USER"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test-user")
    void csrfCookieIsNotIssuedForStatelessSecurity() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void runtimeHasNoDefaultUserDetailsService() {
        org.junit.jupiter.api.Assertions.assertTrue(
                applicationContext.getBeansOfType(UserDetailsService.class).isEmpty());
    }

    @RestController
    static class TestController {

        @GetMapping("/api/v1/test/protected")
        String protectedResource() {
            return "ok";
        }

        @GetMapping("/api/v1/admin/security-test")
        String adminResource() {
            return "admin";
        }

        @PostMapping("/api/v1/test/mutation")
        String mutation() {
            return "changed";
        }
    }

    private static JwtProperties jwtProperties(KeyPair keys, String issuer) {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer(issuer);
        properties.setPrivateKey(toPem("PRIVATE KEY", keys.getPrivate().getEncoded()));
        properties.setPublicKey(toPem("PUBLIC KEY", keys.getPublic().getEncoded()));
        return properties;
    }

    private static UserEntity userWithId() {
        UserEntity user = new UserEntity("security-test@example.com", UserType.WORKING_PROFESSIONAL);
        try {
            var field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, java.util.UUID.randomUUID());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
        return user;
    }

    private static KeyPair generateJwtKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static String toPem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded)
                + "\n-----END " + type + "-----";
    }
}
