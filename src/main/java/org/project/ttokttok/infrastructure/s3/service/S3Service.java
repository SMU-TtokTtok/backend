package org.project.ttokttok.infrastructure.s3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.infrastructure.s3.exception.S3FileUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final ContentValidatable validator;
    private final S3KeyUrlGenerator keyUrlGenerator;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String dirName) {
        validateFile(file);

        String key = keyUrlGenerator.generateKey(dirName, file.getOriginalFilename());

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            return keyUrlGenerator.createUrl(key);
        } catch (IOException e) {
            throw new S3FileUploadException();
        }
    }

    public void deleteFile(String cloudFrontUrl) {
        String key = keyUrlGenerator.extractKeyFromUrl(cloudFrontUrl);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }

    /**
     * 활성 트랜잭션이 있으면 커밋 이후에, 없으면 즉시 파일을 삭제한다.
     * 커밋 실패로 롤백되면 파일을 삭제하지 않으므로, DB가 여전히 참조하는 파일이 유실되지 않는다.
     * 삭제 실패는 best-effort로 로그만 남긴다.
     */
    public void deleteFileAfterCommit(String cloudFrontUrl) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFileQuietly(cloudFrontUrl);
                }
            });
        } else {
            deleteFileQuietly(cloudFrontUrl);
        }
    }

    /**
     * 활성 트랜잭션이 롤백되면 파일을 삭제한다(업로드 보상 정리).
     * 커밋 여부가 불명확한 STATUS_UNKNOWN에서는 삭제하지 않는다 — 커밋된 데이터가 참조하는
     * 파일을 지우는 최악의 시나리오를 피하기 위함이다. 트랜잭션이 없으면 no-op.
     */
    public void deleteFileOnRollback(String cloudFrontUrl) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        deleteFileQuietly(cloudFrontUrl);
                    }
                }
            });
        }
    }

    private void deleteFileQuietly(String cloudFrontUrl) {
        try {
            deleteFile(cloudFrontUrl);
        } catch (RuntimeException e) {
            log.warn("S3 파일 삭제 실패 (best-effort): {}", cloudFrontUrl, e);
        }
    }

    private void validateFile(MultipartFile file) {
        // 값싼 검증부터 수행하여 fail-fast. 무거운 ZIP 스트리밍 검증(validateArchive)은 마지막.
        validator.validateNotEmpty(file);
        validator.validateSize(file.getSize());
        validator.validateType(file.getContentType());
        validator.validateFileName(file.getOriginalFilename());
        validator.validateArchive(file);
    }
}
