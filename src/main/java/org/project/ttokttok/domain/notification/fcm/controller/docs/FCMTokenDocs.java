package org.project.ttokttok.domain.notification.fcm.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.project.ttokttok.domain.notification.fcm.controller.dto.request.FCMTokenDeleteRequest;
import org.project.ttokttok.domain.notification.fcm.controller.dto.request.FCMTokenSaveRequest;
import org.project.ttokttok.domain.notification.fcm.controller.dto.response.FCMTokenResponse;
import org.project.ttokttok.global.exception.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "[사용자] FCM 토큰 API", description = "Firebase Cloud Messaging 토큰 관리 API 입니다.")
public interface FCMTokenDocs {

    @Operation(
            summary = "FCM 토큰 저장",
            description = """
                    사용자의 FCM 토큰을 저장합니다.
                    푸시 알림을 받기 위해 클라이언트에서 생성된 FCM 토큰을 서버에 등록합니다.
                    같은 이메일과 기기 타입으로 이미 등록된 토큰이 있다면 새로운 토큰으로 업데이트됩니다.
                    
                    *주의사항*
                    - FCM 토큰은 필수 입력 값입니다.
                    - 기기 타입은 WEB, ANDROID, IOS, UNKNOWN 중 하나여야 합니다.
                    - 인증된 사용자만 접근할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "FCM 토큰 저장 성공",
                    content = @Content(schema = @Schema(implementation = FCMTokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (토큰 또는 기기 타입 누락/형식 오류)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (로그인이 필요함)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<FCMTokenResponse> saveToken(
            @Parameter(hidden = true, description = "인증된 사용자 이메일 (자동 주입)")
            String email,
            @Parameter(description = "FCM 토큰 저장 요청 (토큰, 기기 타입)")
            FCMTokenSaveRequest request
    );

    @Operation(
            summary = "FCM 토큰 삭제",
            description = """
                    사용자의 FCM 토큰을 삭제합니다.
                    로그아웃이나 앱 삭제 시 더 이상 푸시 알림을 받지 않도록 토큰을 제거합니다.
                    존재하지 않는 토큰을 삭제하려고 해도 성공 응답을 반환합니다.
                    
                    *주의사항*
                    - FCM 토큰은 필수 입력 값입니다.
                    - 인증된 사용자만 접근할 수 있습니다.
                    - 본인이 등록한 토큰만 삭제할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "FCM 토큰 삭제 성공",
                    content = @Content(schema = @Schema(implementation = FCMTokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (토큰 누락)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (로그인이 필요함)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 작동 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<FCMTokenResponse> deleteToken(
            @Parameter(hidden = true, description = "인증된 사용자 이메일 (자동 주입)")
            String email,
            @Parameter(description = "FCM 토큰 삭제 요청 (토큰)")
            FCMTokenDeleteRequest request
    );
}
