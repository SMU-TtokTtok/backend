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
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

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
                            content = @Content(schema = @Schema(implementation = String.class, example = "Club content updated successfully."))
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
                    )
            })
    ResponseEntity<String> updateClubContent(
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
                    description = "동아리 프로필 이미지 파일 (JPG, PNG, WEBP 지원, 최대 5MB)"
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
                    - 최대 파일 크기: 5MB
                    - 지원 형식: JPG, PNG, WEBP
                    - 해당 동아리의 관리자만 업로드 가능합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "이미지 업로드 및 업데이트 성공",
                    content = @Content(schema = @Schema(implementation = String.class, example = "Markdown image Updated successfully. Image Key: IMAGE_KEY"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 파일 형식 또는 크기 초과",
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
                    description = "서버 내부 오류 (S3 업로드 실패 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<String> updateMarkdownImage(
            @Parameter(description = "인증된 관리자 이름", hidden = true)
            String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            String clubId,
            @Parameter(description = "업로드할 이미지 파일 (JPG, PNG, GIF, WEBP 지원, 최대 5MB)")
            MultipartFile imageFile
    );

    @Operation(
            summary = "이미지 URL 조회",
            description = """
                    이미지 키를 사용하여 실제 이미지 URL을 조회합니다.
                    S3에 저장된 이미지의 Pre-signed URL을 반환합니다.
                    
                    *주의사항*
                    - 반환된 URL은 일정 시간 후 만료됩니다.
                    - 존재하지 않는 이미지 키의 경우 404 오류가 발생합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "이미지 URL 조회 성공",
                    content = @Content(schema = @Schema(implementation = String.class, example = "https://s3.amazonaws.com/bucket/club-1-image-12345.jpg?X-Amz-Expires=3600"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 이미지 키 형식",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 이미지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류 (S3 접근 실패 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<String> getImageUrl(
            @Parameter(description = "조회할 이미지의 키", required = true, example = "club-1-image-12345")
            @RequestParam String imageKey
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
                    동아리 모집 상태를 토글합니다.
                    현재 모집 중이면 마감으로, 마감 상태이면 모집 재시작으로 변경됩니다.
                    
                    **동작 방식**:
                    - 활성화된 지원 폼이 있는 경우: 해당 폼의 상태를 토글
                    - 활성화된 지원 폼이 없는 경우: 가장 최근 지원 폼을 활성화
                    - 지원 폼이 전혀 없는 경우: 예외 발생
                    
                    *주의사항*:
                    - 해당 동아리의 관리자만 실행 가능합니다.
                    - 지원 폼이 존재하지 않으면 오류가 발생합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
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
    ResponseEntity<Void> toggleRecruitment(
            @Parameter(description = "인증된 사용자 정보", hidden = true)
            String username,
            @Parameter(description = "동아리 ID", required = true, example = "UUID")
            String clubId
    );
}
