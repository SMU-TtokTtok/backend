package org.project.ttokttok.domain.temp.applicant.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempApplicantSaveRequest;
import org.project.ttokttok.domain.temp.applicant.controller.dto.response.TempApplicantDataResponse;
import org.project.ttokttok.domain.temp.applicant.controller.dto.response.TempApplicantSaveResponse;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "[사용자] 임시 지원서 API", description = "지원서 임시 저장 관리용 API 입니다.")
public interface TempApplicantDocs {


    @Operation(
            summary = "임시 지원서 저장",
            description = """
                    지원서 작성 중 임시 저장을 수행합니다.
                    사용자가 지원서를 작성하는 과정에서 임시로 데이터를 저장할 때 사용됩니다.
                    
                    *주의사항*
                    - 사용자 인증이 필요합니다.
                    - 지원폼 ID는 필수 입력 사항입니다.
                    - 임시 데이터(tempData)는 지원서 작성 중인 내용을 JSON 형태로 저장합니다.
                    - 임시 저장된 데이터는 나중에 정식 지원서로 제출할 수 있습니다.
                    - 임시 데이터 필드들은 작성되지 않은 필드는 null을 허용합니다.
                    """
    )
    @RequestBody(
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TempApplicantSaveRequest.class)
                    ),
                    @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schemaProperties = {
                                    @SchemaProperty(name = "request", schema = @Schema(type = "string", format = "json", description = "지원서 데이터")),
                                    @SchemaProperty(name = "questionIds", schema = @Schema(type = "string", format = "json", description = "응답 형식이 파일인 질문 id")),
                                    @SchemaProperty(name = "files", schema = @Schema(type = "form", format = "multipart/formData", description = "파일 리스트"))
                            }
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "임시 지원서 저장 성공",
                    content = @Content(schema = @Schema(implementation = TempApplicantSaveResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효하지 않은 요청 데이터)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "파일크기 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<TempApplicantSaveResponse> saveTempApplicant(
            @Parameter(
                    description = "인증된 사용자 이메일", hidden = true
            )
            String email,

            @Parameter(
                    description = "임시 저장한 지원폼의 아이디"
            )
            String formId,

            @Parameter(
                    description = "임시 지원서 저장 요청 데이터",
                    content = @Content(schema = @Schema(implementation = TempApplicantSaveRequest.class))
            )
            TempApplicantSaveRequest request,

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
            summary = "임시 지원서 조회",
            description = """
                    저장된 임시 지원서를 조회합니다.
                    사용자가 이전에 임시 저장한 지원서 데이터를 불러올 때 사용됩니다.
                    
                    *주의사항*
                    - 사용자 인증이 필요합니다.
                    - 지원폼 ID를 기준으로 임시 저장된 데이터를 조회합니다.
                    - 임시 저장된 데이터가 없는 경우 hasTempData가 false로 반환되며, data는 빈 객체입니다.
                    - 임시 저장된 데이터가 있는 경우 hasTempData가 true로 반환되며, data에 저장된 내용이 포함됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "임시 지원서 조회 성공",
                    content = @Content(schema = @Schema(implementation = TempApplicantDataResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효하지 않은 요청 데이터)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "지원폼을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "지원폼을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<TempApplicantDataResponse> getTempApplicant(
            @Parameter(description = "인증된 사용자 이메일", hidden = true)
            String userEmail,
            @Parameter(description = "임시 저장된 지원폼의 아이디", required = true, example = "form123")
            String formId
    );
}
