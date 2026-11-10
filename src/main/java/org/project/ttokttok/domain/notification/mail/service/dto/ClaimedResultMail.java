package org.project.ttokttok.domain.notification.mail.service.dto;

import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;

/**
 * 워커가 가져간 발송 작업. 엔티티를 트랜잭션 밖으로 넘기지 않으려고 값만 담는다.
 */
public record ClaimedResultMail(String id, String recipientEmail, ResultMailContent content) {
}
