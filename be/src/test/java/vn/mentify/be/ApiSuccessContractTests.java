package vn.mentify.be;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mentify.be.infra.web.SuccessResponseFactory;
import vn.mentify.be.shared.api.PageMetadata;

import java.util.List;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiSuccessContractTests.TestController.class)
@WithMockUser(username = "test-user")
@Testcontainers(disabledWithoutDocker = true)
class ApiSuccessContractTests {

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
    void successResponseContainsDataNullMetaTimestampAndRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/test-success/item")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user"))
                        .header("X-Request-Id", "success-request-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "success-request-1"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("example-1"))
                .andExpect(jsonPath("$.meta").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern(".*\\+07:00")))
                .andExpect(jsonPath("$.requestId").value("success-request-1"));
    }

    @Test
    void paginatedSuccessResponseContainsMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/test-success/items")
                        .with(SecurityMockMvcRequestPostProcessors.user("test-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(20))
                .andExpect(jsonPath("$.meta.totalItems").value(31))
                .andExpect(jsonPath("$.meta.totalPages").value(2))
                .andExpect(jsonPath("$.requestId").isString());
    }

    @RestController
    static class TestController {

        private final SuccessResponseFactory successResponseFactory;

        TestController(SuccessResponseFactory successResponseFactory) {
            this.successResponseFactory = successResponseFactory;
        }

        @GetMapping("/api/v1/test-success/item")
        Object item(jakarta.servlet.http.HttpServletRequest request) {
            return successResponseFactory.success(new Item("example-1"), request);
        }

        @GetMapping("/api/v1/test-success/items")
        Object items(jakarta.servlet.http.HttpServletRequest request) {
            return successResponseFactory.success(
                    List.of(new Item("example-1")),
                    PageMetadata.of(1, 20, 31),
                    request);
        }
    }

    record Item(String id) {
    }
}
