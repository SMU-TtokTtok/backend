package org.project.ttokttok.domain.temp.applicant.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempAnswer;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.project.ttokttok.domain.temp.applicant.repository.TempApplicantRepository;
import org.project.ttokttok.domain.temp.applicant.service.dto.request.TempApplicantSaveServiceRequest;
import org.project.ttokttok.domain.temp.applicant.service.dto.response.TempApplicantDataServiceResponse;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TempApplicantService {

    private final TempApplicantRepository tempApplicantRepository;
    private final S3Service s3Service;

    /**
     * 임시 지원서를 저장합니다. 기존 임시 지원서가 있다면 업데이트하고, 없다면 새로 생성합니다.
     */
    @Transactional
    public String saveTempApplicant(TempApplicantSaveServiceRequest request) {
        Optional<TempApplicant> optionalTempApplicant =
                tempApplicantRepository.findByUserEmailAndFormId(request.email(), request.formId());

        Map<String, Object> tempData = buildTempData(request);

        if (optionalTempApplicant.isPresent()) {
            TempApplicant tempApplicant = optionalTempApplicant.get();
            tempApplicant.update(tempData);

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

        List<TempAnswer> tempAnswer = new ArrayList<>();

        // 텍스트 답변 추가 로직
        for (TempAnswer answer : request.data().answers()) {
            if (!request.questionIds().contains(answer.questionId())) {
                tempAnswer.add(answer);
            }
        }

        // 파일 질문 추가 로직
        if (request.files() != null && request.questionIds() != null && !request.files().isEmpty()) {
            for (int i = 0; i < request.files().size(); i++) {
                String questionIdFromReq = request.data().answers().get(i).questionId();
                String fileTypeQuestionId = request.questionIds().get(i);

                // 파일 질문이면 S3에 업로드 후 URL 저장
                if (questionIdFromReq.equals(fileTypeQuestionId)) {
                    String uploadedUrl = s3Service.uploadFile(request.files().get(i), "temp-applicants/" + request.email() + "/");
                    tempAnswer.add(new TempAnswer(fileTypeQuestionId, uploadedUrl));
                }
            }
        }

        tempData.put("answers", tempAnswer);

        return tempData;
    }
}
