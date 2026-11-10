package org.project.ttokttok.domain.club.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.club.controller.dto.request.UpdateClubContentRequest;
import org.project.ttokttok.domain.club.controller.dto.response.ClubAdminDetailResponse;
import org.project.ttokttok.domain.club.controller.dto.response.UpdateImageResponse;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "[관리자] 동아리 관련 API", description = "관리자 동아리 정보 관리용 API 입니다.")
public interface ClubAdminApiDocs {

    @Operation(
            summary = "동아리 소개 수정",
            description = """
                    동아리 관리자가 동아리 소개 내용을 수정합니다.
                    
                    **요청 형식**: Multipart Form Data
                    
                    **요청 파라미터**:
                    - `request`: JSON 형태의 동아리 정보 (UpdateClubContentRequest, 필드 내 선택사항)
                    - `profileImage`: 동아리 프로필 이미지 파일 (선택사항)
                    
                    *주의사항*:
                    - 해당 동아리의 관리자만 수정 가능합니다.
                    - 마크다운 형식을 지원합니다.
                    - 이미지는 JPG, PNG, WEBP 형식만 지원됩니다.
                    - 수정 사항에 포함되지 않는 필드는 NULL이 아닌, 포함하지 말아주세요.
                    - 모집 정보(기간, 학년, 인원)는 활성 폼에 반영되고, 바꾸지 않은 값과 합친 기간으로 검증합니다.
                      모집 기간은 최대 3개월(400 `RECRUIT_PERIOD_TOO_LONG`)이고, 지원서가 제출된 폼은 시작일을 바꿀 수 없습니다(409 `APPLY_START_DATE_LOCKED`).
                    
                    **열거형**:
                    - clubType:
                        - CENTRAL (중앙)
                        - UNION (연합)
                        - DEPARTMENT (학과)
                    
                    - clubCategory:
                        - VOLUNTEER ("봉사")
                        - CULTURE ("문화")
                        - ACADEMIC ("학술")
                        - SPORTS ("체육")
                        - RELIGION ("종교")
                        - ARTS ("예술")
                        - SOCIAL ("친목")
                        - ETC ("기타")

                    - clubUniv:
                        - GLOBAL_AREA (글로벌지역 학부)
                        - DESIGN (디자인대학)
                        - ENGINEERING (공대)
                        - CONVERGENCE_TECHNOLOGY (융합기술대)
                        - ARTS` (예술대)

                    - grades:
                        - FIRST_GRADE (1학년)
                        - SECOND_GRADE (2학년)
                        - THIRD_GRADE (3학년)
                        - FOURTH_GRADE (4학년)
                    """
    )
    @RequestBody(
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateClubContentRequest.class)
                    ),
                    @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schemaProperties = {
                                    @SchemaProperty(name = "request", schema = @Schema(type = "string", format = "json", description = "동아리 정보 수정 데이터")),
                                    @SchemaProperty(name = "profileImage", schema = @Schema(type = "string", format = "binary", description = "동아리 프로필 이미지 파일"))
                            }
                    )
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "동아리 소개 수정 성공",
                            content = @Content(schema = @Schema(implementation = Map.class, example = "{\"message\": \"동아리 소개가 수정되었습니다.\"}"))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (필수 필드 누락 또는 형식 오류)",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "권한 없음 (동아리 관리자가 아님)",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "존재하지 않는 동아리",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "415",
                            description = "지원하지 않는 파일 형식",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            })
    ResponseEntity<Map<String, String>> updateClubContent(
            @Parameter(
                    description = "인증된 사용자 정보",
                    hidden = true
            ) String username,

            @Parameter(
                    description = "수정할 동아리 ID",
                    example = "club-001",
                    required = true
            ) String clubId,

            @Schema(
                    type = "string",
                    format = "json",
                    description = "동아리 정보 수정 데이터")
            UpdateClubContentRequest request,

            @Schema(
                    type = "string",
                    format = "binary",
                    description = "동아리 프로필 이미지 파일 (JPG, PNG, WEBP 지원, 최대 20MB)"
            )
            MultipartFile profileImage
    );

    @Operation(
            summary = "마크다운 이미지 업데이트",
            description = """
                    동아리 소개에 사용할 마크다운 이미지를 업로드하고 업데이트합니다.
                    업로드된 이미지는 S3에 저장되며, 고유한 이미지 키가 반환됩니다.
                    
                    *주의사항*
                    - 이미지 파일만 업로드 가능합니다.
                    - 최대 파일 크기: 20MB
                    - 지원 형식: JPG, PNG, WEBP, GIF, HEIC
                    - 해당 동아리의 관리자만 업로드 가능합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "이미지 업로드 및 업데이트 성공",
                    content = @Content(schema = @Schema(implementation = UpdateImageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 파일 형식",
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
                    responseCode = "415",
                    description = "지원하지 않는 파일 형식",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류 (S3 업로드 실패 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<UpdateImageResponse> updateMarkdownImage(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            String clubId,
            @Parameter(description = "업로드할 이미지 파일 (JPG, PNG, GIF, WEBP, HEIC 지원, 최대 20MB)")
            MultipartFile imageFile
    );

    @Operation(
            summary = "관리자 동아리 소개 조회",
            description = """
                    동아리 관리자가 동아리의 상세 정보를 조회합니다.
                    
                    **조회 가능한 정보**:
                    - 동아리 기본 정보 (이름, 타입, 카테고리, 대학 구분 등)
                    - 모집 관련 정보
                    - 동아리 소개 내용 (마크다운 형식)
                    - 현재 동아리원 수
                    - 프로필 이미지 URL
                    
                    **모집 정보**:
                    - 활성 폼의 기간, 학년, 인원을 모집 스위치가 꺼져 있어도 내려줍니다. 활성 폼이 없으면 null, [], 0 입니다.
                    - `recruitState`: UPCOMING(시작일 전), OPEN(모집 중), CLOSED(마감, 스위치 꺼짐, 또는 활성 폼 없음)
                    - `recruiting`: recruitState 가 OPEN 일 때만 true

                    *주의사항*:
                    - 해당 동아리의 관리자만 조회 가능합니다.
                    - 존재하지 않는 동아리 ID의 경우 404 오류가 발생합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "동아리 상세 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = ClubAdminDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 부족",
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
    ResponseEntity<ClubAdminDetailResponse> getClubContent(
            @Parameter(
                    description = "조회할 동아리 ID",
                    example = "UUID",
                    required = true
            ) String clubId
    );

    @Operation(
            summary = "모집 마감/재시작 토글",
            description = """
                    동아리 모집 스위치를 토글합니다.

                    **동작 방식**:
                    - 활성 폼의 스위치가 켜져 있으면(OPEN, UPCOMING, 마감일이 지나 새벽 4시 정리 전인 CLOSED): 모집 마감
                    - 활성 폼의 스위치가 꺼져 있으면: 스위치를 켭니다. 마감일이 지나지 않았을 때만 가능합니다.
                      오늘이 모집 기간 안이면 OPEN 이 되고 지원 가능 사용자에게 알림을 보냅니다.
                      시작일 전이면 UPCOMING 으로 돌아가고 알림은 보내지 않습니다. 시작일이 되면 저절로 OPEN 이 됩니다.
                    - 활성 폼이 없으면: 가장 최근 지원 폼을 스위치가 꺼진 상태로 되살립니다. 알림은 보내지 않습니다.

                    *주의사항*:
                    - 해당 동아리의 관리자만 실행 가능합니다.
                    - 지원 폼이 존재하지 않으면 404 오류가 발생합니다.
                    - 마감일이 지났으면 409 `RECRUIT_PERIOD_NOT_OPEN` 입니다. 기간을 먼저 수정해야 합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "모집 상태 토글 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (동아리 관리자가 아님)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 동아리 또는 지원 폼",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Map<String, String>> toggleRecruitment(
            @Parameter(description = "인증된 사용자 정보", hidden = true)
            String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            String clubId
    );
}
