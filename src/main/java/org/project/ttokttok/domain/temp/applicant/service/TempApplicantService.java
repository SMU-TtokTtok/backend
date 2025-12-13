package org.project.ttokttok.domain.temp.applicant.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.controller.dto.request.AnswerRequest;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempApplicantSaveRequest;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.project.ttokttok.domain.temp.applicant.repository.TempApplicantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TempApplicantService {

    private final TempApplicantRepository tempApplicantRepository;

    /**
     * 임시 지원서를 저장합니다.
     * 기존 임시 지원서가 있다면 업데이트하고, 없다면 새로 생성합니다.
     */
    public String saveTempApplicant(String userEmail, TempApplicantSaveRequest request) {
        // 기존 임시 지원서가 있는지 확인
        Optional<TempApplicant> existingTempApplicant =
            tempApplicantRepository.findByUserEmailAndFormId(userEmail, request.formId());

        List<Answer> answers = convertToAnswers(request.answers());

        if (existingTempApplicant.isPresent()) {
            // 기존 임시 지원서 업데이트
            TempApplicant tempApplicant = existingTempApplicant.get();
            updateTempApplicant(tempApplicant, request, answers);
            return tempApplicant.getId();
        } else {
            // 새로운 임시 지원서 생성
            TempApplicant newTempApplicant = TempApplicant.create(
                    request.formId(),
                    userEmail,
                    request.name(),
                    request.age(),
                    request.major(),
                    request.email(),
                    request.phone(),
                    request.studentStatus(),
                    request.grade(),
                    request.gender(),
                    answers
            );

            TempApplicant saved = tempApplicantRepository.save(newTempApplicant);
            return saved.getId();
        }
    }

    /**
     * 특정 사용자의 임시 지원서를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Optional<TempApplicant> getTempApplicant(String userEmail, String formId) {
        return tempApplicantRepository.findByUserEmailAndFormId(userEmail, formId);
    }

    /**
     * 임시 지원서를 삭제합니다.
     */
    public void deleteTempApplicant(String userEmail, String formId) {
        Optional<TempApplicant> tempApplicant =
            tempApplicantRepository.findByUserEmailAndFormId(userEmail, formId);

        tempApplicant.ifPresent(tempApplicantRepository::delete);
    }

    private List<Answer> convertToAnswers(List<AnswerRequest> answerRequests) {
        if (answerRequests == null) {
            return List.of();
        }

        return answerRequests.stream()
                .map(request -> new Answer(
                    null, // title - 임시 저장에서는 불필요
                    null, // subTitle - 임시 저장에서는 불필요
                    null, // questionType - 임시 저장에서는 불필요
                    false, // isEssential - 임시 저장에서는 불필요
                    List.of(request.questionId()), // content에 questionId 저장
                    request.value()
                ))
                .collect(Collectors.toList());
    }

    private void updateTempApplicant(TempApplicant tempApplicant,
                                   TempApplicantSaveRequest request,
                                   List<Answer> answers) {
        tempApplicant.update(
                request.name(),
                request.age(),
                request.major(),
                request.email(),
                request.phone(),
                request.studentStatus(),
                request.grade(),
                request.gender(),
                answers
        );
    }
}
