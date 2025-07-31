package org.project.ttokttok.infrastructure.health;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Health Check API", description = "API 서버의 헬스 체크를 위한 엔드포인트입니다.")
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "서버가 정상 작동 중임을 나타내는 헬스 체크 응답",
                    content = @Content(schema = @Schema(type = "string", example = "Healthy"))
            ),
            @ApiResponse(
                    responseCode = "4xx",
                    description = "200번대 응답이 아닌 경우, 서버가 정상 작동하지 않음을 나타냅니다."
            )
    })
    @GetMapping
    public ResponseEntity<Map<String, String>> healthCheck() {
        // 헬스 체크를 위한 간단한 응답
        return ResponseEntity.ok()
                .body(Map.of("status", "Healthy"));
    }
}
