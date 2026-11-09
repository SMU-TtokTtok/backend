package org.project.ttokttok.domain.applicant.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** 정리 스케줄러가 쓰는 {@code findAllDocumentAnswers} JPQL 이 실제 JSON 컬럼에서 동작하는지 검증한다. */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:applicantDocumentAnswersQuery;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@ActiveProfiles("test")
@Transactional
class ApplicantDocumentAnswersQueryIntegrationTest {
    @Autowired private ApplicantRepository applicantRepository;
    @Autowired private EntityManager entityManager;
    @MockitoBean private EmailService emailService;
    @MockitoBean private FCMService fcmService;
    @MockitoBean private S3Service s3Service;

    @Test
    void 저장된_서류_답변을_모두_조회한다() {
        Applicant applicant = Applicant.createApplicant("a@x.kr", "홍길동", 20, "컴공", "a@x.kr", "010",
                StudentStatus.ENROLLED, Grade.FIRST_GRADE, Gender.MALE, null);
        applicant.submitDocument(List.of(new Answer("포트폴리오", null, QuestionType.FILE, false, List.of(),
                "https://files.test/applicant/a@x.kr/p.pdf")));
        entityManager.persist(applicant);
        entityManager.flush();
        entityManager.clear();

        List<List<Answer>> answers = applicantRepository.findAllDocumentAnswers();

        assertThat(answers).hasSize(1);
        assertThat(answers.get(0)).singleElement().satisfies(answer -> {
            assertThat(answer.questionType()).isEqualTo(QuestionType.FILE);
            assertThat(answer.value()).isEqualTo("https://files.test/applicant/a@x.kr/p.pdf");
        });
    }
}
