package org.project.ttokttok.domain.applyform.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Scheduled(cron = "0 0 4 * * *")
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
