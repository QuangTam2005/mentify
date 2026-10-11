package vn.mentify.be;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuditTimestampSerializationTest {

    @Test
    void auditTimestampFormatUsesVietnameseOffset() throws Exception {
        Instant instant = Instant.parse("2026-01-02T03:04:05.123Z");
        String serialized = JsonMapper.builder()
                .build()
                .writeValueAsString(new AuditTimestampPayload(instant));

        assertThat(serialized).isEqualTo("{\"value\":\"2026-01-02T10:04:05.123+07:00\"}");
    }

    private record AuditTimestampPayload(
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Asia/Ho_Chi_Minh")
            Instant value) {

    }
}
