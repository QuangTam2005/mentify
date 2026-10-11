package vn.mentify.be;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.modules.auth.dto.DevLoginRequest;
import vn.mentify.be.modules.auth.security.OAuth2LoginSuccessHandler;
import vn.mentify.be.modules.user.persistence.UserConsentRepository;
import vn.mentify.be.modules.user.persistence.UserEntity;
import vn.mentify.be.modules.user.persistence.UserProfileEntity;
import vn.mentify.be.modules.user.persistence.UserProfileRepository;
import vn.mentify.be.modules.user.persistence.UserRepository;
import vn.mentify.be.modules.user.persistence.UserRole;
import vn.mentify.be.modules.user.persistence.UserType;
import vn.mentify.be.modules.user.persistence.UserTypeHistoryRepository;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthContractAndIntegrationTests {

    private static final String ISO_OFFSET_REGEX = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?\\+07:00$";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mentify")
            .withUsername("mentify")
            .withPassword("mentify_test");

    private static final KeyPair JWT_KEY_PAIR = generateJwtKeyPair();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserConsentRepository userConsentRepository;

    @Autowired
    private UserTypeHistoryRepository userTypeHistoryRepository;

    @Autowired
    private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.security.jwt.enabled", () -> true);
        registry.add("app.security.jwt.private-key", () -> toPem("PRIVATE KEY", JWT_KEY_PAIR.getPrivate().getEncoded()));
        registry.add("app.security.jwt.public-key", () -> toPem("PUBLIC KEY", JWT_KEY_PAIR.getPublic().getEncoded()));
    }

    @BeforeEach
    void cleanData() {
        userConsentRepository.deleteAll();
        userTypeHistoryRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void csrfEndpointIsRemovedAfterJwtCutover() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMe_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(ISO_OFFSET_REGEX)));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void passwordResetEndpoints_useGenericForgotResponseAndRejectInvalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "unknown@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(header().doesNotExist("Set-Cookie"));

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", "invalid-token",
                                "newPassword", "correct horse battery"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void devLogin_and_jwtLifecycle_e2e() throws Exception {
        DevLoginRequest loginRequest = new DevLoginRequest(
                "dev-tester@mentify.vn",
                UserType.UNIVERSITY_STUDENT,
                UserRole.USER,
                "Trần Dev"
        );

        // 1. Call POST /api/auth/dev-login and receive a JWT
        MvcResult loginResult = mockMvc.perform(post("/api/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("dev-tester@mentify.vn"))
                .andExpect(jsonPath("$.data.userType").value("UNIVERSITY_STUDENT"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.mentorStatus").value("NOT_APPLIED"))
                .andExpect(jsonPath("$.data.acceptingBookings").value(false))
                .andExpect(jsonPath("$.data.fullName").value("Trần Dev"))
                .andExpect(jsonPath("$.data.token.value").isNotEmpty())
                .andExpect(jsonPath("$.data.token.tokenType").value("Bearer"))
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").path("value").asText();
        assertThat(loginResult.getResponse().getCookie("JSESSIONID")).isNull();

        // 2. Use the JWT to access GET /api/auth/me
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("dev-tester@mentify.vn"))
                .andExpect(jsonPath("$.data.userType").value("UNIVERSITY_STUDENT"))
                .andExpect(jsonPath("$.data.mentorStatus").value("NOT_APPLIED"));

        // 3. Logout is stateless; FE clears the token.
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void devLogin_canCreateAdminUser() throws Exception {
        DevLoginRequest adminRequest = new DevLoginRequest(
                "admin-tester@mentify.vn",
                UserType.WORKING_PROFESSIONAL,
                UserRole.ADMIN,
                "Quản Trị Viên"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin-tester@mentify.vn"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.userType").value("WORKING_PROFESSIONAL"))
                .andExpect(jsonPath("$.data.token.value").isNotEmpty())
                .andReturn();

        assertThat(result.getResponse().getCookie("JSESSIONID")).isNull();

        // Verify in DB
        UserEntity dbUser = userRepository.findByEmail("admin-tester@mentify.vn").orElseThrow();
        assertThat(dbUser.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(dbUser.getUserType()).isEqualTo(UserType.WORKING_PROFESSIONAL);
    }

    @Test
    void oauth2LoginSuccessHandler_upsertsUserAndProfile_andRedirects() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-12345",
                "email", "google.student@mentify.vn",
                "email_verified", true,
                "name", "Lê Google",
                "picture", "https://lh3.googleusercontent.com/avatar.jpg"
        );

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "email"
        );

        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "google"
        );

        // Execute Success Handler
        oauth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authToken);

        // Verify Redirect
        assertThat(response.getRedirectedUrl())
                .startsWith("http://localhost:3000/auth/callback?code=");
        String code = UriComponentsBuilder.fromUriString(response.getRedirectedUrl())
                .build()
                .getQueryParams()
                .getFirst("code");
        assertThat(code).isNotBlank();

        mockMvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token.value").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("google.student@mentify.vn"));

        mockMvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        // Verify User was upserted in DB
        UserEntity user = userRepository.findByEmail("google.student@mentify.vn").orElseThrow();
        assertThat(user.getUserType()).isEqualTo(UserType.SCHOOL_STUDENT);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);

        // Verify Profile was created in DB
        UserProfileEntity profile = userProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(profile.getFullName()).isEqualTo("Lê Google");
        assertThat(profile.getAvatarUrl()).isEqualTo("https://lh3.googleusercontent.com/avatar.jpg");
    }

    private static KeyPair generateJwtKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static String toPem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded)
                + "\n-----END " + type + "-----";
    }
}
