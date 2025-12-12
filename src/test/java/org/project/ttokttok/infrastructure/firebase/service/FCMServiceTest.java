package org.project.ttokttok.infrastructure.firebase.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.infrastructure.firebase.service.dto.FCMRequest;

@ExtendWith(MockitoExtension.class)
class FCMServiceTest {

    private final String TOKEN_STRING = "token";
    private final int TOKEN_BATCH_SIZE = 500;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private FCMTokenListPartitioner partitioner;

    @Mock
    private FCMFailureHandler failureHandler;

    @InjectMocks
    private FCMService fcmService;

    @Test
    @DisplayName("파티션이 하나일 때 알림 전송에 성공한다")
    void sendNotificationWithSinglePartitionTest() throws Exception {
        // given
        List<String> tokens = createTokenList(TOKEN_BATCH_SIZE);
        FCMRequest request = createFCMRequest(tokens);

        List<List<String>> singlePartition = List.of(tokens);
        setupPartitionerMock(tokens, singlePartition);
        setupFirebaseMessagingMock(500, 0);

        // when - 비동기 처리(CompletableFuture로 감싸서 완료 대기)
        CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                fcmService.sendNotification(request)
        );
        future.get(2, TimeUnit.SECONDS);

        // then
        verify(partitioner, times(1)).partitionList(tokens);
        verify(firebaseMessaging, times(1)) // 1개 파티션이므로 1번 호출
                .sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("파티션이 여러 개일 때 알림 전송에 성공한다")
    void sendNotificationWithMultiplePartitionsTest() throws Exception {
        // given
        List<String> tokens = createTokenList(TOKEN_BATCH_SIZE * 2 + 1); // 파티션 3개
        FCMRequest request = createFCMRequest(tokens);

        // 파티션을 3개로 분할 (500 + 500 + 100)
        List<List<String>> multiplePartitions = List.of(
            tokens.subList(0, TOKEN_BATCH_SIZE),                    // 0-499
            tokens.subList(TOKEN_BATCH_SIZE, TOKEN_BATCH_SIZE * 2), // 500-999
            tokens.subList(TOKEN_BATCH_SIZE * 2, tokens.size())     // 1000-1099
        );
        setupPartitionerMock(tokens, multiplePartitions);
        setupFirebaseMessagingMock(TOKEN_BATCH_SIZE, 0); // 모든 파티션 내 토큰 성공 처리

        // when - 비동기 처리(CompletableFuture로 감싸서 완료 대기)
        CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                fcmService.sendNotification(request)
        );
        future.get(2, TimeUnit.SECONDS);

        // then
        verify(partitioner, times(1)).partitionList(tokens);
        verify(firebaseMessaging, times(3)) // 3개 파티션이므로 3번 호출
                .sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("토큰이 비어있는 경우 알림을 전송하지 않는다")
    void sendNotificationWithEmptyTokensTest() throws Exception {
        // given
        FCMRequest request = createFCMRequest(List.of());

        // when
        fcmService.sendNotification(request);

        // then
        verify(firebaseMessaging, never())
                .sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("예외처리 케이스 확인 - 핸들러 호출 여부")
    void sendNotificationWithFirebaseExceptionTest() throws Exception {
        // TODO: 실패 핸들러 호출 여부 작성하기
    }

    @Test
    @DisplayName("여러 토큰 중 일부만 실패해도 계속 전송을 시도한다")
    void sendNotificationWithPartialFailureTest() throws Exception {
        // given
        List<String> tokens = createTokenList(TOKEN_BATCH_SIZE * 4); // 파티션 4개
        FCMRequest request = createFCMRequest(tokens);

        // 파티션을 4개로 분할 (500 + 500 + 500 + 500)
        List<List<String>> multiplePartitions = List.of(
                tokens.subList(0, TOKEN_BATCH_SIZE),                    // 0-499
                tokens.subList(TOKEN_BATCH_SIZE, TOKEN_BATCH_SIZE * 2), // 500-999
                tokens.subList(TOKEN_BATCH_SIZE * 2, TOKEN_BATCH_SIZE * 3),     // 1000-1999
                tokens.subList(TOKEN_BATCH_SIZE * 3, tokens.size())     // 2000-2999
        );

        setupPartitionerMock(tokens, multiplePartitions);
        setupFirebaseMessagingMock(TOKEN_BATCH_SIZE / 2, TOKEN_BATCH_SIZE / 2);

        // when - 비동기 처리(CompletableFuture로 감싸서 완료 대기)
        CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                fcmService.sendNotification(request)
        );
        future.get(2, TimeUnit.SECONDS);

        // 모든 토큰에 대해 전송 시도했는지 검증
        verify(firebaseMessaging, times(4))
                .sendEachForMulticast(any(MulticastMessage.class));
    }

    // 테스트용 DTO를 생성하는 메서드
    private FCMRequest createFCMRequest(List<String> tokens) {
        return FCMRequest.builder()
                .tokens(tokens)
                .title("테스트 제목")
                .body("테스트 내용")
                .build();
    }

    // 테스트용 토큰 리스트를 생성하는 메서드
    private List<String> createTokenList(int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> TOKEN_STRING + i)
                .toList();
    }

    private void setupPartitionerMock(List<String> tokens, List<List<String>> partitions) {
        given(partitioner.partitionList(tokens)).willReturn(partitions);
    }

    private void setupFirebaseMessagingMock(int successCount, int failureCount)
            throws FirebaseMessagingException {
        BatchResponse mockResponse = mock(BatchResponse.class);

        given(mockResponse.getSuccessCount())
                .willReturn(successCount);
        given(mockResponse.getFailureCount())
                .willReturn(failureCount);
        given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                .willReturn(mockResponse);
    }
}