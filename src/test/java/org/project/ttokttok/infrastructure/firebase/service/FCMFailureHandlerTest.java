package org.project.ttokttok.infrastructure.firebase.service;

import static com.google.firebase.messaging.MessagingErrorCode.INVALID_ARGUMENT;
import static com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.SendResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.notification.fcm.repository.FCMTokenRepository;

@ExtendWith(MockitoExtension.class)
class FCMFailureHandlerTest {

    @Mock
    private FCMTokenRepository fcmTokenRepository;

    @InjectMocks
    private FCMFailureHandler handler;

    @Test
    @DisplayName("collectFailedTokens(): 토큰 리스트가 비어있다면 빈 리스트를 반환한다.")
    void emptyTokenListTest() throws Exception {
        // given
        List<String> emptyTokens = List.of();
        BatchResponse batchResponse = mock(BatchResponse.class);

        // when
        List<String> result = handler.collectFailedTokens(batchResponse, emptyTokens);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("collectFailedTokens(): 실패 토큰 종류가 UNREGISTERED인 경우, 리스트에 추가하여 반환한다.")
    void collectFailedTokensWhenUnregistered() {
        // "UNREGISTERED" - 404 (토큰 만료 혹은 앱 삭제됨.)
        // given
        List<String> originalTokens = List.of("token1", "token2", "token3");
        BatchResponse batchResponse = mock(BatchResponse.class);

        SendResponse successResponse = mock(SendResponse.class);
        SendResponse anotherSuccessResponse = mock(SendResponse.class);
        SendResponse unregisteredResponse = mock(SendResponse.class);

        given(successResponse.isSuccessful()).willReturn(true);
        given(anotherSuccessResponse.isSuccessful()).willReturn(true);

        given(unregisteredResponse.isSuccessful()).willReturn(false);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(UNREGISTERED);
        given(unregisteredResponse.getException()).willReturn(exception);

        given(batchResponse.getResponses())
                .willReturn(List.of(successResponse, unregisteredResponse, anotherSuccessResponse));

        // when
        List<String> result = handler.collectFailedTokens(batchResponse, originalTokens);

        // then
        assertThat(result).containsExactly("token2");
    }

    @Test
    @DisplayName("collectFailedTokens(): 실패 토큰 종류가 INVALID_ARGUMENT인 경우, 리스트에 추가하여 반환한다.")
    void collectFailedTokensWhenInvalidArgument() {
        // "INVALID_ARGUMENT" - 400 (메시지 내용 / 형식 혹은 토큰이 잘못됨.)
        // given
        List<String> originalTokens = List.of("token1", "token2", "token3");
        BatchResponse batchResponse = mock(BatchResponse.class);

        SendResponse successResponse = mock(SendResponse.class);
        SendResponse invalidArgumentResponse = mock(SendResponse.class);
        SendResponse anotherSuccessResponse = mock(SendResponse.class);

        given(successResponse.isSuccessful()).willReturn(true);
        given(anotherSuccessResponse.isSuccessful()).willReturn(true);

        given(invalidArgumentResponse.isSuccessful()).willReturn(false);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(INVALID_ARGUMENT);
        given(invalidArgumentResponse.getException()).willReturn(exception);

        given(batchResponse.getResponses())
                .willReturn(List.of(successResponse, invalidArgumentResponse, anotherSuccessResponse));

        // when
        List<String> result = handler.collectFailedTokens(batchResponse, originalTokens);

        // then
        assertThat(result).containsExactly("token2");
    }
}
