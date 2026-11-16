package org.project.ttokttok.domain.applicant.service.dto.request;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.project.ttokttok.domain.applicant.domain.ResultAnnouncement;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.exception.ResultAnnouncementException;
import org.project.ttokttok.global.exception.ErrorMessage;

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
    public String fingerprint() {
        String canonical = kind + "\n" + mail.pass() + "\n" + mail.fail();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256은 모든 JVM이 제공해야 한다", e);
        }
    }
}
