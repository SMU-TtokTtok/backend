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
        int retrying = 0;
        int failed = 0;
        int recordErrors = 0;
        for (ClaimedResultMail mail : claimed) {
            try {
                SendOutcome outcome = send(mail);
                switch (outcome) {
                    case SENT -> sent++;
                    case RETRYING -> retrying++;
                    case FAILED -> failed++;
                }
            } catch (Exception e) {
                // 기록이 실패해도 배치는 계속한다. 이 행은 임대가 만료되면 다시 회수된다.
                log.error("결과 메일 발송 결과 기록 실패: id={}, error={}", mail.id(), e.getClass().getSimpleName());
                recordErrors++;
            }
        }
        log.info("결과 메일 발송 배치 완료 - 성공: {}건, 재시도 예정: {}건, 최종 실패: {}건, 기록 실패: {}건",
                sent, retrying, failed, recordErrors);
    }

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void deleteFinishedMails() {
        try {
            int deleted = deliveryService.deleteFinished(LocalDateTime.now());
            log.info("끝난 결과 메일 발송 작업 정리 완료: 삭제={}건", deleted);
        } catch (Exception e) {
            log.error("끝난 결과 메일 발송 작업 정리 중 오류 발생", e);
        }
    }

    private SendOutcome send(ClaimedResultMail mail) {
        try {
            emailService.sendResultEmail(mail.recipientEmail(), mail.content());
        } catch (Exception e) {
            boolean willRetry = deliveryService.recordFailure(mail.id(), e, LocalDateTime.now());
            return willRetry ? SendOutcome.RETRYING : SendOutcome.FAILED;
        }
        deliveryService.recordSuccess(mail.id(), LocalDateTime.now());
        return SendOutcome.SENT;
    }

    private enum SendOutcome {
        SENT, RETRYING, FAILED
    }
}
