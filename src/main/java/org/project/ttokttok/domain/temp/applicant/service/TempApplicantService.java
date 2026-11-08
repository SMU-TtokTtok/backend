package org.project.ttokttok.domain.temp.applicant.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.RecruitState;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.exception.ApplyFormRuleException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.temp.applicant.domain.TempAnswer;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.project.ttokttok.domain.temp.applicant.repository.TempApplicantRepository;
import org.project.ttokttok.domain.temp.applicant.service.dto.request.TempApplicantSaveServiceRequest;
import org.project.ttokttok.domain.temp.applicant.service.dto.response.TempApplicantDataServiceResponse;
import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TempApplicantService {

    private final TempApplicantRepository tempApplicantRepository;
    private final ApplyFormRepository applyFormRepository;
    private final S3Service s3Service;

    /**
     * 임시 지원서를 저장합니다. 기존 임시 지원서가 있다면 업데이트하고, 없다면 새로 생성합니다.
     * 모집 중인 지원폼에만 저장할 수 있고, 빈 답변과 미입력 항목이 있는 작성 중 상태도 허용합니다.
     * 업데이트 후에는 새 답변이 더 이상 참조하지 않는 이전 임시 첨부파일을 커밋 후 삭제합니다.
     */
    @Transactional
    public String saveTempApplicant(TempApplicantSaveServiceRequest request) {
        validateOpenForm(request.formId());
        validateStructure(request);

        Optional<TempApplicant> optionalTempApplicant =
                tempApplicantRepository.findByUserEmailAndFormId(request.email(), request.formId());

        Map<String, Object> tempData = buildTempData(request);

        if (optionalTempApplicant.isPresent()) {
            TempApplicant tempApplicant = optionalTempApplicant.get();
            Map<String, Object> oldData = tempApplicant.getTempData();
            tempApplicant.update(tempData);
            deleteReplacedTempFiles(oldData, tempData, request.email());

            return tempApplicant.getId();
        }

        TempApplicant createdApplicant = TempApplicant.create(
                request.formId(),
                request.email(),
                tempData
        );

        return tempApplicantRepository.save(createdApplicant)
                .getId();
    }

    /**
     * 임시 지원서를 조회합니다.
     */
    @Transactional(readOnly = true)
    public TempApplicantDataServiceResponse getTempApplicantData(String userEmail, String formId) {
        Optional<TempApplicant> tempApplicant = tempApplicantRepository.findByUserEmailAndFormId(userEmail, formId);

        boolean exists = tempApplicant.isPresent();

        return TempApplicantDataServiceResponse.builder()
                .hasTempData(exists)
                .data(exists ? tempApplicant.get().getTempData() : null)
                .build();
    }

    private void validateOpenForm(String formId) {
        ApplyForm form = applyFormRepository.findById(formId)
                .orElseThrow(ApplyFormNotFoundException::new);

        if (form.recruitState() != RecruitState.OPEN) {
            throw new ApplyFormRuleException(ErrorMessage.TEMP_APPLY_NOT_OPEN);
        }
    }

    /**
     * 작성 중인 값은 비어 있어도 되므로 구조(답변 목록, 질문 ID와 파일의 짝)만 검사한다.
     */
    private void validateStructure(TempApplicantSaveServiceRequest request) {
        if (request.data().answers() == null) {
            throw new ApplyFormRuleException(ErrorMessage.ANSWER_REQUEST_NOT_MATCH);
        }
        for (TempAnswer answer : request.data().answers()) {
            if (answer == null || answer.questionId() == null || answer.questionId().isBlank()) {
                throw new ApplyFormRuleException(ErrorMessage.INVALID_ANSWER);
            }
        }

        List<String> questionIds = orEmpty(request.questionIds());
        List<MultipartFile> files = orEmpty(request.files());
        if (questionIds.isEmpty() != files.isEmpty()) {
            throw new ApplyFormRuleException(ErrorMessage.ANSWER_REQUEST_NOT_MATCH);
        }
        if (questionIds.size() != files.size()) {
            throw new ApplyFormRuleException(ErrorMessage.LIST_SIZE_NOT_MATCH);
        }
        Set<String> uniqueIds = new HashSet<>();
        for (int i = 0; i < files.size(); i++) {
            String id = questionIds.get(i);
            if (id == null || id.isBlank() || !uniqueIds.add(id) || files.get(i) == null) {
                throw new ApplyFormRuleException(ErrorMessage.INVALID_ANSWER);
            }
        }
    }

    private Map<String, Object> buildTempData(TempApplicantSaveServiceRequest request) {
        Map<String, Object> tempData = new HashMap<>();

        tempData.put("name", request.data().name());
        tempData.put("age", request.data().age());
        tempData.put("major", request.data().major());
        tempData.put("email", request.data().email());
        tempData.put("phone", request.data().phone());
        tempData.put("studentStatus", request.data().studentStatus());
        tempData.put("grade", request.data().grade());
        tempData.put("gender", request.data().gender());

        List<String> questionIds = orEmpty(request.questionIds());
        List<MultipartFile> files = orEmpty(request.files());
        List<TempAnswer> tempAnswer = new ArrayList<>();

        // 텍스트 답변 추가 로직
        for (TempAnswer answer : request.data().answers()) {
            if (!questionIds.contains(answer.questionId())) {
                tempAnswer.add(answer);
            }
        }

        // 파일 질문 추가 로직
        for (int i = 0; i < files.size(); i++) {
            String uploadedUrl = s3Service.uploadFile(files.get(i), tempDir(request.email()));
            s3Service.deleteFileOnRollback(uploadedUrl);
            tempAnswer.add(new TempAnswer(questionIds.get(i), uploadedUrl));
        }

        tempData.put("answers", tempAnswer);

        return tempData;
    }

    private String tempDir(String email) {
        return "temp-applicants/" + email + "/";
    }

    /** 이전 답변의 이 사용자 임시 첨부({@code temp-applicants/{email}/}) 중 새 답변에 없는 것을 커밋 후 지운다. */
    private void deleteReplacedTempFiles(Map<String, Object> oldData, Map<String, Object> newData, String email) {
        Set<Object> kept = answerValues(newData);
        answerValues(oldData).stream()
                .filter(value -> value instanceof String url && url.contains("/" + tempDir(email)) && !kept.contains(url))
                .map(String.class::cast)
                .forEach(s3Service::deleteFileAfterCommit);
    }

    private Set<Object> answerValues(Map<String, Object> data) {
        if (data == null || !(data.get("answers") instanceof List<?> answers)) {
            return Set.of();
        }
        return answers.stream()
                .filter(TempAnswer.class::isInstance)
                .map(answer -> ((TempAnswer) answer).value())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
