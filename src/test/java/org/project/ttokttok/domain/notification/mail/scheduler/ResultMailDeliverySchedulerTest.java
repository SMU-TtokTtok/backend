package org.project.ttokttok.domain.notification.mail.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.notification.mail.service.ResultMailDeliveryService;
import org.project.ttokttok.domain.notification.mail.service.dto.ClaimedResultMail;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.project.ttokttok.infrastructure.email.service.EmailService;

@ExtendWith(MockitoExtension.class)
class ResultMailDeliverySchedulerTest {

    private static final ResultMailContent CONTENT = new ResultMailContent("합격", "축하합니다");
    private static final ClaimedResultMail FIRST = new ClaimedResultMail("id-1", "a@example.com", CONTENT);
    private static final ClaimedResultMail SECOND = new ClaimedResultMail("id-2", "b@example.com", CONTENT);

    @Mock
    private ResultMailDeliveryService deliveryService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ResultMailDeliveryScheduler scheduler;

    @Test
    @DisplayName("가져온 작업을 모두 보내면 작업마다 recordSuccess를 부른다")
    void sendDueMails_recordsSuccess() {
        given(deliveryService.claimDue(any(LocalDateTime.class))).willReturn(List.of(FIRST, SECOND));

        scheduler.sendDueMails();

        verify(emailService).sendResultEmail("a@example.com", CONTENT);
        verify(emailService).sendResultEmail("b@example.com", CONTENT);
        verify(deliveryService).recordSuccess(eq("id-1"), any(LocalDateTime.class));
        verify(deliveryService).recordSuccess(eq("id-2"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("발송이 예외를 던지면 그 예외로 recordFailure를 부르고 다음 작업을 계속 보낸다")
    void sendDueMails_recordsFailureAndContinues() {
        RuntimeException failure = new RuntimeException("smtp down");
        given(deliveryService.claimDue(any(LocalDateTime.class))).willReturn(List.of(FIRST, SECOND));
        willThrow(failure).given(emailService).sendResultEmail("a@example.com", CONTENT);

        scheduler.sendDueMails();

        verify(deliveryService).recordFailure(eq("id-1"), eq(failure), any(LocalDateTime.class));
        verify(deliveryService, never()).recordSuccess(eq("id-1"), any(LocalDateTime.class));
        verify(deliveryService).recordSuccess(eq("id-2"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("재시도 예정 1건과 최종 실패 1건이 섞여도 각 행에 recordFailure를 한 번씩 부른다")
    void sendDueMails_mixedFailures_recordsEachOnce() {
        RuntimeException retryable = new RuntimeException("retry");
        RuntimeException permanent = new RuntimeException("permanent");
        given(deliveryService.claimDue(any(LocalDateTime.class))).willReturn(List.of(FIRST, SECOND));
        willThrow(retryable).given(emailService).sendResultEmail("a@example.com", CONTENT);
        willThrow(permanent).given(emailService).sendResultEmail("b@example.com", CONTENT);
        given(deliveryService.recordFailure(eq("id-1"), eq(retryable), any(LocalDateTime.class))).willReturn(true);
        given(deliveryService.recordFailure(eq("id-2"), eq(permanent), any(LocalDateTime.class))).willReturn(false);

        scheduler.sendDueMails();

        verify(deliveryService, times(1)).recordFailure(eq("id-1"), eq(retryable), any(LocalDateTime.class));
        verify(deliveryService, times(1)).recordFailure(eq("id-2"), eq(permanent), any(LocalDateTime.class));
        verify(deliveryService, never()).recordSuccess(any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("recordSuccess가 첫 행에서 예외를 던져도 두 번째 행을 보내고 기록한다")
    void sendDueMails_recordSuccessThrows_continuesWithNextRow() {
        given(deliveryService.claimDue(any(LocalDateTime.class))).willReturn(List.of(FIRST, SECOND));
        willThrow(new IllegalStateException("db down")).given(deliveryService)
                .recordSuccess(eq("id-1"), any(LocalDateTime.class));

        scheduler.sendDueMails();

        verify(emailService).sendResultEmail("b@example.com", CONTENT);
        verify(deliveryService).recordSuccess(eq("id-2"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("recordFailure가 예외를 던져도 다음 행을 계속 처리한다")
    void sendDueMails_recordFailureThrows_continuesWithNextRow() {
        RuntimeException failure = new RuntimeException("smtp down");
        given(deliveryService.claimDue(any(LocalDateTime.class))).willReturn(List.of(FIRST, SECOND));
        willThrow(failure).given(emailService).sendResultEmail("a@example.com", CONTENT);
        willThrow(new IllegalStateException("db down")).given(deliveryService)
                .recordFailure(eq("id-1"), eq(failure), any(LocalDateTime.class));

        scheduler.sendDueMails();

        verify(deliveryService).recordSuccess(eq("id-2"), any(LocalDateTime.class));
    }
}
