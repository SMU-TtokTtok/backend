package org.project.ttokttok.infrastructure.s3.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applicant.scheduler.ApplicantCleanupScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * #456 고아 첨부 정리가 실제 S3 API(로컬 MinIO)에서 1000개가 넘는 목록을 끝까지 읽고, 접두사 밖은 건드리지 않는지 확인한다.
 *
 * <p>전용 로컬 MinIO 컨테이너에서만 실행한다. 운영 버킷에 쓰지 않는다. 테스트마다 새 버킷을 만들고 끝나면 지운다.
 * TTOKTTOK_MINIO_ENDPOINT(http://127.0.0.1:포트), TTOKTTOK_MINIO_ACCESS_KEY, TTOKTTOK_MINIO_SECRET_KEY 가 필요하다.
 */
@EnabledIfEnvironmentVariable(named = "TTOKTTOK_MINIO_ENDPOINT", matches = "http://127\\.0\\.0\\.1:\\d+")
class S3ServiceMinioTest {

    private static final int APPLICANT_FILES = 1005; // ListObjectsV2 한 페이지(1000개)를 넘긴다

    private S3Client s3Client;
    private S3Service s3Service;
    private final String bucket = "ttokttok-456-" + UUID.randomUUID();

    @BeforeEach
    void setUp() {
        s3Client = S3Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .endpointOverride(URI.create(System.getenv("TTOKTTOK_MINIO_ENDPOINT")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        System.getenv("TTOKTTOK_MINIO_ACCESS_KEY"), System.getenv("TTOKTTOK_MINIO_SECRET_KEY"))))
                .build();
        s3Client.createBucket(b -> b.bucket(bucket));
        s3Service = new S3Service(s3Client, null, new S3KeyUrlGenerator());
        ReflectionTestUtils.setField(s3Service, "bucketName", bucket);

        IntStream.range(0, APPLICANT_FILES).parallel().forEach(i -> put("applicant/u" + i + "@x.kr/f.pdf"));
        put("temp-applicants/u@x.kr/draft.pdf");
    }

    @AfterEach
    void tearDown() {
        s3Client.listObjectsV2Paginator(b -> b.bucket(bucket)).contents()
                .forEach(object -> s3Client.deleteObject(b -> b.bucket(bucket).key(object.key())));
        s3Client.deleteBucket(b -> b.bucket(bucket));
        s3Client.close();
    }

    @Test
    void 접두사_아래_객체를_1000개가_넘어도_끝까지_조회하고_지정한_키만_삭제한다() {
        List<S3Object> listed = s3Service.listObjects("applicant/");

        assertThat(listed).hasSize(APPLICANT_FILES).allMatch(object -> object.key().startsWith("applicant/"));

        s3Service.deleteObject("applicant/u0@x.kr/f.pdf");

        assertThat(s3Service.listObjects("applicant/")).hasSize(APPLICANT_FILES - 1)
                .noneMatch(object -> object.key().equals("applicant/u0@x.kr/f.pdf"));
        assertThat(s3Service.listObjects("temp-applicants/")).hasSize(1);
    }

    @Test
    void 정리_작업은_방금_올라온_파일을_참조가_없어도_지우지_않는다() {
        ApplicantRepository applicantRepository = mock(ApplicantRepository.class);
        when(applicantRepository.findAllDocumentAnswers()).thenReturn(List.of());
        ApplicantCleanupScheduler scheduler = new ApplicantCleanupScheduler(
                null, null, null, applicantRepository, s3Service, new S3KeyUrlGenerator());

        assertThat(scheduler.deleteOrphanApplicantFiles()).isZero();
        assertThat(s3Service.listObjects("")).hasSize(APPLICANT_FILES + 1);
    }

    private void put(String key) {
        s3Client.putObject(b -> b.bucket(bucket).key(key), RequestBody.fromString("x"));
    }
}
