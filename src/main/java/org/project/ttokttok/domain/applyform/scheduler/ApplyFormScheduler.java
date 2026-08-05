package org.project.ttokttok.domain.applyform.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplyFormScheduler {

    private final ApplyFormRepository applyFormRepository;

    // 블루-그린 전환 창에서 blue/green 이 동시에 떠 있어도 한 인스턴스만 실행하도록 락을 건다.
    // lockAtLeastFor: 인스턴스 간 시계 오차로 락이 조기 해제돼 중복 실행되는 것을 막는다.
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(
            name = "ApplyFormScheduler_updateExpiredApplyFormsStatus",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT1M"
    )
    @Transactional
    public void updateExpiredApplyFormsStatus() {
        log.info("지원 폼 마감날짜 확인 스케줄러 시작");

        try {
            LocalDate today = LocalDate.now();

            // 활성화된 지원 폼들 중에서 마감날짜가 지난 것들 조회
            List<ApplyForm> expiredApplyForms = applyFormRepository.findExpiredApplyForms(today);

            if (expiredApplyForms.isEmpty()) {
                log.info("마감날짜가 지난 지원 폼이 없습니다.");
                return;
            }

            // 마감된 지원 폼들의 상태를 비활성화로 변경
            int updatedCount = 0;
            for (ApplyForm applyForm : expiredApplyForms) {
                applyForm.endRecruiting(); // 모집상태를 false로 변경
                updatedCount++;

                log.debug("지원 폼 상태 변경: ID={}, 제목={}, 마감일={}",
                    applyForm.getId(), applyForm.getTitle(), applyForm.getApplyEndDate());
            }

            log.info("지원 폼 상태 변경 완료: {}개의 지원 폼이 비활성화되었습니다.", updatedCount);

        } catch (Exception e) {
            log.error("지원 폼 마감날짜 확인 스케줄러 실행 중 오류 발생", e);
        }
    }
}
