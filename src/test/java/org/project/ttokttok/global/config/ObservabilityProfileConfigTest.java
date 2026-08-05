package org.project.ttokttok.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * {@code application-observability.yml} 의 값이 의도대로 정의돼 있는지 검증한다.
 *
 * <p>이 프로파일은 아직 어떤 실행 경로에서도 활성화되지 않는다(compose 도입 이슈에서 연결).
 * 그래서 "설정이 존재하고 값이 맞다"는 것을 여기서 못 박아두지 않으면, 나중에 활성화하는 시점에
 * 조용히 틀린 채로 켜질 수 있다. 특히 readiness 그룹은 기본값이 DB 를 보지 않기 때문에
 * 빠뜨려도 아무 에러 없이 "항상 UP" 이 되어버린다 — 그 회귀를 막는 것이 이 테스트의 목적이다.
 */
@DisplayName("application-observability.yml")
class ObservabilityProfileConfigTest {

    private static PropertySource<?> properties;

    @BeforeAll
    static void loadYaml() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources =
                loader.load("observability", new ClassPathResource("application-observability.yml"));

        assertThat(sources)
                .as("application-observability.yml 이 클래스패스에 존재해야 한다")
                .hasSize(1);
        properties = sources.get(0);
    }

    @Test
    @DisplayName("management 포트를 서비스 포트(8080)와 분리한다")
    void managementPortIsSeparatedFromServicePort() {
        // then: 별도 포트여야 호스트에 publish 하지 않고 내부 네트워크로만 격리할 수 있다
        assertThat(properties.getProperty("management.server.port")).isEqualTo(9080);
    }

    @Test
    @DisplayName("노출 엔드포인트를 health/prometheus/info 로 제한한다")
    void exposesOnlyTheThreeNeededEndpoints() {
        // then
        assertThat(properties.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,prometheus,info");
    }

    @Test
    @DisplayName("health 상세 정보를 노출하지 않는다")
    void healthDetailsAreNeverExposed() {
        // then: show-details 가 always 면 DB 호스트명/드라이버/디스크 경로가 그대로 샌다
        assertThat(properties.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("never");
    }

    @Test
    @DisplayName("liveness/readiness 프로브를 활성화한다")
    void probesAreEnabled() {
        // then
        assertThat(properties.getProperty("management.endpoint.health.probes.enabled"))
                .isEqualTo(true);
    }

    @Test
    @DisplayName("readiness 그룹이 db 와 redis 를 명시적으로 포함한다")
    void readinessGroupIncludesDbAndRedis() {
        // given: Spring 의 기본 readiness 그룹은 readinessState 만 본다 — DB 를 검사하지 않는다.
        //        블루-그린 전환 게이트로 쓰려면 반드시 db/redis 를 직접 넣어야 한다.
        Object include = properties.getProperty("management.health.group.readiness.include");

        // then
        assertThat(include).asString()
                .contains("readinessState")
                .contains("db")
                .contains("redis");
    }

    @Test
    @DisplayName("메트릭에 블루-그린 색상 라벨을 붙인다")
    void metricsCarryDeployColorTag() {
        // then: Grafana 에서 전환 중 두 색상을 나란히 비교하기 위한 라벨
        assertThat(properties.getProperty("management.metrics.tags.application"))
                .isEqualTo("ttokttok");
        assertThat(properties.getProperty("management.metrics.tags.color")).asString()
                .contains("DEPLOY_COLOR");
    }

    @Test
    @DisplayName("graceful shutdown 을 켜고 드레인 시간을 준다")
    void gracefulShutdownIsEnabled() {
        // then: 구컨테이너 종료 시 인플라이트 요청과 @Async 작업이 잘려나가지 않도록
        assertThat(properties.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(properties.getProperty("spring.lifecycle.timeout-per-shutdown-phase"))
                .isEqualTo("30s");
    }
}
