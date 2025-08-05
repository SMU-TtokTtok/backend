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
import org.project.ttokttok.domain.applicant.repository.dto.UserApplicationHistoryQueryResponse;
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

        // 1. 중복 지원 검증
        validateApplicantExists(email, clubId);

        // 2. questionIds와 files의 유효성 검사
        validateQuestionIdsAndFiles(questionIds, files);

        // 3. 타겟 사용자 검증
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
                form
        );

        applicant.submitDocument(answers);

        return applicantRepository.save(applicant)
                .getId();
    }

    private void validateApplicantExists(String email, String clubId) {

    }

    private List<Answer> getFileAnswers(
            List<String> questionIds,
            List<MultipartFile> files,
            List<Question> questions,
            String email,
            String formId
    ) {
        // files가 null이거나 비어있으면 빈 리스트 반환
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        
        return IntStream.range(0, files.size())
                .mapToObj(i -> {
                    String questionId = questionIds.get(i);
                    MultipartFile file = files.get(i);
                    Question question = questions.stream()
                            .filter(q -> q.questionId().equals(questionId))
                            .findFirst()
                            .orElseThrow(ApplyFormNotFoundException::new);
                    String fileKey = s3Service.uploadFile(file, "/applicant/" + email + "/" + formId);
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
