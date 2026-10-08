package org.project.ttokttok.domain.applicant.controller.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

public record AnswerRequest(
        @NotBlank(message = "질문 ID는 필수입니다.")
        String questionId,

        /**
         * 답변 내용
         * - 단답형/장문형: 텍스트 값
         * - 체크박스: 선택된 옵션 문자열 목록, 라디오: 선택된 옵션 문자열 하나
         * - 파일: null (별도의 MultipartFile로 처리)
         */
        @Nullable
        Object value
) {
}
