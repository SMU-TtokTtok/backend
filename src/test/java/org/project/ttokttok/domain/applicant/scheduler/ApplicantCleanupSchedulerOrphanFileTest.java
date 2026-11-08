package org.project.ttokttok.domain.applicant.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.infrastructure.s3.service.S3KeyUrlGenerator;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
class ApplicantCleanupSchedulerOrphanFileTest {
    private static final String BASE_URL = "https://files.test";
    private static final Duration OLD = Duration.ofDays(2);
    private static final Duration RECENT = Duration.ofHours(1);

    @Mock private ApplicantRepository applicantRepository;
    @Mock private S3Service s3Service;

    private ApplicantCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        S3KeyUrlGenerator keyUrlGenerator = new S3KeyUrlGenerator();
        ReflectionTestUtils.setField(keyUrlGenerator, "fileCloudUrl", BASE_URL);
        scheduler = new ApplicantCleanupScheduler(null, null, null,
                applicantRepository, s3Service, keyUrlGenerator);
    }

    @Test
    void 참조된_파일은_유지하고_참조되지_않은_오래된_파일만_삭제한다() {
        when(applicantRepository.findAllDocumentAnswers()).thenReturn(List.of(List.of(
                fileAnswer(BASE_URL + "/applicant/a@x.kr/referenced.pdf"),
                fileAnswer(""),
                new Answer("자기소개", null, QuestionType.LONG_ANSWER, true, List.of(), "applicant/a@x.kr/text.pdf"))));
        when(s3Service.listObjects("applicant/")).thenReturn(List.of(
                object("applicant/a@x.kr/referenced.pdf", OLD),
                object("applicant/a@x.kr/orphan-old.pdf", OLD),
                object("applicant/a@x.kr/orphan-recent.pdf", RECENT),
                object("applicant/a@x.kr/text.pdf", OLD),
                object("temp-applicants/a@x.kr/draft.pdf", OLD)));

        int deleted = scheduler.deleteOrphanApplicantFiles();

        assertThat(deleted).isEqualTo(2);
        verify(s3Service).deleteObject("applicant/a@x.kr/orphan-old.pdf");
        verify(s3Service).deleteObject("applicant/a@x.kr/text.pdf");
        verify(s3Service, never()).deleteObject("applicant/a@x.kr/referenced.pdf");
        verify(s3Service, never()).deleteObject("applicant/a@x.kr/orphan-recent.pdf");
        verify(s3Service, never()).deleteObject("temp-applicants/a@x.kr/draft.pdf");
    }

    @Test
    void applicant_접두어가_아닌_키는_목록에_섞여도_삭제하지_않는다() {
        when(applicantRepository.findAllDocumentAnswers()).thenReturn(List.of());
        when(s3Service.listObjects("applicant/")).thenReturn(List.of(
                object("club/profile.png", OLD),
                object("temp-applicants/a@x.kr/draft.pdf", OLD)));

        assertThat(scheduler.deleteOrphanApplicantFiles()).isZero();
        verify(s3Service, never()).deleteObject(anyString());
    }

    @Test
    void 참조_조회에_실패하면_아무것도_삭제하지_않는다() {
        when(applicantRepository.findAllDocumentAnswers()).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> scheduler.deleteOrphanApplicantFiles()).isInstanceOf(IllegalStateException.class);
        verify(s3Service, never()).deleteObject(anyString());
    }

    @Test
    void 해석할_수_없는_참조_URL이_있으면_아무것도_삭제하지_않는다() {
        when(applicantRepository.findAllDocumentAnswers()).thenReturn(List.of(List.of(
                fileAnswer("https://other-host.test/applicant/a@x.kr/legacy.pdf"))));

        assertThatThrownBy(() -> scheduler.deleteOrphanApplicantFiles()).isInstanceOf(RuntimeException.class);
        verify(s3Service, never()).deleteObject(anyString());
    }

    private static Answer fileAnswer(String url) {
        return new Answer("포트폴리오", null, QuestionType.FILE, false, List.of(), url);
    }

    private static S3Object object(String key, Duration age) {
        return S3Object.builder().key(key).lastModified(Instant.now().minus(age)).build();
    }
}
