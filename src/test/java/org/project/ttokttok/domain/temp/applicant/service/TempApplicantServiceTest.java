package org.project.ttokttok.domain.temp.applicant.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempAnswer;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempApplicantSaveRequest;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.project.ttokttok.domain.temp.applicant.repository.TempApplicantRepository;
import org.project.ttokttok.domain.temp.applicant.service.dto.request.TempApplicantSaveServiceRequest;
import org.project.ttokttok.domain.temp.applicant.service.dto.response.TempApplicantDataServiceResponse;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TempApplicantServiceTest {

    @Mock
    private TempApplicantRepository tempApplicantRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private TempApplicantService tempApplicantService;

    private TempApplicantSaveServiceRequest buildRequest(
            String email, String formId, List<TempAnswer> answers,
            List<String> questionIds, List<MultipartFile> files
    ) {
        TempApplicantSaveRequest request = new TempApplicantSaveRequest(
                "홍길동", 22, "컴퓨터공학과", email, "010-1234-5678",
                StudentStatus.ENROLLED, Grade.FIRST_GRADE, Gender.MALE, answers
        );

        return TempApplicantSaveServiceRequest.of(email, formId, request, questionIds, files);
    }

    @Test
    @DisplayName("saveTempApplicant(): 기존 임시 지원서가 없으면 새로 생성한다")
    void saveTempApplicant_CreatesNew_WhenNoExisting() {
        // given
        String email = "test@sangmyung.kr";
        String formId = "form-1";
        TempApplicantSaveServiceRequest request = buildRequest(
                email, formId, List.of(new TempAnswer("q1", "답변")), null, null
        );

        given(tempApplicantRepository.findByUserEmailAndFormId(email, formId))
                .willReturn(Optional.empty());

        TempApplicant saved = mock(TempApplicant.class);
        given(saved.getId()).willReturn("temp-1");
        given(tempApplicantRepository.save(any(TempApplicant.class))).willReturn(saved);

        // when
        String result = tempApplicantService.saveTempApplicant(request);

        // then
        assertThat(result).isEqualTo("temp-1");
        verify(tempApplicantRepository, times(1)).save(any(TempApplicant.class));
    }

    @Test
    @DisplayName("saveTempApplicant(): 기존 임시 지원서가 있으면 업데이트한다")
    void saveTempApplicant_UpdatesExisting_WhenPresent() {
        // given
        String email = "test@sangmyung.kr";
        String formId = "form-1";
        TempApplicantSaveServiceRequest request = buildRequest(
                email, formId, List.of(new TempAnswer("q1", "답변")), null, null
        );

        TempApplicant existing = mock(TempApplicant.class);
        given(existing.getId()).willReturn("temp-existing");
        given(tempApplicantRepository.findByUserEmailAndFormId(email, formId))
                .willReturn(Optional.of(existing));

        // when
        String result = tempApplicantService.saveTempApplicant(request);

        // then
        assertThat(result).isEqualTo("temp-existing");
        verify(existing, times(1)).update(any(Map.class));
        verify(tempApplicantRepository, never()).save(any(TempApplicant.class));
    }

    @Test
    @DisplayName("saveTempApplicant(): 파일 응답이 있으면 S3에 업로드하고 답변에 포함시킨다")
    void saveTempApplicant_UploadsFile_WhenFilesPresent() {
        // given
        String email = "test@sangmyung.kr";
        String formId = "form-1";
        MultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "content".getBytes());

        TempApplicantSaveServiceRequest request = buildRequest(
                email, formId, List.of(), List.of("q-file"), List.of(file)
        );

        given(tempApplicantRepository.findByUserEmailAndFormId(email, formId))
                .willReturn(Optional.empty());
        given(s3Service.uploadFile(eq(file), anyString())).willReturn("https://s3.bucket/resume.pdf");

        TempApplicant saved = mock(TempApplicant.class);
        given(saved.getId()).willReturn("temp-1");
        given(tempApplicantRepository.save(any(TempApplicant.class))).willReturn(saved);

        // when
        tempApplicantService.saveTempApplicant(request);

        // then
        verify(s3Service, times(1)).uploadFile(file, "temp-applicants/" + email + "/");

        ArgumentCaptor<TempApplicant> captor = ArgumentCaptor.forClass(TempApplicant.class);
        verify(tempApplicantRepository).save(captor.capture());

        @SuppressWarnings("unchecked")
        List<TempAnswer> savedAnswers = (List<TempAnswer>) captor.getValue().getTempData().get("answers");
        assertThat(savedAnswers)
                .extracting(TempAnswer::questionId, TempAnswer::value)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("q-file", "https://s3.bucket/resume.pdf"));
    }

    @Test
    @DisplayName("getTempApplicantData(): 임시 지원서가 존재하면 데이터를 반환한다")
    void getTempApplicantData_WhenExists() {
        // given
        String email = "test@sangmyung.kr";
        String formId = "form-1";
        Map<String, Object> tempData = new HashMap<>();
        tempData.put("name", "홍길동");

        TempApplicant existing = mock(TempApplicant.class);
        given(existing.getTempData()).willReturn(tempData);
        given(tempApplicantRepository.findByUserEmailAndFormId(email, formId))
                .willReturn(Optional.of(existing));

        // when
        TempApplicantDataServiceResponse response = tempApplicantService.getTempApplicantData(email, formId);

        // then
        assertThat(response.hasTempData()).isTrue();
        assertThat(response.data()).isEqualTo(tempData);
    }

    @Test
    @DisplayName("getTempApplicantData(): 임시 지원서가 없으면 hasTempData가 false이고 데이터는 null이다")
    void getTempApplicantData_WhenNotExists() {
        // given
        String email = "test@sangmyung.kr";
        String formId = "form-1";
        given(tempApplicantRepository.findByUserEmailAndFormId(email, formId))
                .willReturn(Optional.empty());

        // when
        TempApplicantDataServiceResponse response = tempApplicantService.getTempApplicantData(email, formId);

        // then
        assertThat(response.hasTempData()).isFalse();
        assertThat(response.data()).isNull();
    }
}
