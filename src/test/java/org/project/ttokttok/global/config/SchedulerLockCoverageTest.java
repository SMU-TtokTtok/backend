package org.project.ttokttok.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 모든 {@code @Scheduled} 메서드가 {@code @SchedulerLock} 으로 보호되는지 검증한다.
 *
 * <p>블루-그린 무중단 배포에서는 전환 창 동안 blue/green 두 인스턴스가 동시에 떠 있고,
 * {@code @Scheduled} 는 인스턴스마다 독립적으로 발화한다. 락이 없는 스케줄러가 하나라도 있으면
 * FCM 푸시 중복 발송 같은 사용자에게 직접 보이는 버그가 된다.
 *
 * <p>개별 스케줄러를 하나씩 검사하는 대신 클래스패스를 스캔하는 이유는, 앞으로 <b>새로 추가되는</b>
 * 스케줄러가 락 없이 들어오는 것까지 막기 위해서다. 이 테스트가 이 규칙의 유일한 강제 수단이다.
 */
@DisplayName("스케줄러 분산 락 적용 범위")
class SchedulerLockCoverageTest {

    private static final String BASE_PACKAGE = "org.project.ttokttok";

    @Test
    @DisplayName("@Scheduled 메서드는 예외 없이 @SchedulerLock 을 가진다")
    void everyScheduledMethodIsLocked() {
        // given
        List<Method> scheduledMethods = findScheduledMethods();

        // then: 스캔 자체가 헛돌면(0건) 테스트가 의미 없이 통과하므로 최소 개수를 못 박는다
        assertThat(scheduledMethods)
                .as("@Scheduled 메서드를 하나도 찾지 못했다면 스캔 설정이 잘못된 것이다")
                .isNotEmpty();

        List<String> unlocked = scheduledMethods.stream()
                .filter(method -> method.getAnnotation(SchedulerLock.class) == null)
                .map(method -> method.getDeclaringClass().getSimpleName() + "#" + method.getName())
                .toList();

        assertThat(unlocked)
                .as("락이 없는 스케줄러는 블루-그린 전환 창에서 중복 실행된다")
                .isEmpty();
    }

    @Test
    @DisplayName("락 이름은 스케줄러마다 고유하다")
    void lockNamesAreUnique() {
        // given
        List<String> lockNames = findScheduledMethods().stream()
                .map(method -> method.getAnnotation(SchedulerLock.class))
                .filter(lock -> lock != null)
                .map(SchedulerLock::name)
                .toList();

        // then: 이름이 겹치면 서로 무관한 스케줄러가 서로를 막아 하나가 조용히 실행되지 않는다
        assertThat(lockNames).doesNotHaveDuplicates();
        assertThat(lockNames).allSatisfy(name -> assertThat(name).isNotBlank());
    }

    @Test
    @DisplayName("lockAtMostFor 가 lockAtLeastFor 보다 길다")
    void lockDurationsAreCoherent() {
        // given
        List<SchedulerLock> locks = findScheduledMethods().stream()
                .map(method -> method.getAnnotation(SchedulerLock.class))
                .filter(lock -> lock != null)
                .toList();

        // then: lockAtMostFor 는 프로세스가 죽어도 락이 영구히 남지 않게 하는 상한이므로
        //       하한(lockAtLeastFor)보다 반드시 길어야 한다
        assertThat(locks).allSatisfy(lock -> {
            Duration atMost = Duration.parse(lock.lockAtMostFor());
            Duration atLeast = Duration.parse(lock.lockAtLeastFor());
            assertThat(atMost)
                    .as("%s: lockAtMostFor 는 lockAtLeastFor 보다 길어야 한다", lock.name())
                    .isGreaterThan(atLeast);
        });
    }

    private List<Method> findScheduledMethods() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents(BASE_PACKAGE);

        List<Method> scheduledMethods = new ArrayList<>();
        for (BeanDefinition candidate : candidates) {
            Class<?> type = resolve(candidate.getBeanClassName());
            for (Method method : type.getDeclaredMethods()) {
                if (method.getAnnotation(Scheduled.class) != null) {
                    scheduledMethods.add(method);
                }
            }
        }
        return scheduledMethods;
    }

    private Class<?> resolve(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("스캔된 클래스를 로드할 수 없다: " + className, e);
        }
    }
}
