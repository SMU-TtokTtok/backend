package org.project.ttokttok.domain.applicant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applicant.controller.dto.request.ApplyFormRequest;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.json.Answer;
import org.project.ttokttok.domain.applicant.exception.AlreadyApplicantExistsException;
import org.project.ttokttok.domain.applicant.repository.ApplicantRepository;
import org.project.ttokttok.domain.applicant.repository.dto.UserApplicationHistoryQueryResponse;
import org.project.ttokttok.domain.applicant.service.answer.AnswerAssembler;
import org.project.ttokttok.domain.applicant.service.answer.AnswerSubmission;
import org.project.ttokttok.domain.applyform.domain.ApplyDeadlinePolicy;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.exception.ApplyFormNotFoundException;
import org.project.ttokttok.domain.applyform.repository.ApplyFormRepository;
import org.project.ttokttok.domain.club.service.dto.response.ClubCardServiceResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
import org.project.ttokttok.domain.temp.applicant.repository.TempApplicantRepository;
import org.project.ttokttok.domain.user.exception.UserNotFoundException;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.project.ttokttok.domain.applyform.domain.enums.ApplyFormStatus.ACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicantUserService {

    private final UserRepository userRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplyFormRepository applyFormRepository;
    private final TempApplicantRepository tempApplicantRepository;
    private final AnswerAssembler answerAssembler;

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

        // 3. 답변 검증 및 조립 (파일 질문 처리 포함)
        List<Answer> answers = answerAssembler.assemble(
                new AnswerSubmission(request.answers(), questionIds, files),
                form.getFormJson(),
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

        // 임시 지원폼 존재 여부 확인 후 삭제
        tempApplicantRepository.findByUserEmailAndFormId(email, form.getId())
                .ifPresent(tempApplicantRepository::delete);

        return applicantRepository.save(applicant)
                .getId();
    }

    private void validateApplicantExists(String email, String formId) {
        if (applicantRepository.existsByUserEmailAndApplyFormId(email, formId)) {
            throw new AlreadyApplicantExistsException();
        }
    }

    private void validateUserExists(String email) {
        if (!userRepository.existsByEmail(email))
            throw new UserNotFoundException();
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
            nextCursor = results.get(results.size() - 1).applicantId();
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
        boolean isDeadlineImminent = ApplyDeadlinePolicy.isImminent(queryResponse.applyEndDate());

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
                queryResponse.bookmarked(),
                isDeadlineImminent
        );
    }

}
