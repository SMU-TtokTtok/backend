package org.project.ttokttok.infrastructure.firebase.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.infrastructure.firebase.service.dto.FCMRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FCMService {

    private final FirebaseMessaging firebaseMessaging;
    private final FCMTokenListPartitioner partitioner;
    private final FCMFailureHandler failureHandler;

    @Async
    public void sendNotification(FCMRequest request) {
        if (request.tokens() == null || request.tokens().isEmpty()) {
            return;
        }

        Notification notification = Notification.builder()
                .setTitle(request.title())
                .setBody(request.body())
                .build();

        List<List<String>> partitions = partitioner.partitionList(request.tokens());

        sendMessagesToTokens(partitions, notification);
    }

    private void sendMessagesToTokens(List<List<String>> partitions, Notification notification) {
        List<String> invalidTokens = new ArrayList<>();

        for (List<String> chunk : partitions) {
            try {
                BatchResponse response = getBatchResponse(notification, chunk);
                if (response.getFailureCount() > 0) {
                    // 실패한 토큰 수집
                    List<String> failedTokensInChunk = failureHandler.collectFailedTokens(response, chunk);
                    invalidTokens.addAll(failedTokensInChunk);
                }

            } catch (Exception e) {
                // TODO: 추후 재시도 로직 추가
                log.error("Failed to send FCM notification to token chunk.", e);
            }
        }

        if (!invalidTokens.isEmpty()) {
            failureHandler.handleInvalidTokens(invalidTokens);
        }
    }

    // 메시지 전송 로직
    private BatchResponse getBatchResponse(Notification notification, List<String> chunk)
            throws FirebaseMessagingException {

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(notification)
                .addAllTokens(chunk) // 토큰 청크 추가
                .build();

        // @Async 내부이므로 코드 단순화를 위해 동기식 호출 사용
        BatchResponse response = firebaseMessaging.sendEachForMulticast(message);

        log.info("Group notification sent: {} successful, {} failed",
                response.getSuccessCount(), response.getFailureCount());

        return response;
    }
}
