package org.project.ttokttok.domain.applicant.service.answer;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 지원서 파일 답변 업로드
 *
 * <p>답변 조립 과정에서 유일하게 인프라(S3)에 의존하는 부분을 이 클래스로 격리합니다.
 * 덕분에 {@link AnswerAssembler} 와 {@code ApplicantUserService} 는 저장소 구현을 알지 못합니다.
 */
@Component
@RequiredArgsConstructor
public class FileAnswerUploader {

    private static final String APPLICANT_DIRECTORY_PREFIX = "applicant/";

    private final S3Service s3Service;

    /**
     * 지원자별 디렉터리에 파일을 업로드하고 접근 URL을 반환한다.
     *
     * @param file           업로드할 파일
     * @param applicantEmail 지원자 이메일 (업로드 경로 구분에 사용)
     * @return 업로드된 파일의 URL
     */
    public String upload(MultipartFile file, String applicantEmail) {
        return s3Service.uploadFile(file, APPLICANT_DIRECTORY_PREFIX + applicantEmail + "/");
    }
}
