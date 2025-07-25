package org.project.ttokttok.infrastructure.email.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailRequest {
    private final String to;           // 받는 사람
    private final String subject;      // 제목
    private final String content;      // 내용
    private final boolean isHtml;      // HTML 여부

    // 이메일 인증코드 발송용 팩토리 메서드
    public static EmailRequest createVerificationEmail(String email, String code) {
        return EmailRequest.builder()
                .to(email)
                .subject("[똑똑] 이메일 인증코드")
                .content(createVerificationContent(code))
                .isHtml(true)
                .build();
    }

    // 비밀번호 재설정용 팩토리 메서드
    public static EmailRequest createPasswordResetEmail(String email, String code) {
        return EmailRequest.builder()
                .to(email)
                .subject("[똑똑] 비밀번호 재설정 인증코드")
                .content(createPasswordResetContent(code))
                .isHtml(true)
                .build();
    }

    // 동아리 지원 결과 안내용 팩토리 메서드
    public static EmailRequest createResultEmail(String email, String resultBody) {
        return EmailRequest.builder()
                .to(email)
                .subject("[똑똑] 지원 결과 안내")
                .content(createResultContent(resultBody))
                .isHtml(true)
                .build();
    }

    private static String createVerificationContent(String code) {
        return String.format("""
            <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>
                <h2>똑똑 이메일 인증</h2>
                <p>아래 인증코드를 입력해주세요:</p>
                <div style='background: #f5f5f5; padding: 20px; text-align: center; font-size: 24px; font-weight: bold;'>
                    %s
                </div>
                <p>인증코드는 5분간 유효합니다.</p>
            </div>
            """, code);
    }

    private static String createPasswordResetContent(String code) {
        return String.format("""
            <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>
                <h2>똑똑 비밀번호 재설정</h2>
                <p>아래 인증코드를 입력해주세요:</p>
                <div style='background: #f5f5f5; padding: 20px; text-align: center; font-size: 24px; font-weight: bold;'>
                    %s
                </div>
                <p>인증코드는 5분간 유효합니다.</p>
            </div>
            """, code);
    }

    private static String createResultContent(String resultBody) {
        return String.format("""
            <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>
                <h2>동아리 지원 결과 안내</h2>
                <p>%s</p>
                <p>자세한 사항은 동아리 관리자에게 문의해주세요.</p>
            </div>
            """, resultBody);
    }
}