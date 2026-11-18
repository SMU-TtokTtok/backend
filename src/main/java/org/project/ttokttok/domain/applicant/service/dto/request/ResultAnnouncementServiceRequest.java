package org.project.ttokttok.domain.applicant.service.dto.request;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.project.ttokttok.domain.applicant.domain.ResultAnnouncement;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.exception.ResultAnnouncementException;
import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;

public record ResultAnnouncementServiceRequest(
        String username,
        String clubId,
        ApplicantPhase kind,
        String idempotencyKey,
        SendResultMailServiceRequest mail
) {
    public ResultAnnouncementServiceRequest {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > ResultAnnouncement.IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new ResultAnnouncementException(ErrorMessage.IDEMPOTENCY_KEY_INVALID);
        }
    }

    // 같은 키로 온 요청이 처음 요청과 같은지 비교할 값. 전형과 메일 내용이 같으면 같다.
    // 저장된 값과 비교하므로 형식을 바꾸면 이미 받은 키의 재요청이 422가 된다. 값마다 길이를 앞에 붙여 경계가 섞이지 않게 한다.
    public String fingerprint() {
        StringBuilder canonical = new StringBuilder(kind.name());
        appendContent(canonical, mail.pass());
        appendContent(canonical, mail.fail());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256은 모든 JVM이 제공해야 한다", e);
        }
    }

    private static void appendContent(StringBuilder canonical, ResultMailContent content) {
        if (content == null) {
            canonical.append("|-");
            return;
        }
        appendValue(canonical, content.title());
        appendValue(canonical, content.body());
    }

    private static void appendValue(StringBuilder canonical, String value) {
        if (value == null) {
            canonical.append("|~");
            return;
        }
        canonical.append('|').append(value.length()).append(':').append(value);
    }
}
