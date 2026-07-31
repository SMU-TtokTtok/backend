package org.project.ttokttok.domain.applicant.service.answer;

import org.project.ttokttok.domain.applicant.controller.dto.request.AnswerRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * 지원서 제출 시 함께 올라온 답변 원본 데이터
 *
 * <p>multipart 요청으로 나뉘어 들어오는 세 값(답변 목록, 파일 질문 ID 목록, 파일 목록)을
 * 한 덩어리로 묶습니다. 파일은 {@code questionIds} 와 {@code files} 의 <b>같은 인덱스끼리</b>
 * 대응하는 병렬 리스트 구조로 전달됩니다.
 *
 * @param answers     답변 목록
 * @param questionIds 파일이 첨부된 질문 ID 목록 (null 허용)
 * @param files       업로드된 파일 목록 (null 허용)
 */
public record AnswerSubmission(
        List<AnswerRequest> answers,
        List<String> questionIds,
        List<MultipartFile> files
) {

    /** 파일 관련 입력이 아예 없는지 여부 */
    public boolean hasNoFileInput() {
        return isEmpty(questionIds) && isEmpty(files);
    }

    /** 파일 질문 ID가 하나라도 전달됐는지 여부 */
    public boolean hasQuestionIds() {
        return !isEmpty(questionIds);
    }

    /**
     * 주어진 질문 ID에 대응하는 업로드 파일을 찾는다.
     *
     * <p>대응하는 파일이 없거나 비어 있으면 {@link Optional#empty()} 를 반환한다.
     * 파일 질문이 필수가 아닐 수 있으므로 이는 예외 상황이 아니다.
     */
    public Optional<MultipartFile> findFileFor(String questionId) {
        if (isEmpty(questionIds) || isEmpty(files)) {
            return Optional.empty();
        }

        int fileIndex = questionIds.indexOf(questionId);
        if (fileIndex == -1 || fileIndex >= files.size()) {
            return Optional.empty();
        }

        MultipartFile file = files.get(fileIndex);

        return (file == null || file.isEmpty())
                ? Optional.empty()
                : Optional.of(file);
    }

    /** 전달된 파일 개수 */
    public int fileCount() {
        return isEmpty(files) ? 0 : files.size();
    }

    private static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
