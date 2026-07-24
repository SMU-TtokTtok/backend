package org.project.ttokttok.infrastructure.s3.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.infrastructure.s3.enums.S3FileDirectory;
import org.project.ttokttok.infrastructure.s3.exception.S3FileUploadException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private ContentValidatable validator;

    @Mock
    private S3KeyUrlGenerator keyUrlGenerator;

    private S3Service s3Service;

    private static final String TEST_BUCKET = "test-bucket";

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client, validator, keyUrlGenerator);
        ReflectionTestUtils.setField(s3Service, "bucketName", TEST_BUCKET);
    }

    @AfterEach
    void tearDown() {
        // 테스트에서 수동으로 연 트랜잭션 동기화 컨텍스트를 반드시 정리한다 (스레드 로컬 누수 방지).
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("파일 업로드 성공 - 각 컴포넌트 간 협력 검증")
    void uploadFile_Success_ComponentsCollaboration() {
        // Given
        String fileName = "profile.jpg";
        String contentType = "image/jpeg";
        MockMultipartFile file = new MockMultipartFile("file", fileName, contentType, "content".getBytes());
        String dirName = S3FileDirectory.PROFILE_IMAGE.getDirectoryName();

        String expectedKey = "profile-images/uuid_profile.jpg";
        String expectedUrl = "https://cdn.test.com/profile-images/uuid_profile.jpg";

        // Mock 설정
        when(keyUrlGenerator.generateKey(dirName, fileName)).thenReturn(expectedKey);
        when(keyUrlGenerator.createUrl(expectedKey)).thenReturn(expectedUrl);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // When
        String result = s3Service.uploadFile(file, dirName);

        // Then
        assertEquals(expectedUrl, result);

        // 컴포넌트 간 협력 검증
        verify(validator).validateNotEmpty(file);
        verify(validator).validateSize(file.getSize());
        verify(validator).validateType(contentType);
        verify(validator).validateFileName(fileName);
        verify(validator).validateArchive(file);
        verify(keyUrlGenerator).generateKey(dirName, fileName);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(keyUrlGenerator).createUrl(expectedKey);
    }

    @Test
    @DisplayName("파일 업로드 실패 - 검증 실패 시 S3 업로드 시도 안 함")
    void uploadFile_ValidationFailure_DoesNotAttemptS3Upload() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", "content".getBytes());
        String dirName = S3FileDirectory.PROFILE_IMAGE.getDirectoryName();

        RuntimeException validationException = new RuntimeException("파일 크기가 5MB를 초과합니다");
        doThrow(validationException).when(validator).validateSize(anyLong());

        // When & Then
        assertThatThrownBy(() -> s3Service.uploadFile(file, dirName))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("파일 크기가 5MB를 초과합니다");

        verify(validator).validateNotEmpty(file);
        verify(validator).validateSize(file.getSize());
        // 검증 실패 후에는 다른 작업들이 호출되지 않아야 함
        verify(validator, never()).validateType(anyString());
        verify(validator, never()).validateFileName(anyString());
        verify(validator, never()).validateArchive(any());
        verify(keyUrlGenerator, never()).generateKey(anyString(), anyString());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - I/O 오류 시 S3FileUploadException 발생")
    void uploadFile_IOError_ThrowsS3FileUploadException() throws IOException {
        // Given
        String fileName = "corrupted.jpg";
        String contentType = "image/jpeg";
        String dirName = S3FileDirectory.PROFILE_IMAGE.getDirectoryName();
        String expectedKey = "profile-images/uuid_corrupted.jpg";

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn(fileName);
        when(mockFile.getContentType()).thenReturn(contentType);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getInputStream()).thenThrow(new IOException("파일 읽기 실패"));

        when(keyUrlGenerator.generateKey(dirName, fileName)).thenReturn(expectedKey);

        // When & Then
        assertThatThrownBy(() -> s3Service.uploadFile(mockFile, dirName))
                .isInstanceOf(S3FileUploadException.class);

        verify(validator).validateNotEmpty(mockFile);
        verify(validator).validateSize(1024L);
        verify(validator).validateType(contentType);
        verify(validator).validateFileName(fileName);
        verify(validator).validateArchive(mockFile);
        verify(keyUrlGenerator).generateKey(dirName, fileName);
        // I/O 에러로 인해 S3 업로드는 시도되지 않음
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(keyUrlGenerator, never()).createUrl(anyString());
    }

    @Test
    @DisplayName("파일 삭제 성공 - URL에서 키 추출 후 S3 삭제")
    void deleteFile_Success() {
        // Given
        String cloudFrontUrl = "https://cdn.test.com/profile-images/uuid_test.jpg";
        String expectedKey = "profile-images/uuid_test.jpg";

        when(keyUrlGenerator.extractKeyFromUrl(cloudFrontUrl)).thenReturn(expectedKey);

        // When
        s3Service.deleteFile(cloudFrontUrl);

        // Then
        verify(keyUrlGenerator).extractKeyFromUrl(cloudFrontUrl);
        verify(s3Client).deleteObject(DeleteObjectRequest.builder()
                .bucket(TEST_BUCKET)
                .key(expectedKey)
                .build());
    }

    @Test
    @DisplayName("파일 삭제 실패 - 잘못된 URL 형식")
    void deleteFile_InvalidUrl_ThrowsException() {
        // Given
        String invalidUrl = "https://malicious-domain.com/file.jpg";

        when(keyUrlGenerator.extractKeyFromUrl(invalidUrl))
                .thenThrow(new IllegalArgumentException("Invalid CloudFront URL format"));

        // When & Then
        assertThatThrownBy(() -> s3Service.deleteFile(invalidUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid CloudFront URL format");

        verify(keyUrlGenerator).extractKeyFromUrl(invalidUrl);
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("다양한 파일 형식 업로드 - 컨텐츠 타입 올바르게 전달")
    void uploadFile_VariousFileTypes_CorrectContentType() {
        // Given
        String fileName = "document.pdf";
        String contentType = "application/pdf";
        MockMultipartFile file = new MockMultipartFile("file", fileName, contentType, "content".getBytes());
        String dirName = S3FileDirectory.BOARD_FILE.getDirectoryName();

        String expectedKey = "board-files/uuid_document.pdf";
        String expectedUrl = "https://cdn.test.com/board-files/uuid_document.pdf";

        when(keyUrlGenerator.generateKey(dirName, fileName)).thenReturn(expectedKey);
        when(keyUrlGenerator.createUrl(expectedKey)).thenReturn(expectedUrl);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // When
        String result = s3Service.uploadFile(file, dirName);

        // Then
        assertEquals(expectedUrl, result);
        verify(validator).validateType(contentType);
        verify(keyUrlGenerator).generateKey(dirName, fileName);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // ------- 트랜잭션 연동 삭제 (tx-aware) -------

    private static final String FILE_URL = "https://cdn.test.com/board-images/uuid_thumb.png";

    private void stubExtractKey() {
        lenient().when(keyUrlGenerator.extractKeyFromUrl(FILE_URL))
                .thenReturn("board-images/uuid_thumb.png");
    }

    private void triggerAfterCommit() {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
    }

    private void triggerAfterCompletion(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(status));
    }

    @Nested
    @DisplayName("deleteFileAfterCommit()")
    class DeleteFileAfterCommit {

        @Test
        @DisplayName("활성 트랜잭션이 있으면 커밋 전에는 삭제하지 않고, 커밋 이후에 삭제한다.")
        void deletesOnlyAfterCommit() {
            stubExtractKey();
            TransactionSynchronizationManager.initSynchronization();

            s3Service.deleteFileAfterCommit(FILE_URL);

            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));

            triggerAfterCommit();

            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("트랜잭션이 롤백되면(afterCommit 미호출) 파일을 삭제하지 않는다.")
        void doesNotDeleteOnRollback() {
            TransactionSynchronizationManager.initSynchronization();

            s3Service.deleteFileAfterCommit(FILE_URL);

            triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("활성 트랜잭션이 없으면 즉시 삭제한다.")
        void deletesImmediatelyWithoutTransaction() {
            stubExtractKey();

            s3Service.deleteFileAfterCommit(FILE_URL);

            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("커밋 이후 삭제가 실패해도 예외를 전파하지 않는다.")
        void swallowsDeleteFailureAfterCommit() {
            stubExtractKey();
            TransactionSynchronizationManager.initSynchronization();
            when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .thenThrow(new RuntimeException("s3 down"));

            s3Service.deleteFileAfterCommit(FILE_URL);

            assertThatCode(S3ServiceTest.this::triggerAfterCommit)
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("deleteFileOnRollback()")
    class DeleteFileOnRollback {

        @Test
        @DisplayName("트랜잭션이 롤백되면 파일을 삭제한다.")
        void deletesOnRollback() {
            stubExtractKey();
            TransactionSynchronizationManager.initSynchronization();

            s3Service.deleteFileOnRollback(FILE_URL);

            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));

            triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("트랜잭션이 커밋되면 파일을 삭제하지 않는다.")
        void doesNotDeleteOnCommit() {
            TransactionSynchronizationManager.initSynchronization();

            s3Service.deleteFileOnRollback(FILE_URL);

            triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);

            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("커밋 여부가 불명확하면(STATUS_UNKNOWN) 파일을 삭제하지 않는다.")
        void doesNotDeleteOnUnknownStatus() {
            TransactionSynchronizationManager.initSynchronization();

            s3Service.deleteFileOnRollback(FILE_URL);

            triggerAfterCompletion(TransactionSynchronization.STATUS_UNKNOWN);

            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("활성 트랜잭션이 없으면 아무 동작도 하지 않는다.")
        void noOpWithoutTransaction() {
            s3Service.deleteFileOnRollback(FILE_URL);

            verifyNoInteractions(s3Client);
        }

        @Test
        @DisplayName("롤백 시 삭제가 실패해도 예외를 전파하지 않는다.")
        void swallowsDeleteFailureOnRollback() {
            stubExtractKey();
            TransactionSynchronizationManager.initSynchronization();
            when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .thenThrow(new RuntimeException("s3 down"));

            s3Service.deleteFileOnRollback(FILE_URL);

            assertThatCode(() -> triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK))
                    .doesNotThrowAnyException();
        }
    }
}