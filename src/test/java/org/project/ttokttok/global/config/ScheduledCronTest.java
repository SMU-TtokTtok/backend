package org.project.ttokttok.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

class ScheduledCronTest {

    private static final String BASE_PACKAGE = "org.project.ttokttok";

    @Test
    @DisplayName("모든 크론은 한국 시간으로 돌고, 같은 시각에 두 개 이상 몰리지 않는다")
    void cronsUseSeoulZoneAndDistinctTimes() {
        List<Scheduled> crons = findCronSchedules();

        assertThat(crons).isNotEmpty();
        assertThat(crons).allSatisfy(scheduled -> assertThat(scheduled.zone()).isEqualTo("Asia/Seoul"));
        Map<String, Long> countByCron = crons.stream()
                .collect(Collectors.groupingBy(Scheduled::cron, Collectors.counting()));
        assertThat(countByCron).allSatisfy((cron, count) -> assertThat(count).as(cron).isEqualTo(1L));
    }

    private List<Scheduled> findCronSchedules() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        return scanner.findCandidateComponents(BASE_PACKAGE).stream()
                .map(BeanDefinition::getBeanClassName)
                .map(name -> ClassUtils.resolveClassName(name, getClass().getClassLoader()))
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .map(method -> method.getAnnotation(Scheduled.class))
                .filter(scheduled -> scheduled != null && !scheduled.cron().isEmpty())
                .toList();
    }
}
