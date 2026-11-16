package org.project.ttokttok.domain.applicant.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.applicant.controller.dto.request.ApplicantStatusUpdateRequest;
import org.project.ttokttok.domain.applicant.controller.dto.request.SendResultMailRequest;
import org.project.ttokttok.domain.applicant.controller.dto.response.ApplicantDetailResponse;
import org.project.ttokttok.domain.applicant.controller.dto.response.ApplicantFinalizeResponse;
import org.project.ttokttok.domain.applicant.controller.dto.response.ApplicantPageResponse;
import org.project.ttokttok.domain.applicant.controller.dto.response.ResultAnnouncementResponse;
import org.project.ttokttok.domain.applicant.controller.enums.Kind;
import org.project.ttokttok.domain.applicant.domain.enums.Sort;
import org.project.ttokttok.domain.applicant.domain.enums.PhaseStatus;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Tag(name = "[관리자] 지원자 관리 API", description = "관리자가 지원자를 조회, 평가, 상태 관리하는 API입니다.")
public interface ApplicantAdminDocs {

    @Operation(
            summary = "지원자 목록 조회",
            description = """
                    지원자 목록을 페이지네이션으로 조회합니다.
                    정렬 기준과 평가 여부에 따라 필터링할 수 있습니다.
                    
                    *정렬 기준*
                    - GRADE: 성적순 (기본값)
                    - SUBMIT: 제출순, 오름차순
                    
                    *주의사항*
                    - 페이지 번호는 1부터 시작합니다.
                    - 페이지 크기는 기본값 7개입니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "지원자 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApplicantPageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (페이지 정보 오류)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "지원폼을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ApplicantPageResponse> getApplicantsPage(
            @Parameter(hidden = true) String username,
            @Parameter(description = "정렬 기준 (GRADE 등)", example = "GRADE") Sort sort,
            @Parameter(description = "평가 중인 지원자만 조회 여부", example = "false") boolean isEvaluating,
            @Parameter(description = "페이지 커서 (1부터 시작)", example = "1") int cursor,
            @Parameter(description = "페이지 크기", example = "7") int size,
            @Parameter(description = "서류 / 면접 구분", schema = @Schema(implementation = Kind.class), example = "DOCUMENT / INTERVIEW") Kind kind
    );

    @Operation(
            summary = "지원자 상세 정보 조회",
            description = """
                    특정 지원자의 상세 정보를 조회합니다.
                    지원서 내용, 평가 상태 등을 확인할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "지원자 상세 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApplicantDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (지원자 ID 형식 오류)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "지원자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ApplicantDetailResponse> getApplicantDetail(
            @Parameter(hidden = true) String username,
            @Parameter(description = "지원자 ID", example = "UUID") String applicantId
    );

    @Operation(
            summary = "지원자 검색",
            description = """
                    이름으로 지원자를 검색합니다.
                    검색 결과도 페이지네이션과 정렬이 적용됩니다.
                    
                    *주의사항*
                    - 키워드는 필수 입력값입니다.
                    - 페이지 번호는 1부터 시작합니다.
                    - 페이지 크기는 기본값 7개입니다.
                    
                    *정렬 기준*
                    - GRADE: 성적순 (기본값)
                    - SUBMIT: 제출순, 오름차순
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "지원자 검색 성공",
                    content = @Content(schema = @Schema(implementation = ApplicantPageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (검색 키워드 누락 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (관리자 권한 필요)"
            )
    })
    ResponseEntity<ApplicantPageResponse> applicantPageSearch(
            @Parameter(hidden = true) String username,
            @Parameter(description = "검색할 지원자 이름", example = "김철수") String keyword,
            @Parameter(description = "정렬 기준", example = "GRADE") Sort sort,
            @Parameter(description = "평가 중인 지원자만 조회 여부", example = "false") boolean isEvaluating,
            @Parameter(description = "페이지 커서", example = "1") int cursor,
            @Parameter(description = "페이지 크기", example = "7") int size,
            @Parameter(description = "서류 / 면접 구분", schema = @Schema(implementation = Kind.class), example = "DOCUMENT / INTERVIEW") Kind kind
    );

    @Operation(
            summary = "합격 지원자 목록 조회",
            description = """
                    합격한 지원자들의 목록을 조회합니다.
                    최종 확정된 합격자들을 확인할 수 있습니다.
                    
                    *주의사항*
                    - 페이지 번호는 1부터 시작합니다.
                    - 페이지 크기는 기본값 4개입니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "합격 지원자 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApplicantPageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "지원폼을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ApplicantPageResponse> getPassedApplicantsPage(
            @Parameter(hidden = true) String username,
            @Parameter(description = "페이지 번호", example = "1") int page,
            @Parameter(description = "페이지 크기", example = "4") int size,
            @Parameter(description = "서류 / 면접 구분", schema = @Schema(implementation = Kind.class), example = "DOCUMENT / INTERVIEW") Kind kind
    );

    @Operation(
            summary = "불합격 지원자 목록 조회",
            description = """
                    불합격한 지원자들의 목록을 조회합니다.
                    최종 확정된 불합격자들을 확인할 수 있습니다.
                    
                    *주의사항*
                    - 페이지 번호는 1부터 시작합니다.
                    - 페이지 크기는 기본값 4개입니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "불합격 지원자 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApplicantPageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "지원폼을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ApplicantPageResponse> getFailedApplicantsPage(
            @Parameter(hidden = true) String username,
            @Parameter(description = "페이지 번호", example = "1") int page,
            @Parameter(description = "페이지 크기", example = "4") int size,
            @Parameter(description = "서류 / 면접 구분", example = "DOCUMENT / INTERVIEW") Kind kind
    );

    @Operation(
            summary = "지원자 평가 상태 업데이트",
            description = """
                    특정 지원자의 평가 상태를 업데이트합니다.
                    합격/불합격/평가 중 등의 상태를 변경할 수 있습니다.
                    
                    *상태 종류*
                    - PASS: 합격
                    - FAIL: 불합격
                    - EVALUATING: 평가 중
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "평가 상태 업데이트 성공",
                    content = @Content(schema = @Schema(implementation = Map.class, example = "{\"message\", \"지원자 상태가 성공적으로 업데이트되었습니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (상태값 오류)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "지원자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Map<String, String>> updateApplicantEvaluation(
            @Parameter(hidden = true) String username,
            @Parameter(description = "지원자 ID", example = "UUID") String applicantId,
            @Parameter(description = "변경할 상태", schema = @Schema(implementation = ApplicantStatusUpdateRequest.class), example = "PASS / FAIL / EVALUATING") ApplicantStatusUpdateRequest request,
            @Parameter(description = "서류 / 면접 구분", schema = @Schema(implementation = Kind.class), example = "DOCUMENT / INTERVIEW") Kind kind
    );

    @Operation(
            summary = "지원자 상태 최종 확정",
            description = """
                    해당 동아리의 모든 지원자 상태를 최종 확정합니다.
                    확정 후에는 상태 변경이 제한될 수 있습니다.
                    
                    *주의사항*
                    - 한번 확정하면 되돌리기 어려우므로 신중하게 진행하세요.
                    - 모든 지원자의 평가가 완료된 후 실행하는 것을 권장합니다.
                    - 평가 중인 지원자는 제외하고 실행됩니다.
                    - 결과 메일 없이 확정만 할 때 씁니다. 메일과 함께 발표하려면 `POST /{clubId}/result-announcements`를 쓰세요.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "지원자 상태 최종 확정 성공",
                    content = @Content(schema = @Schema(implementation = ApplicantFinalizeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (동아리 ID 오류)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (해당 동아리 관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "동아리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ApplicantFinalizeResponse> finalizeApplicantsStatus(
            @Parameter(hidden = true) String username,
            @Parameter(description = "동아리 ID", example = "UUID") String clubId,
            @Parameter(description = "서류 / 면접 구분", schema = @Schema(implementation = Kind.class), example = "DOCUMENT / INTERVIEW") Kind kind
    );

    @Operation(
            summary = "전형 결과 발표 (확정 + 결과 메일 접수)",
            description = """
                    전형 결과를 확정하고 지원자별 결과 메일 발송 작업을 한 번에 접수합니다.
                    확정과 접수는 함께 성공하거나 함께 실패합니다. 메일은 응답 뒤에 서버가 차례로 보냅니다.

                    *Idempotency-Key*
                    - 필수 헤더입니다. 발표 화면을 열 때 한 번 만들고, 재시도에는 같은 값을 다시 보내 주세요.
                    - 같은 키로 다시 보내면 처음 응답을 그대로 돌려주고 메일을 다시 접수하지 않습니다.
                    - 같은 키에 다른 전형이나 메일 내용을 보내면 422입니다.
                    - 이미 발표한 전형을 새 키로 다시 발표하면 409입니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "202",
                    description = "확정과 메일 접수 완료. 같은 키 재요청도 202와 처음 응답"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (Idempotency-Key 누락·형식 오류, 메일 내용 오류)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (해당 동아리 관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "동아리 또는 활성 지원 폼을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 결과를 발표한 전형",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "이미 다른 내용의 발표에 쓴 Idempotency-Key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ResultAnnouncementResponse> announceResults(
            @Parameter(hidden = true) String username,
            @Parameter(description = "동아리 ID", example = "UUID") String clubId,
            @Parameter(description = "재요청을 같은 발표로 묶는 키 (1~100자)", required = true, example = "UUID") String idempotencyKey,
            @Parameter(description = "이메일 발송 요청 정보") SendResultMailRequest request,
            @Parameter(description = "서류 / 면접 구분", schema = @Schema(implementation = Kind.class), example = "DOCUMENT / INTERVIEW") Kind kind
    );

    @Operation(
            summary = "지원자에게 결과 이메일 발송 (지원 중단 예정)",
            deprecated = true,
            description = """
                    해당 동아리의 지원자들에게 보낼 결과 이메일 발송 작업을 접수합니다.
                    합격/불합격 상태에 따라 다른 내용의 이메일이 발송됩니다.
                    메일은 응답 뒤에 서버가 차례로 보내며, 일시 오류는 약 24시간 동안 다시 시도합니다.
                    
                    *주의사항*
                    - 이메일은 한번 접수하면 취소할 수 없습니다.
                    - 멱등하지 않습니다. 다시 호출하면 같은 메일이 한 번 더 접수됩니다.
                    - 확정과 발송을 함께 하려면 `POST /{clubId}/result-announcements`를 쓰세요. 프론트가 옮기면 이 API는 삭제합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "이메일 발송 작업 접수 완료 (발송 완료가 아님)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (이메일 내용 오류)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (해당 동아리 관리자 권한 필요)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "동아리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Map<String, String>> sendEmailToApplicants(
            @Parameter(hidden = true) String username,
            @Parameter(description = "동아리 ID", example = "UUID") String clubId,
            @Parameter(description = "이메일 발송 요청 정보") SendResultMailRequest request,
            @Parameter(description = "서류 / 면접 구분", schema = @Schema(implementation = Kind.class), example = "DOCUMENT / INTERVIEW") Kind kind
    );
}
