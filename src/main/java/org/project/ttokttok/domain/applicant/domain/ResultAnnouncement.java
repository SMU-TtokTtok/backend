package org.project.ttokttok.domain.applicant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.exception.ResultAnnouncementException;
import org.project.ttokttok.global.entity.BaseTimeEntity;
import org.project.ttokttok.global.exception.ErrorMessage;

/**
 * 한 지원 폼·전형의 결과 발표 기록. 발송 작업({@code result_mail_deliveries.request_id})이 이 id를 가리킨다.
 */
@Entity
@Getter
@Table(name = "result_announcements", uniqueConstraints = {
        @UniqueConstraint(name = "uk_result_announcements_club_key", columnNames = {"club_id", "idempotency_key"}),
        @UniqueConstraint(name = "uk_result_announcements_form_phase", columnNames = {"apply_form_id", "phase"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResultAnnouncement extends BaseTimeEntity {

    public static final int IDEMPOTENCY_KEY_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false)
    private String id;

    @Column(name = "club_id", nullable = false, length = 36, updatable = false)
    private String clubId;

    @Column(name = "apply_form_id", nullable = false, length = 36, updatable = false)
    private String applyFormId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ApplicantPhase phase;

    @Column(name = "idempotency_key", nullable = false, length = IDEMPOTENCY_KEY_MAX_LENGTH, updatable = false)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64, updatable = false)
    private String requestFingerprint;

    @Column(name = "pass_mail_count", nullable = false, updatable = false)
    private int passMailCount;

    @Column(name = "fail_mail_count", nullable = false, updatable = false)
    private int failMailCount;

    public static ResultAnnouncement of(String clubId, String applyFormId, ApplicantPhase phase, String idempotencyKey,
                                        String requestFingerprint, int passMailCount, int failMailCount) {
        ResultAnnouncement announcement = new ResultAnnouncement();
        announcement.clubId = clubId;
        announcement.applyFormId = applyFormId;
        announcement.phase = phase;
        announcement.idempotencyKey = idempotencyKey;
        announcement.requestFingerprint = requestFingerprint;
        announcement.passMailCount = passMailCount;
        announcement.failMailCount = failMailCount;
        return announcement;
    }

    // 같은 키 재요청은 처음 결과를 그대로 돌려준다. 키를 다른 내용에 다시 쓰면 무엇을 돌려줄지 정할 수 없어 거절한다.
    public ResultAnnouncement replay(String fingerprint) {
        if (!requestFingerprint.equals(fingerprint)) {
            throw new ResultAnnouncementException(ErrorMessage.IDEMPOTENCY_KEY_REUSED);
        }
        return this;
    }
}
