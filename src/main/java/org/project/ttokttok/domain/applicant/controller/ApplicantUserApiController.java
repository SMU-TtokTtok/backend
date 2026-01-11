package org.project.ttokttok.domain.applicant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.applicant.controller.docs.ApplicantUserDocs;
import org.project.ttokttok.domain.applicant.controller.dto.request.ApplyFormRequest;
import org.project.ttokttok.domain.applicant.service.ApplicantUserService;
import org.project.ttokttok.domain.club.controller.dto.response.ClubListResponse;
import org.project.ttokttok.domain.club.service.dto.response.ClubListServiceResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/applies")
public class ApplicantUserApiController implements ApplicantUserDocs {

    private final ApplicantUserService applicantUserService;

    @PostMapping(
            value = "/{clubId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, String>> apply(@AuthUserInfo String email,
                                                     @PathVariable String clubId, // 지원폼 ID
                                                     @Valid @RequestPart ApplyFormRequest request, // json으로 명시 필요
                                                     @RequestPart(required = false) List<String> questionIds,// json으로 명시 필요
                                                     @RequestPart(required = false) List<MultipartFile> files) {

        String ApplicantId = applicantUserService.apply(email, request, questionIds, files, clubId);

        return ResponseEntity.ok()
                .body(Map.of("message", "지원서 작성 완료, id: " + ApplicantId));
    }

    /**
     * 사용자의 동아리 지원내역 조회
     * 무한스크롤과 정렬 기능을 지원합니다.
     *
     * @param email 인증된 사용자 이메일
     * @param size 조회할 개수 (기본값: 20)
     * @param cursor 커서 (무한스크롤용, 선택사항)
     * @param sort 정렬 방식 (latest: 최신순, popular: 인기도순, member_count: 멤버많은순, 기본값: latest)
     * @return 사용자 지원내역 목록과 페이징 정보 (마감 임박 여부 포함)
     */
    @GetMapping("/history")
    public ResponseEntity<ClubListResponse> getUserApplicationHistory(
            @AuthUserInfo String email,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "latest") String sort) {

        ClubListServiceResponse serviceResponse =
                applicantUserService.getUserApplicationHistory(email, size, cursor, sort);

        ClubListResponse response = ClubListResponse.from(serviceResponse);

        return ResponseEntity.ok(response);
    }
}
