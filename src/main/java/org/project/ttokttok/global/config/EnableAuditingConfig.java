package org.project.ttokttok.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class EnableAuditingConfig {
    // JPA의 Auditing 기능을 사용하겟다고 명시
    // Auditing: 엔티티의 생성 및 수정 시간 추적(@CreatedDate, @LastModifiedDate 등)
}
