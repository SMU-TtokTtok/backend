package org.project.ttokttok.domain.clubMember.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.project.ttokttok.domain.clubMember.controller.dto.request.ClubMemberAddRequest;
import org.project.ttokttok.domain.clubMember.controller.dto.request.RoleChangeRequest;
import org.project.ttokttok.domain.clubMember.controller.dto.response.ClubMemberCountResponse;
import org.project.ttokttok.domain.clubMember.controller.dto.response.ClubMemberCreateResponse;
import org.project.ttokttok.domain.clubMember.controller.dto.response.ClubMemberPageResponse;
import org.project.ttokttok.domain.clubMember.controller.dto.response.ClubMemberSearchCoverResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "[관리자] 동아리 부원 관리 API", description = "동아리 관리자용 부원 관리 API 입니다.")
public interface ClubMemberDocs {

    @Operation(
            summary = "동아리 부원 목록 조회",
            description = """
                    동아리 부원 목록을 페이징 형태로 조회합니다.
                    관리자만 접근 가능하며, 멤버 정보와 역할을 포함합니다.
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 조회 가능합니다.
                    - 페이지는 1부터 시작합니다.
                    - 기본 페이지 크기는 5개입니다.
                    
                    Grade 열거형값은 아래와 같습니다.
                    - FIRST_GRADE: 1학년
                    - SECOND_GRADE: 2학년
                    - THIRD_GRADE: 3학년
                    - FOURTH_GRADE: 4학년
                    
                    role 열거형값은 아래와 같습니다.
                    - PRESIDENT: "회장"
                    - VICE_PRESIDENT: "부회장"
                    - EXECUTIVE: "임원진"
                    - MEMBER: "부원"
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "멤버 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ClubMemberPageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 동아리",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ClubMemberPageResponse> getClubMembers(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            @AuthUserInfo String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            @PathVariable String clubId,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1", required = false) int page,
            @Parameter(description = "페이지 크기", example = "5")
            @RequestParam(defaultValue = "5", required = false) int size
    );

    @Operation(
            summary = "동아리 부원 총 개수 조회",
            description = """
                    동아리에 속한 전체 멤버 수를 조회합니다.
                    관리자 권한이 필요합니다.
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 조회 가능합니다.
                    - 탈퇴한 부원은 포함되지 않습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "부원 개수 조회 성공",
                    content = @Content(schema = @Schema(implementation = ClubMemberCountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 동아리",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ClubMemberCountResponse> getClubMemberCount(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            @AuthUserInfo String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            @PathVariable String clubId
    );

    @Operation(
            summary = "멤버 삭제",
            description = """
                    동아리에서 멤버를 삭제(추방)합니다.
                    관리자 권한이 필요하며, 되돌릴 수 없습니다.
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 삭제 가능합니다.
                    - 자신을 삭제할 수 없습니다.
                    - 삭제된 멤버의 모든 활동 기록이 제거될 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "멤버 삭제 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아니거나 자신을 삭제하려는 경우",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 동아리 또는 멤버",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> deleteMember(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            @AuthUserInfo String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            @PathVariable String clubId,
            @Parameter(description = "삭제할 멤버 ID", required = true, example = "UUID")
            @PathVariable String memberId
    );

    @Operation(
            summary = "멤버 역할 변경",
            description = """
                    동아리 멤버의 역할을 변경합니다.
                    관리자 권한이 필요합니다.
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 변경 가능합니다.
                    - 회장, 부회장은 각 한 명만 존재할 수 있습니다.
                    
                    role 열거형값은 아래와 같습니다.
                    - PRESIDENT: "회장"
                    - VICE_PRESIDENT: "부회장"
                    - EXECUTIVE: "임원진"
                    - MEMBER: "부원"
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "역할 변경 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효하지 않은 역할)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아니거나 권한 부족",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 동아리 또는 멤버",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> changeRole(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            @AuthUserInfo String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            @PathVariable String clubId,
            @Parameter(description = "멤버 ID", required = true, example = "UUID")
            @PathVariable String memberId,
            @Parameter(description = "역할 변경 요청 데이터")
            @Valid @RequestBody RoleChangeRequest request
    );

    @Operation(
            summary = "부원 목록 엑셀 다운로드",
            description = """
                    동아리 부원 목록을 엑셀 파일 형태로 다운로드합니다.
                    관리자 권한이 필요하며, 모든 부원 정보가 포함됩니다.
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 다운로드 가능합니다.
                    - 개인정보가 포함되므로 신중하게 관리해야 합니다.
                    - 파일명: "{동아리명}_부원_목록.xlsx"
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "엑셀 파일 다운로드 성공",
                    content = @Content(
                            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 동아리",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류 (엑셀 생성 실패 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<byte[]> downloadMembersExcel(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            String clubId
    );

    @Operation(
            summary = "멤버 검색",
            description = """
                    키워드를 사용하여 동아리 멤버를 검색합니다.
                    이름으로 검색 가능합니다.
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 검색 가능합니다.
                    - 이름 기반 검색입니다.
                    
                    Grade 열거형값은 아래와 같습니다.
                    - FIRST_GRADE: 1학년
                    - SECOND_GRADE: 2학년
                    - THIRD_GRADE: 3학년
                    - FOURTH_GRADE: 4학년
                    
                    role 열거형값은 아래와 같습니다.
                    - PRESIDENT: "회장"
                    - VICE_PRESIDENT: "부회장"
                    - EXECUTIVE: "임원진"
                    - MEMBER: "부원"
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "멤버 검색 성공",
                    content = @Content(schema = @Schema(implementation = ClubMemberSearchCoverResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 검색 키워드 (너무 짧거나 형식 오류)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 동아리",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ClubMemberSearchCoverResponse> searchMembers(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            @AuthUserInfo String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            @PathVariable String clubId,
            @Parameter(description = "검색 키워드 (이름, 학번, 학과 등)", required = true, example = "엄준식")
            @RequestParam String keyword
    );

    @Operation(
            summary = "멤버 추가",
            description = """
                    동아리에 새로운 멤버를 추가합니다.
                    관리자 권한이 필요합니다.
                    
                    *주의사항*
                    - 해당 동아리의 관리자만 추가 가능합니다.
                    - 이미 가입된 멤버는 중복 추가할 수 없습니다.
                    - 회장 혹은 부회장 역할을 추가할 때 이미 존재하는 경우 409 오류가 발생합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "멤버 추가 성공",
                    content = @Content(schema = @Schema(implementation = String.class, example = "ClubMember Add Successfully. clubMemberId: UUID"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수 필드 누락 또는 중복 멤버)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 동아리의 관리자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 동아리 또는 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 존재하는 역할 혹은 이미 가입된 멤버",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ClubMemberCreateResponse> addMembers(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            String clubId,
            @Parameter(description = "멤버 추가 요청 데이터")
            ClubMemberAddRequest request
    );
}
