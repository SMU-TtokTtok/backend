package org.project.ttokttok.domain.temp.applicant.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TempApplicantTest {

    @Nested
    @DisplayName("create(): 임시 지원서 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("전달된 값으로 임시 지원서를 생성한다")
        void create_Success() {
            // given
            String formId = "form-1";
            String userEmail = "test@sangmyung.kr";
            Map<String, Object> tempData = new HashMap<>();
            tempData.put("name", "홍길동");

            // when
            TempApplicant tempApplicant = TempApplicant.create(formId, userEmail, tempData);

            // then
            assertThat(tempApplicant.getFormId()).isEqualTo(formId);
            assertThat(tempApplicant.getUserEmail()).isEqualTo(userEmail);
            assertThat(tempApplicant.getTempData()).isEqualTo(tempData);
            assertThat(tempApplicant.getId()).isNull(); // JPA 저장 전이므로 null
        }
    }

    @Nested
    @DisplayName("update(): 임시 지원서 수정 테스트")
    class UpdateTest {

        @Test
        @DisplayName("새로운 데이터로 임시 지원서 내용을 교체한다")
        void update_Success() {
            // given
            Map<String, Object> originalData = new HashMap<>();
            originalData.put("name", "홍길동");
            TempApplicant tempApplicant = TempApplicant.create("form-1", "test@sangmyung.kr", originalData);

            Map<String, Object> newData = new HashMap<>();
            newData.put("name", "김철수");
            newData.put("age", 23);

            // when
            tempApplicant.update(newData);

            // then
            assertThat(tempApplicant.getTempData()).isEqualTo(newData);
            assertThat(tempApplicant.getTempData()).isNotEqualTo(originalData);
        }
    }
}
