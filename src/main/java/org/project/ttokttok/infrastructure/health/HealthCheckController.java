package org.project.ttokttok.infrastructure.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @GetMapping
    public ResponseEntity<String> healthCheck() {
        // 헬스 체크를 위한 간단한 응답
        return ResponseEntity.ok()
                .body("Healthy");
    }
}
