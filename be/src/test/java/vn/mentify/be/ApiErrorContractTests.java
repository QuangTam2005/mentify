package vn.mentify.be;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.test.context.support.WithMockUser;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.shared.api.ConflictException;
import vn.mentify.be.shared.api.ResourceNotFoundException;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiErrorContractTests.TestController.class)
@WithMockUser(username = "test-user")
@Testcontainers(disabledWithoutDocker = true)
class ApiErrorContractTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mentify")
            .withUsername("mentify")
            .withPassword("mentify_test");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void invalidRequestBodyUsesValidationContract() throws Exception {
        mockMvc.perform(post("/api/v1/test-api/validation")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test-user"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("email"))
                .andExpect(jsonPath("$.error.details[1].field").value("name"))
                .andExpect(jsonPath("$.requestId").isString())
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(".*\\+07:00")));
    }

    @Test
    void invalidMethodParameterUsesValidationContract() throws Exception {
        mockMvc.perform(get("/api/v1/test-api/parameter")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test-user"))
                        .queryParam("age", "12"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("age"));
    }

    @Test
    void businessExceptionsUseMappedStatusAndCode() throws Exception {
        mockMvc.perform(get("/api/v1/test-api/not-found")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test-user")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.details").isArray());

        mockMvc.perform(get("/api/v1/test-api/conflict")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test-user")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void unexpectedExceptionIsSanitized() throws Exception {
        mockMvc.perform(get("/api/v1/test-api/failure")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test-user")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Đã xảy ra lỗi hệ thống"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not("secret database details")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void requestIdIsGeneratedOrPreservedInHeaderAndBody() throws Exception {
        mockMvc.perform(get("/api/v1/test-api/not-found")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("test-user")))
                .andExpect(header().string("X-Request-Id", matchesPattern("[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.requestId").isString());

        mockMvc.perform(get("/api/v1/test-api/not-found").header("X-Request-Id", "client-request-42"))
                .andExpect(header().string("X-Request-Id", "client-request-42"))
                .andExpect(jsonPath("$.requestId").value("client-request-42"));
    }

    @RestController
    @Validated
    static class TestController {

        @PostMapping("/api/v1/test-api/validation")
        void validation(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/api/v1/test-api/parameter")
        void parameter(@RequestParam @Min(value = 18, message = "Tuổi phải từ 18") int age) {
        }

        @GetMapping("/api/v1/test-api/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Test resource not found");
        }

        @GetMapping("/api/v1/test-api/conflict")
        void conflict() {
            throw new ConflictException("Test conflict");
        }

        @GetMapping("/api/v1/test-api/failure")
        void failure() {
            throw new IllegalStateException("secret database details");
        }
    }

    record TestRequest(
            @NotBlank(message = "Tên là bắt buộc") String name,
            @NotBlank(message = "Email là bắt buộc")
            @Email(message = "Email không hợp lệ") String email) {
    }
}
