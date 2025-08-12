package org.project.ttokttok.domain.applicant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applicant.controller.dto.request.AnswerRequest;
import org.project.ttokttok.domain.applicant.controller.dto.request.ApplyFormRequest;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.exception.AlreadyApplicantExistsException;
import org.project.ttokttok.domain.applicant.exception.AnswerRequestNotMatchException;
import org.project.ttokttok.domain.applicant.exception.ListSizeNotMatchException;
import org.project.ttokttok.domain.applicant.exception.QuestionParseFailException;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applicant.repository.dto.UserApplicationHistoryQueryResponse;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.club.service.dto.response.ClubCardServiceResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;
import static org.project.ttokttok.domain.applyform.domain.enums.QuestionType.FILE;

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

        // 1. 타겟 사용자 검증
        validateUserExists(email);

        ApplyForm form = applyFormRepository.findByClubIdAndStatus(clubId, ACTIVE)
                .orElseThrow(ApplyFormNotFoundException::new);

        // 2. 중복 지원 검증
        validateApplicantExists(email, form.getId());

        List<Question> questions = form.getFormJson();

        // 3. questionIds와 files의 유효성 검사 (질문 타입 고려)
        validateQuestionIdsAndFiles(questionIds, files, questions);

        List<Answer> answers = getAnswers(
                request.answers(),
                questionIds,
                files,
                questions,
                email
        );

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
                form
        );

        // 4. 답변 제출 (서류 전형 생성)
        applicant.submitDocument(answers);

        return applicantRepository.save(applicant)
                .getId();
    }

    private void validateApplicantExists(String email, String formId) {
        if (applicantRepository.existsByUserEmailAndApplyFormId(email, formId)) {
            throw new AlreadyApplicantExistsException();
        }
    }

    // FIXME: 리팩토링 필수
    private List<Answer> getAnswers(
            List<AnswerRequest> answers,
            List<String> questionIds,
            List<MultipartFile> files,
            List<Question> questions,
            String email
    ) {
        return answers.stream()
                .map(answerRequest ->
                        answerParseLogic(
                                questions,
                                answerRequest,
                                questionIds,
                                files,
                                email
                        )
                )
                .toList();
    }

    //TODO: 복잡도가 높아 리팩토링 필요
    private Answer answerParseLogic(List<Question> questions,
                                    AnswerRequest answerRequest,
                                    List<String> questionIds,
                                    List<MultipartFile> files,
                                    String email) {

        // 1. 질문이 파일 타입인 경우,
        // files에서 questionId와 일치하는 인덱스를 가진 파일을 찾아 업로드
        Question question = questions.stream()
                .filter(q -> q.questionId().equals(answerRequest.questionId()))
                .findFirst()
                .orElseThrow(QuestionParseFailException::new);

        if (question.questionType() == FILE) {
            // 파일 질문인데 파일이 없는 경우 처리
            if (questionIds == null || files == null || 
                questionIds.isEmpty() || files.isEmpty()) {
                // 파일이 필수가 아닌 경우 빈 값으로 처리
                return new AnswerRequest(answerRequest.questionId(), "").toAnswer(question);
            }

            // questionIds에서 현재 questionId의 인덱스를 찾음
            int fileIndex = questionIds.indexOf(answerRequest.questionId());
            if (fileIndex == -1 || fileIndex >= files.size()) {
                // 파일이 필수가 아닌 경우 빈 값으로 처리
                return new AnswerRequest(answerRequest.questionId(), "").toAnswer(question);
            }

            // 파일 업로드 처리
            MultipartFile file = files.get(fileIndex);
            if (file == null || file.isEmpty()) {
                // 파일이 필수가 아닌 경우 빈 값으로 처리
                return new AnswerRequest(answerRequest.questionId(), "").toAnswer(question);
            }

            String fileUrl = s3Service.uploadFile(file, "applicant/" + email + "/");
            return new AnswerRequest(answerRequest.questionId(), fileUrl).toAnswer(question);
        }

        return answerRequest.toAnswer(question);
    }

    private void validateUserExists(String email) {
        if (!userRepository.existsByEmail(email))
            throw new UserNotFoundException();
    }

    private void validateQuestionIdsAndFiles(List<String> questionIds, List<MultipartFile> files, List<Question> questions) {
        // 1. 둘 다 null이거나 빈 리스트인 경우 통과
        if ((questionIds == null || questionIds.isEmpty()) && 
            (files == null || files.isEmpty())) {
            return;
        }

        // 2. 파일 질문이 있는지 확인
        boolean hasFileQuestions = questions.stream()
                .anyMatch(q -> q.questionType() == FILE);

        // 3. 파일 질문이 없으면 questionIds와 files가 일치할 필요 없음
        if (!hasFileQuestions) {
            return;
        }

        // 4. 파일 질문이 있으면 questionIds와 files의 일관성 검사
        if (questionIds != null && !questionIds.isEmpty()) {
            // 파일 질문 ID만 추출
            List<String> fileQuestionIds = questionIds.stream()
                    .filter(id -> questions.stream()
                            .anyMatch(q -> q.questionId().equals(id) && q.questionType() == FILE))
                    .toList();

            // 파일 질문이 있으면 files도 있어야 함
            if (!fileQuestionIds.isEmpty()) {
                if (files == null || files.isEmpty()) {
                    throw new AnswerRequestNotMatchException();
                }
                
                // 파일 질문 개수와 파일 개수가 일치해야 함
                if (fileQuestionIds.size() != files.size()) {
                    throw new ListSizeNotMatchException();
                }
            }
        }
    }

    /**
     * 사용자의 동아리 지원내역 조회
     * 무한스크롤과 정렬 기능을 지원합니다.
     *
     * @param userEmail 사용자 이메일
     * @param size 조회할 개수
     * @param cursor 커서 (무한스크롤용)
     * @param sort 정렬 방식 (latest, popular, member_count)
     * @return 사용자 지원내역 목록과 페이징 정보
     */
    @Transactional(readOnly = true)
    public ClubListServiceResponse getUserApplicationHistory(String userEmail,
                                                           int size,
                                                           String cursor,
                                                           String sort) {
        // 1. 사용자 존재 여부 검증
        validateUserExists(userEmail);

        // 2. 지원내역 조회 (size+1로 조회하여 hasNext 확인)
        List<UserApplicationHistoryQueryResponse> results = applicantRepository.getUserApplicationHistory(
                userEmail, size, cursor, sort
        );

        // 3. hasNext 확인을 위해 size+1로 조회했으므로
        boolean hasNext = results.size() > size;
        if (hasNext) {
            results = results.subList(0, size);  // 실제 size만큼만 반환
        }

        // 4. 다음 커서 생성
        String nextCursor = null;
        if (hasNext && !results.isEmpty()) {
            UserApplicationHistoryQueryResponse lastItem = results.get(results.size() - 1);
            nextCursor = generateNextCursor(lastItem.applicantId(), sort);
        }

        // 5. ClubCardServiceResponse로 변환
        List<ClubCardServiceResponse> clubs = results.stream()
                .map(this::toClubCardServiceResponse)
                .toList();

        return new ClubListServiceResponse(
                clubs,
                clubs.size(),
                0L, // totalCount는 별도 조회가 필요하지만 무한스크롤에서는 생략
                hasNext,
                nextCursor
        );
    }

    /**
     * UserApplicationHistoryQueryResponse를 ClubCardServiceResponse로 변환
     */
    private ClubCardServiceResponse toClubCardServiceResponse(UserApplicationHistoryQueryResponse queryResponse) {
        return new ClubCardServiceResponse(
                queryResponse.clubId(),
                queryResponse.clubName(),
                queryResponse.clubType(),
                queryResponse.clubCategory(),
                queryResponse.customCategory(),
                queryResponse.summary(),
                queryResponse.profileImageUrl(),
                queryResponse.clubMemberCount(),
                queryResponse.recruiting(),
                queryResponse.bookmarked()
        );
    }

    /**
     * 정렬 방식에 따라 다음 커서 생성
     *
     * @param lastItemId 마지막으로 조회된 아이템의 ID
     * @param sort 정렬 방식
     * @return 다음 커서 문자열
     */
    private String generateNextCursor(String lastItemId, String sort) {
        // 정렬 방식에 따라 다른 커서 생성
        // 현재는 간단하게 ID만 사용하지만, 향후 복합 커서로 개선 가능
        return switch (sort.toLowerCase()) {
            case "latest", "popular", "member_count" -> lastItemId;
            default -> lastItemId;
        };
    }
}
