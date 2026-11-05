package org.project.ttokttok.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 구조 개선 중 API 계약이 바뀌지 않았음을 검출한다.
 *
 * <p>구조 개선 기간에만 쓰는 임시 테스트다. 스냅샷은 공개 저장소에 올리지 않고 로컬에만 두며,
 * 파일이 없는 환경에서는 건너뛴다. {@code UPDATE_API_SNAPSHOT=true}로 실행하면 스냅샷을 만든다.
 */
@SpringBootTest(properties = "springdoc.writer-with-order-by-keys=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractSnapshotTest {

    private static final Path SNAPSHOT = Path.of("src/test/resources/openapi/api-contract.json");

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("OpenAPI 문서가 저장된 API 계약과 같다")
    void apiContractMatchesSnapshot() throws Exception {
        String actual = currentContract();

        if ("true".equals(System.getenv("UPDATE_API_SNAPSHOT"))) {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.writeString(SNAPSHOT, actual, StandardCharsets.UTF_8);
        }

        assumeTrue(Files.exists(SNAPSHOT), "로컬 API 스냅샷이 없어 비교를 건너뛴다.");

        assertThat(actual)
                .as("API 계약이 바뀌었다. 의도한 변경이면 UPDATE_API_SNAPSHOT=true로 스냅샷을 갱신한다.")
                .isEqualTo(readSnapshot());
    }

    private String currentContract() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode contract = mapper.readTree(body);
        // 실행 환경에 따라 달라지는 값은 계약이 아니다.
        ((ObjectNode) contract).remove("servers");
        String pretty = mapper.writeValueAsString(mapper.treeToValue(contract, Object.class));
        // Jackson은 OS 줄바꿈으로 출력하므로 LF로 고정해 OS 간 비교가 같게 한다.
        return pretty.replace("\r\n", "\n") + "\n";
    }

    private String readSnapshot() throws IOException {
        return Files.readString(SNAPSHOT, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
