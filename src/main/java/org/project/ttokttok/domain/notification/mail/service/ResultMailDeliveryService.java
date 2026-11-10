package org.project.ttokttok.domain.notification.mail.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.notification.mail.domain.ResultMailDelivery;
import org.project.ttokttok.domain.notification.mail.domain.ResultMailDeliveryStatus;
import org.project.ttokttok.domain.notification.mail.repository.ResultMailDeliveryRepository;
import org.project.ttokttok.domain.notification.mail.service.dto.ClaimedResultMail;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultMailDeliveryService {

    static final int CLAIM_SIZE = 10;
    private static final int LAST_ERROR_MAX_LENGTH = 255;
    private static final String LEASE_EXPIRED = "LEASE_EXPIRED";
    private static final String CAUSE_SEPARATOR = " <- ";

    private final ResultMailDeliveryRepository repository;

    @Transactional
    public void enqueue(String requestId, String clubId, List<String> recipients,
                        ResultMailContent content, LocalDateTime now) {
        if (content == null || recipients.isEmpty()) {
            return;
        }
        repository.saveAll(recipients.stream()
                .map(email -> ResultMailDelivery.pending(requestId, clubId, email, content, now))
                .toList());
    }

    @Transactional
    public List<ClaimedResultMail> claimDue(LocalDateTime now) {
        List<ResultMailDelivery> due = repository.findDueForUpdateSkipLocked(now, CLAIM_SIZE);
        List<ResultMailDelivery> claimable = new ArrayList<>();
        for (ResultMailDelivery delivery : due) {
            if (delivery.hasAttemptsLeft()) {
                delivery.startAttempt(now);
                claimable.add(delivery);
            } else {
                delivery.markFailed(LEASE_EXPIRED, false, now);
                log.warn("임대 만료로 시도 횟수를 모두 쓴 발송 작업을 실패 처리합니다: id={}", delivery.getId());
            }
        }
        return claimable.stream()
                .map(delivery -> new ClaimedResultMail(delivery.getId(), delivery.getRecipientEmail(),
                        delivery.content()))
                .toList();
    }

    @Transactional
    public void recordSuccess(String id, LocalDateTime now) {
        findSending(id).ifPresent(delivery -> delivery.markSent(now));
    }

    /**
     * 실패를 기록한다.
     *
     * @return 재시도가 예정되면 true, 최종 실패이거나 SENDING이 아니라 반영하지 않았으면 false
     */
    @Transactional
    public boolean recordFailure(String id, Exception failure, LocalDateTime now) {
        Optional<ResultMailDelivery> sending = findSending(id);
        if (sending.isEmpty()) {
            return false;
        }
        ResultMailDelivery delivery = sending.get();
        delivery.markFailed(describe(failure), isRetryable(failure), now);
        if (delivery.getStatus() == ResultMailDeliveryStatus.FAILED) {
            log.warn("결과 메일 발송을 최종 실패 처리합니다: id={}, lastError={}", delivery.getId(), delivery.getLastError());
            return false;
        }
        return true;
    }

    // 예외 자신이나 원인이 MailException이고 파싱·준비 실패가 아닐 때만 다시 시도한다.
    static boolean isRetryable(Exception failure) {
        return isRetryableMailException(failure) || isRetryableMailException(failure.getCause());
    }

    private static boolean isRetryableMailException(Throwable throwable) {
        return throwable instanceof MailException
                && !(throwable instanceof MailParseException)
                && !(throwable instanceof MailPreparationException);
    }

    private Optional<ResultMailDelivery> findSending(String id) {
        Optional<ResultMailDelivery> sending = repository.findById(id).filter(ResultMailDelivery::isSending);
        if (sending.isEmpty()) {
            log.debug("SENDING이 아닌 발송 작업이라 결과를 반영하지 않습니다: id={}", id);
        }
        return sending;
    }

    // 예외 메시지에는 수신자 주소가 들어갈 수 있어 클래스 이름만 남긴다.
    private String describe(Exception failure) {
        String error = failure.getClass().getSimpleName();
        if (failure.getCause() != null) {
            error += CAUSE_SEPARATOR + failure.getCause().getClass().getSimpleName();
        }
        return error.length() > LAST_ERROR_MAX_LENGTH ? error.substring(0, LAST_ERROR_MAX_LENGTH) : error;
    }
}
