package org.project.ttokttok.infrastructure.s3.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.infrastructure.s3.exception.S3FileUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ContentValidatable validator;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${file-cloud.url}")
    private String fileCloudUrl;

    public String uploadFile(MultipartFile file, String dirName) {

        validateFile(file);

        String key = createFileKey(dirName);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            return key;
        } catch (IOException e) {
            throw new S3FileUploadException();
        }
    }

    public void deleteFile(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }

    private void validateFile(MultipartFile file) {
        validator.validateContent(file);
        validator.validateSize(file.getSize());
        validator.validateType(file.getContentType());
        validator.validateFileName(file.getOriginalFilename());
    }

    private String createFileKey(String dirName) {
        return fileCloudUrl + "/" + dirName + UUID.randomUUID();
    }
}
