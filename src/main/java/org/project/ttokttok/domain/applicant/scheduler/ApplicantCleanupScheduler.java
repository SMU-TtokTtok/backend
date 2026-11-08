package org.project.ttokttok.domain.applicant.scheduler;

import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.RecruitStatePolicy;
import org.project.ttokttok.domain.applyform.domain.enums.RecruitState;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.project.ttokttok.domain.temp.applicant.repository.TempApplicantRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 지원 임시 저장과 지원자 첨부 파일의 자동 정리 (매일 새벽 4시, 한국 시간)
 *
 * <p>두 작업은 서로 독립이다. 한쪽이 실패해도 다른 쪽은 실행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicantCleanupScheduler {

    static final Duration TEMP_DRAFT_RETENTION = Duration.ofDays(7);

    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final ApplyFormRepository applyFormRepository;
    private final TempApplicantRepository tempApplicantRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void cleanup() {
        try {
            deleteStaleTempApplicants();
        } catch (Exception e) {
            log.error("임시 저장 정리 중 오류 발생", e);
        }
    }

    /**
     * 모집 중(OPEN)이 아닌 폼(또는 사라진 폼)의 임시 저장 중 마지막 수정이 7일 넘은 것을 삭제한다.
     * 모집 중인 폼의 임시 저장은 오래돼도 지우지 않는다.
     *
     * @return 삭제한 임시 저장 수
     */
    public int deleteStaleTempApplicants() {
        LocalDateTime cutoff = LocalDateTime.now(RecruitStatePolicy.ZONE).minus(TEMP_DRAFT_RETENTION);
        Integer deleted = transactionTemplate.execute(status -> {
            List<TempApplicant> stale = entityManager
                    .createQuery("select t from TempApplicant t where t.updatedAt < :cutoff", TempApplicant.class)
                    .setParameter("cutoff", cutoff)
                    .getResultList();
            Set<String> openFormIds = applyFormRepository
                    .findAllById(stale.stream().map(TempApplicant::getFormId).distinct().toList())
                    .stream()
                    .filter(form -> form.recruitState() == RecruitState.OPEN)
                    .map(ApplyForm::getId)
                    .collect(Collectors.toSet());
            List<TempApplicant> deletable = stale.stream()
                    .filter(temp -> !openFormIds.contains(temp.getFormId()))
                    .toList();
            tempApplicantRepository.deleteAll(deletable);
            return deletable.size();
        });
        log.info("임시 저장 정리 완료: 삭제={}건 (마지막 수정 {} 이전)", deleted, cutoff);
        return deleted;
    }
}
