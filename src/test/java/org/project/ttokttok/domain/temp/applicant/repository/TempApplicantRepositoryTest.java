package org.project.ttokttok.domain.temp.applicant.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TempApplicantRepository의 커스텀 쿼리 메서드 테스트
 * - Spring Data JPA 기본 메서드(save, delete 등)는 테스트하지 않음
 * - 비즈니스 로직에 중요한 findByUserEmailAndFormId 메서드만 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TempApplicantRepositoryTest {

    @Autowired
    private TempApplicantRepository tempApplicantRepository;

    private String testFormId;
    private String testUserEmail;
    private Map<String, Object> testTempData;

    @BeforeEach
    void setUp() {
        testFormId = "test-form-id";
        testUserEmail = "test@university.ac.kr";

        testTempData = new HashMap<>();
        testTempData.put("name", "홍길동");
        testTempData.put("age", 22);
        testTempData.put("major", "컴퓨터공학과");
    }

    @Test
    @DisplayName("findByUserEmailAndFormId: userEmail과 formId가 일치하면 TempApplicant를 반환한다")
    void findByUserEmailAndFormId_MatchingCriteria_ReturnsTempApplicant() {
        // given
        TempApplicant tempApplicant = TempApplicant.create(
                testFormId,
                testUserEmail,
                testTempData
        );
        tempApplicantRepository.save(tempApplicant);

        // when
        Optional<TempApplicant> result = tempApplicantRepository.findByUserEmailAndFormId(
                testUserEmail,
                testFormId
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getFormId()).isEqualTo(testFormId);
        assertThat(result.get().getUserEmail()).isEqualTo(testUserEmail);
        assertThat(result.get().getTempData()).isEqualTo(testTempData);
    }

    @Test
    @DisplayName("findByUserEmailAndFormId: 일치하는 데이터가 없으면 빈 Optional을 반환한다")
    void findByUserEmailAndFormId_NoMatch_ReturnsEmptyOptional() {
        // given - 아무 데이터도 저장하지 않음

        // when
        Optional<TempApplicant> result = tempApplicantRepository.findByUserEmailAndFormId(
                "nonexistent@university.ac.kr",
                "nonexistent-form-id"
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserEmailAndFormId: userEmail이 일치하지 않으면 빈 Optional을 반환한다")
    void findByUserEmailAndFormId_DifferentEmail_ReturnsEmptyOptional() {
        // given
        TempApplicant tempApplicant = TempApplicant.create(
                testFormId,
                testUserEmail,
                testTempData
        );
        tempApplicantRepository.save(tempApplicant);

        // when - 다른 email로 조회
        Optional<TempApplicant> result = tempApplicantRepository.findByUserEmailAndFormId(
                "different@university.ac.kr",
                testFormId
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserEmailAndFormId: formId가 일치하지 않으면 빈 Optional을 반환한다")
    void findByUserEmailAndFormId_DifferentFormId_ReturnsEmptyOptional() {
        // given
        TempApplicant tempApplicant = TempApplicant.create(
                testFormId,
                testUserEmail,
                testTempData
        );
        tempApplicantRepository.save(tempApplicant);

        // when - 다른 formId로 조회
        Optional<TempApplicant> result = tempApplicantRepository.findByUserEmailAndFormId(
                testUserEmail,
                "different-form-id"
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserEmailAndFormId: 동일한 formId에 여러 사용자의 데이터가 있을 때 정확한 사용자의 데이터만 조회한다")
    void findByUserEmailAndFormId_MultipleUsersForSameForm_ReturnsCorrectUser() {
        // given - 동일한 formId에 대해 두 사용자의 임시 지원서 저장
        String user1Email = "user1@university.ac.kr";
        String user2Email = "user2@university.ac.kr";

        Map<String, Object> user1Data = new HashMap<>();
        user1Data.put("name", "홍길동");
        user1Data.put("age", 22);

        Map<String, Object> user2Data = new HashMap<>();
        user2Data.put("name", "김철수");
        user2Data.put("age", 23);

        TempApplicant tempApplicant1 = TempApplicant.create(testFormId, user1Email, user1Data);
        TempApplicant tempApplicant2 = TempApplicant.create(testFormId, user2Email, user2Data);

        tempApplicantRepository.save(tempApplicant1);
        tempApplicantRepository.save(tempApplicant2);

        // when - 각 사용자별로 조회
        Optional<TempApplicant> result1 = tempApplicantRepository.findByUserEmailAndFormId(
                user1Email,
                testFormId
        );
        Optional<TempApplicant> result2 = tempApplicantRepository.findByUserEmailAndFormId(
                user2Email,
                testFormId
        );

        // then - 각 사용자의 올바른 데이터만 조회됨
        assertThat(result1).isPresent();
        assertThat(result1.get().getUserEmail()).isEqualTo(user1Email);
        assertThat(result1.get().getTempData().get("name")).isEqualTo("홍길동");
        assertThat(result1.get().getTempData().get("age")).isEqualTo(22);

        assertThat(result2).isPresent();
        assertThat(result2.get().getUserEmail()).isEqualTo(user2Email);
        assertThat(result2.get().getTempData().get("name")).isEqualTo("김철수");
        assertThat(result2.get().getTempData().get("age")).isEqualTo(23);
    }

    @Test
    @DisplayName("findByUserEmailAndFormId: 동일한 사용자가 여러 formId에 대해 임시 지원서를 작성한 경우 정확한 form의 데이터만 조회한다")
    void findByUserEmailAndFormId_SameUserForMultipleForms_ReturnsCorrectForm() {
        // given - 동일한 사용자가 두 개의 다른 form에 임시 지원서 저장
        String form1Id = "form-1";
        String form2Id = "form-2";

        Map<String, Object> form1Data = new HashMap<>();
        form1Data.put("club", "개발동아리");

        Map<String, Object> form2Data = new HashMap<>();
        form2Data.put("club", "음악동아리");

        TempApplicant tempApplicant1 = TempApplicant.create(form1Id, testUserEmail, form1Data);
        TempApplicant tempApplicant2 = TempApplicant.create(form2Id, testUserEmail, form2Data);

        tempApplicantRepository.save(tempApplicant1);
        tempApplicantRepository.save(tempApplicant2);

        // when - 각 form별로 조회
        Optional<TempApplicant> result1 = tempApplicantRepository.findByUserEmailAndFormId(
                testUserEmail,
                form1Id
        );
        Optional<TempApplicant> result2 = tempApplicantRepository.findByUserEmailAndFormId(
                testUserEmail,
                form2Id
        );

        // then - 각 form의 올바른 데이터만 조회됨
        assertThat(result1).isPresent();
        assertThat(result1.get().getFormId()).isEqualTo(form1Id);
        assertThat(result1.get().getTempData().get("club")).isEqualTo("개발동아리");

        assertThat(result2).isPresent();
        assertThat(result2.get().getFormId()).isEqualTo(form2Id);
        assertThat(result2.get().getTempData().get("club")).isEqualTo("음악동아리");
    }
}