package org.project.ttokttok.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.clubmember.domain.MemberRole;
import org.project.ttokttok.domain.notification.fcm.exception.FCMTokenValidationException;
import org.project.ttokttok.domain.user.exception.UserValidationException;
import org.project.ttokttok.infrastructure.s3.exception.S3ContentValidationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

class ValidationExceptionResponseTest {
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ValidationController())
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @ParameterizedTest
    @CsvSource({
            "user, 존재하지 않는 사용자입니다.",
            "grade, 잘못된 학년입니다.-1",
            "role, 잘못된 역할명입니다: OWNER",
            "fcm, FCM 토큰 내 저장 시도된 토큰 값이 비어 있습니다.",
            "url, 삭제하려는 파일을 찾을 수 없음"
    })
    void businessValidation_preservesBadRequestAndExactMessage(String kind, String message) throws Exception {
        mockMvc.perform(get("/validation/" + kind))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.details").value(message));
    }

    @RestController
    static class ValidationController {
        @GetMapping("/validation/{kind}")
        void validate(@PathVariable String kind) {
            switch (kind) {
                case "user" -> throw new UserValidationException(ErrorMessage.USER_AUTH_NOT_FOUND);
                case "grade" -> ApplicableGrade.from(-1);
                case "role" -> MemberRole.fromRegistrationRole("OWNER");
                case "fcm" -> throw new FCMTokenValidationException(ErrorMessage.FCM_TOKEN_BLANK);
                case "url" -> throw new S3ContentValidationException(ErrorMessage.S3_CONTENT_URL_INVALID);
                default -> throw new AssertionError("Unexpected test case");
            }
        }
    }
}
