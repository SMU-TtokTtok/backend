package org.project.ttokttok.domain.applicant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applicant.controller.dto.request.AnswerRequest;
import org.project.ttokttok.domain.applicant.controller.dto.request.ApplyFormRequest;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.exception.AnswerRequestNotMatchException;
import org.project.ttokttok.domain.applicant.exception.ListSizeNotMatchException;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.user.exception.UserNotFoundException;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicantUserService {

    private final UserRepository userRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplyFormRepository applyFormRepository;
    private final S3Service s3Service;

    @Transactional
    public String apply(String email,
                        ApplyFormRequest request,
                        List<String> questionIds,
                        List<MultipartFile> files,
                        String clubId) {

        // 1. questionIds와 files의 유효성 검사
        validateQuestionIdsAndFiles(questionIds, files);

        // 2. 타겟 사용자 검증
        validateUserExists(email);

        ApplyForm form = applyFormRepository.findByClubIdAndStatus(clubId, ACTIVE)
                .orElseThrow(ApplyFormNotFoundException::new);

        List<Question> questions = form.getFormJson();

        List<Answer> answers = Stream.concat(
                getAnswers(request.answers(), questions).stream(),
                getFileAnswers(questionIds, files, questions, request.email(), request.applyFormId()).stream()
        ).toList();

        // 파일 업로드하고 key값을 반환
        // key값 반환하고, 파일인 응답에 매핑

        Applicant applicant = Applicant.createApplicant(
                email,
                request.name(),
                request.age(),
                request.major(),
                request.email(),
                request.phone(),
                request.studentStatus(),
                request.grade(),
                request.gender(),
                answers,
                form
        );

        return applicantRepository.save(applicant)
                .getId();
    }

    private List<Answer> getFileAnswers(
            List<String> questionIds,
            List<MultipartFile> files,
            List<Question> questions,
            String email,
            String formId
    ) {
        return IntStream.range(0, files.size())
                .mapToObj(i -> {
                    String questionId = questionIds.get(i);
                    MultipartFile file = files.get(i);
                    Question question = questions.stream()
                            .filter(q -> q.questionId().equals(questionId))
                            .findFirst()
                            .orElseThrow(ApplyFormNotFoundException::new);
                    String fileKey = s3Service.uploadFile(file, "applicant/" + email + "/" + formId);
                    return new Answer(
                            question.title(),
                            question.subTitle(),
                            question.questionType(),
                            question.isEssential(),
                            question.content(),
                            fileKey
                    );
                })
                .toList();
    }

    private List<Answer> getAnswers(List<AnswerRequest> answers, List<Question> questions) {
        return answers.stream()
                .map(answerRequest -> answerParseLogic(questions, answerRequest))
                .toList();
    }

    private Answer answerParseLogic(List<Question> questions, AnswerRequest answerRequest) {
        Question question = questions.stream()
                .filter(q -> q.questionId().equals(answerRequest.questionId()))
                .findFirst()
                .orElseThrow(ApplyFormNotFoundException::new);
        return answerRequest.toAnswer(question);
    }

    private void validateUserExists(String email) {
        if (!userRepository.existsByEmail(email))
            throw new UserNotFoundException();
    }

    private void validateQuestionIdsAndFiles(List<String> questionIds, List<MultipartFile> files) {
        // 1. 둘 다 null인지 확인
        if (questionIds == null && files == null) {
            return; // 둘 다 null이면 유효성 검사 통과
        }

        // 2. 둘 중 하나라도 null이 아니면 유효성 검사 실패
        nullListCheck(questionIds, files);

        // 3. questionIds와 files의 크기가 일치하는지 확인
        ListSizeCheck(questionIds.size(), files.size());
    }

    private void nullListCheck(List<String> questionIds, List<MultipartFile> files) {
        if (questionIds == null || files == null) {
            throw new AnswerRequestNotMatchException();
        }
    }

    private void ListSizeCheck(int questionIdsSize, int filesSize) {
        if (questionIdsSize != filesSize) {
            throw new ListSizeNotMatchException();
        }
    }
}
