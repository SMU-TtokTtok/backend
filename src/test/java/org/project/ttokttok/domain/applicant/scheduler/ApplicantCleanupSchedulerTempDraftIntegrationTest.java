package org.project.ttokttok.domain.applicant.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.RecruitStatePolicy;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:applicantCleanupTempDraft;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@ActiveProfiles("test")
@Transactional
class ApplicantCleanupSchedulerTempDraftIntegrationTest {
    @Autowired private ApplicantCleanupScheduler scheduler;
    @Autowired private EntityManager entityManager;
    @MockitoBean private EmailService emailService;
    @MockitoBean private FCMService fcmService;
    @MockitoBean private S3Service s3Service;

    private static final long OLD_DAYS = 8;
    private static final long RECENT_DAYS = 1;

    @Test
    void 마감된_폼의_오래된_임시저장은_삭제한다() {
        ApplyForm closed = persistForm(true);

        String draftId = persistDraft(closed.getId(), OLD_DAYS);

        assertThat(scheduler.deleteStaleTempApplicants()).isEqualTo(1);
        assertThat(exists(draftId)).isFalse();
    }

    @Test
    void 사라진_폼의_오래된_임시저장은_삭제한다() {
        String draftId = persistDraft("missing-form", OLD_DAYS);

        assertThat(scheduler.deleteStaleTempApplicants()).isEqualTo(1);
        assertThat(exists(draftId)).isFalse();
    }

    @Test
    void 모집중_폼의_오래된_임시저장은_유지한다() {
        ApplyForm open = persistForm(false);

        String draftId = persistDraft(open.getId(), OLD_DAYS);

        assertThat(scheduler.deleteStaleTempApplicants()).isZero();
        assertThat(exists(draftId)).isTrue();
    }

    @Test
    void 마감된_폼이어도_최근_임시저장은_유지한다() {
        ApplyForm closed = persistForm(true);

        String draftId = persistDraft(closed.getId(), RECENT_DAYS);

        assertThat(scheduler.deleteStaleTempApplicants()).isZero();
        assertThat(exists(draftId)).isTrue();
    }

    private ApplyForm persistForm(boolean closed) {
        Admin admin = Admin.adminJoin("cleanupAdmin", "test-password");
        entityManager.persist(admin);
        Club club = Club.builder().admin(admin).clubName("Cleanup club")
                .clubUniv(ClubUniv.ENGINEERING).build();
        entityManager.persist(club);
        LocalDate today = RecruitStatePolicy.today();
        ApplyForm form = ApplyForm.createApplyForm(club, false, today.minusDays(10), today.plusDays(10),
                null, null, 10, Set.of(ApplicableGrade.FIRST_GRADE), "title", "subtitle", List.of());
        if (closed) {
            form.endRecruiting();
        }
        entityManager.persist(form);
        return form;
    }

    private String persistDraft(String formId, long ageDays) {
        TempApplicant draft = TempApplicant.create(formId, "user" + System.nanoTime() + "@test.ac.kr", Map.of());
        entityManager.persist(draft);
        entityManager.flush();
        // updatedAt 은 auditing 이 관리하므로 벌크 JPQL 로 과거 시각을 직접 넣는다
        entityManager.createQuery("update TempApplicant t set t.updatedAt = :at where t.id = :id")
                .setParameter("at", LocalDateTime.now(RecruitStatePolicy.ZONE).minusDays(ageDays))
                .setParameter("id", draft.getId())
                .executeUpdate();
        entityManager.clear();
        return draft.getId();
    }

    private boolean exists(String draftId) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(TempApplicant.class, draftId) != null;
    }
}
