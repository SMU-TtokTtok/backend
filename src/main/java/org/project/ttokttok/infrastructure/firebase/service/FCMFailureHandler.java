package org.project.ttokttok.infrastructure.firebase.service;

import static com.google.firebase.messaging.MessagingErrorCode.INVALID_ARGUMENT;
import static com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FCMFailureHandler {

    public List<String> collectFailedTokens(BatchResponse batchResponse, final List<String> originalTokens) {

        // FCM 실패를 볼 때, DB에서 지워져야 하는 토큰으로 보는 에러 코드는 이렇다.
        // "UNREGISTERED" - 404 (토큰 만료 혹은 앱 삭제됨.)
        // "INVALID_ARGUMENT" - 400 (메시지 내용 / 형식 혹은 토큰이 잘못됨.)

        if (originalTokens == null || originalTokens.isEmpty() || batchResponse == null) {
            return List.of();
        }

        List<String> failedTokens = new ArrayList<>();
        List<SendResponse> responses = batchResponse.getResponses();

        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);

            if (!response.isSuccessful() && i < originalTokens.size()) {
                MessagingErrorCode errorCode = response.getException().getMessagingErrorCode();

                if (errorCode == UNREGISTERED ||
                    errorCode == INVALID_ARGUMENT) {
                    failedTokens.add(originalTokens.get(i));
                }
            }
        }

        return failedTokens;
    }

    public void handleInvalidTokens(List<String> invalidTokens) {
        // TODO: 실패한 토큰 처리 로직 추가
    }
}
