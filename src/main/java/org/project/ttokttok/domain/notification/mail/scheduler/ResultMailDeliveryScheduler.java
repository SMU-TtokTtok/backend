package org.project.ttokttok.domain.notification.mail.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.notification.mail.service.ResultMailDeliveryService;
import org.project.ttokttok.domain.notification.mail.service.dto.ClaimedResultMail;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DB에 쌓인 결과 메일 발송 작업을 가져와 트랜잭션 밖에서 보낸다.
 *
 * <p>작업을 가져오는 것과 결과를 기록하는 것은 각각 짧은 트랜잭션이다. SMTP 호출 동안은 DB 커넥션을 쥐지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "result-mail.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResultMailDeliveryScheduler {

    private final ResultMailDeliveryService deliveryService;
    private final EmailService emailService;

    @Scheduled(fixedDelayString = "${result-mail.worker.poll-delay-ms:5000}")
    public void sendDueMails() {
        List<ClaimedResultMail> claimed = deliveryService.claimDue(LocalDateTime.now());
        if (claimed.isEmpty()) {
            return;
        }

        int sent = 0;
        int failed = 0;
        for (ClaimedResultMail mail : claimed) {
            if (send(mail)) {
                sent++;
            } else {
                failed++;
            }
        }
        log.info("결과 메일 발송 배치 완료 - 성공: {}건, 실패: {}건", sent, failed);
    }

    private boolean send(ClaimedResultMail mail) {
        try {
            emailService.sendResultEmail(mail.recipientEmail(), mail.content());
        } catch (Exception e) {
            deliveryService.recordFailure(mail.id(), e, LocalDateTime.now());
            return false;
        }
        deliveryService.recordSuccess(mail.id(), LocalDateTime.now());
        return true;
    }
}
