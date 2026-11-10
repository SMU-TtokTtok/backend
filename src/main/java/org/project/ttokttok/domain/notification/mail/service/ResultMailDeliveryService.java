package org.project.ttokttok.domain.notification.mail.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.notification.mail.domain.ResultMailDelivery;
import org.project.ttokttok.domain.notification.mail.repository.ResultMailDeliveryRepository;
import org.project.ttokttok.domain.notification.mail.service.dto.ClaimedResultMail;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultMailDeliveryService {

    static final int CLAIM_SIZE = 10;
    private static final int LAST_ERROR_MAX_LENGTH = 255;
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
        due.forEach(delivery -> delivery.startAttempt(now));
        return due.stream()
                .map(delivery -> new ClaimedResultMail(delivery.getId(), delivery.getRecipientEmail(),
                        delivery.content()))
                .toList();
    }

    @Transactional
    public void recordSuccess(String id, LocalDateTime now) {
        findSending(id).ifPresent(delivery -> delivery.markSent(now));
    }

    @Transactional
    public void recordFailure(String id, Exception failure, LocalDateTime now) {
        findSending(id).ifPresent(delivery -> delivery.markFailed(describe(failure), false, now));
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
