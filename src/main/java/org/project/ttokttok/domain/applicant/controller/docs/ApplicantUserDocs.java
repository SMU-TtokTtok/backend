package org.project.ttokttok.domain.applicant.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.applicant.controller.dto.request.ApplyFormRequest;
import org.project.ttokttok.domain.club.controller.dto.response.ClubListResponse;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "[사용자] 지원자 API", description = "사용자가 동아리에 지원하는 API")
public interface ApplicantUserDocs {

    @Operation(
            summary = "동아리 지원서 작성",
            description = """
                    사용자가 동아리 지원서를 작성합니다.
                    
                    **요청 형식** : multipart/form-data
                    
                    **요청 파라미터**:
                    - `clubId`: 지원할 동아리의 ID(PATH 경로)
                    - `request`: 지원서 데이터(JSON 형식)
                    - `questionIds`: 파일 응답 형식의 질문 ID 리스트 (JSON 형식)
                    - `files`: 파일 응답 형식의 질문에 대한 파일 리스트 (multipart/form-data 형식)
                    
                    **주의 사항**:
                    - `request`는 필수라서 꼭 요청에 포함되어야 합니다.
                    - `questionIds`와 `files`는 선택 사항입니다. 다만, 어느 한쪽만 제공할 수는 없습니다.
                    - `questionIds`와 `files`는 반드시 쌍으로, 같은 길이로 제공되어야 합니다.(질문 id와 파일 순서 중요) 
                    즉, `questionIds`가 비어있다면 `files`도 비어 있어야 하고, 
                    반대로 `files`가 비어있다면 `questionIds`도 비어 있어야 합니다.
                    
                    **열거형**
                    - grades:
                        - FIRST_GRADE (1학년)
                        - SECOND_GRADE (2학년)
                        - THIRD_GRADE (3학년)
                        - FOURTH_GRADE (4학년)
                        
                    -studentStatus:
                        - ENROLLED (재학 중)
                        - ABSENCE (휴학 중)
                        
                    - gender:
                        - MALE (남성)
                        - FEMALE (여성)
                    """
    )
    @RequestBody(
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApplyFormRequest.class)
                    ),
                    @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schemaProperties = {
                                    @SchemaProperty(name = "request", schema = @Schema(type = "string", format = "json", description = "지원서 데이터")),
                                    @SchemaProperty(name = "profileImage", schema = @Schema(type = "string", format = "json", description = "응답 형식이 파일인 질문 id")),
                                    @SchemaProperty(name = "profileImage", schema = @Schema(type = "form", format = "multipart/formData", description = "파일 리스트"))
                            }
                    )
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "지원서 제출 성공",
                            content = @Content(schema = @Schema(implementation = Map.class, example = "{\"message\": \"지원서 작성 완료, id: {UUID}\"}"))
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
                            responseCode = "404",
                            description = "동아리가 존재하지 않거나, 지원서 작성이 불가능한 상태.",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            })
    ResponseEntity<Map<String, String>> apply(
            @Parameter(hidden = true)
            String email,

            @Parameter(
                    description = "지원할 동아리의 ID",
                    required = true,
                    example = "UUID"
            )
            String clubId,

            @Parameter(
                    description = "지원서 응답 데이터",
                    required = true
            )
            ApplyFormRequest request,

            @Parameter(
                    description = "파일 응답 형식의 질문 ID 리스트",
                    example = "[\"q4\"]"
            )
            List<String> questionIds,

            @Parameter(
                    description = "파일 응답 형식의 질문에 대한 파일 리스트"
            )
            List<MultipartFile> files
    );

    @Operation(
            summary = "사용자 동아리 지원내역 조회",
            description = """
                    사용자가 지원한 동아리 목록을 조회합니다.
                    
                    **기능**:
                    - 무한스크롤 지원 (cursor 기반 페이징)
                    - 정렬 기능 (최신순, 인기도순, 멤버많은순)
                    - 카드 리스트 형태 응답
                    
                    **정렬 옵션**:
                    - `latest`: 최신 지원순 (기본값)
                    - `popular`: 인기도순 (멤버수 기준)
                    - `member_count`: 멤버많은순
                    
                    **무한스크롤**:
                    - 첫 요청: cursor 없이 요청
                    - 다음 요청: 응답의 nextCursor 값을 사용
                    """
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "지원내역 조회 성공",
                            content = @Content(schema = @Schema(implementation = ClubListResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "사용자를 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            })
    ResponseEntity<ClubListResponse> getUserApplicationHistory(
            @Parameter(hidden = true)
            String email,

            @Parameter(
                    description = "조회할 개수",
                    example = "20"
            )
            int size,

            @Parameter(
                    description = "커서 (무한스크롤용, 첫 요청시에는 생략)"
            )
            String cursor,

            @Parameter(
                    description = "정렬 방식",
                    example = "latest",
                    schema = @Schema(allowableValues = {"latest", "popular", "member_count"})
            )
            String sort
    );
}
