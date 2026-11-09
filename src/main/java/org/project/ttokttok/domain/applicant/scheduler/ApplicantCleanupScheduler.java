package org.project.ttokttok.domain.applicant.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.RecruitStatePolicy;
import org.project.ttokttok.domain.applyform.domain.enums.RecruitState;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.project.ttokttok.domain.temp.applicant.repository.TempApplicantRepository;
import org.project.ttokttok.infrastructure.s3.service.S3KeyUrlGenerator;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.s3.model.S3Object;

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
    static final Duration ORPHAN_FILE_GRACE = Duration.ofDays(1);
    static final String APPLICANT_FILE_PREFIX = "applicant/";

    private final TransactionTemplate transactionTemplate;
    private final ApplyFormRepository applyFormRepository;
    private final TempApplicantRepository tempApplicantRepository;
    private final ApplicantRepository applicantRepository;
    private final S3Service s3Service;
    private final S3KeyUrlGenerator keyUrlGenerator;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void cleanup() {
        try {
            deleteStaleTempApplicants();
        } catch (Exception e) {
            log.error("임시 저장 정리 중 오류 발생", e);
        }
        try {
            deleteOrphanApplicantFiles();
        } catch (Exception e) {
            log.error("고아 첨부 파일 정리 중 오류 발생", e);
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
            List<TempApplicant> stale = tempApplicantRepository.findByUpdatedAtBefore(cutoff);
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

    /**
     * {@code applicant/} 아래 객체 중 생성 1일이 지났고 어떤 지원자 FILE 답변도 가리키지 않는 것을 삭제한다.
     * 참조 집합을 만들지 못하면(조회 실패, 키로 바꿀 수 없는 URL) 아무것도 지우지 않고 예외를 던진다.
     * 제출 중인 업로드는 1일 유예로 보호한다. {@code temp-applicants/} 는 건드리지 않는다.
     *
     * @return 삭제한 객체 수
     */
    public int deleteOrphanApplicantFiles() {
        Set<String> referencedKeys = loadReferencedFileKeys();
        Instant threshold = Instant.now().minus(ORPHAN_FILE_GRACE);
        List<S3Object> listed = s3Service.listObjects(APPLICANT_FILE_PREFIX);
        int deleted = 0;
        for (S3Object object : listed) {
            if (!object.key().startsWith(APPLICANT_FILE_PREFIX)
                    || !object.lastModified().isBefore(threshold)
                    || referencedKeys.contains(object.key())) {
                continue;
            }
            try {
                s3Service.deleteObject(object.key());
                deleted++;
            } catch (RuntimeException e) {
                log.warn("고아 첨부 파일 삭제 실패: {}", e.getClass().getSimpleName());
            }
        }
        log.info("고아 첨부 파일 정리 완료: 조회={}건, 삭제={}건", listed.size(), deleted);
        return deleted;
    }

    // ponytail: 모든 서류 답변을 한 번에 메모리로 읽는다. 지원서가 수십만 건이 되면 페이지 단위 조회로 바꾼다.
    private Set<String> loadReferencedFileKeys() {
        return applicantRepository.findAllDocumentAnswers().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .flatMap(answer -> answer.fileUrl().stream())
                .map(keyUrlGenerator::extractKeyFromUrl)
                .collect(Collectors.toSet());
    }
}
