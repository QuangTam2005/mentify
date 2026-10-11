package vn.mentify.be;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import static org.assertj.core.api.Assertions.assertThat;

class DockerAvailabilityTest {

    @Test
    void dockerIsRequiredWhenRunningInCi() {
        boolean dockerAvailable = isDockerAvailable();
        if (isCiEnvironment()) {
            assertThat(dockerAvailable)
                    .as("Docker is required in CI because PostgreSQL integration tests must execute")
                    .isTrue();
        }
    }

    private boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isCiEnvironment() {
        return "true".equalsIgnoreCase(System.getenv("CI"))
                || "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"))
                || "true".equalsIgnoreCase(System.getProperty("ci"));
    }
}
