package org.project.ttokttok.domain.notification.fcm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.notification.fcm.controller.docs.FCMTokenDocs;
import org.project.ttokttok.domain.notification.fcm.controller.dto.request.FCMTokenDeleteRequest;
import org.project.ttokttok.domain.notification.fcm.controller.dto.request.FCMTokenSaveRequest;
import org.project.ttokttok.domain.notification.fcm.controller.dto.response.FCMTokenResponse;
import org.project.ttokttok.domain.notification.fcm.service.FCMTokenService;
import org.project.ttokttok.domain.notification.fcm.service.dto.FCMTokenDeleteServiceRequest;
import org.project.ttokttok.domain.notification.fcm.service.dto.FCMTokenSaveServiceRequest;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/fcm/token")
public class FCMTokenApiController implements FCMTokenDocs {

    private final FCMTokenService fcmTokenService;

    @PostMapping
    public ResponseEntity<FCMTokenResponse> saveToken(@AuthUserInfo String email,
                                                      @Valid @RequestBody FCMTokenSaveRequest request) {
        FCMTokenSaveServiceRequest serviceRequest = FCMTokenSaveServiceRequest.builder()
                .email(email)
                .token(request.token())
                .deviceType(request.deviceType())
                .build();

        fcmTokenService.saveOrUpdate(serviceRequest);

        return ResponseEntity.ok()
                .body(
                        new FCMTokenResponse("FCM 토큰 저장 완료")
                );
    }

    @DeleteMapping
    public ResponseEntity<FCMTokenResponse> deleteToken(@AuthUserInfo String email,
                                                        @Valid @RequestBody FCMTokenDeleteRequest request) {
        FCMTokenDeleteServiceRequest serviceRequest = FCMTokenDeleteServiceRequest.builder()
                .token(request.token())
                .email(email)
                .build();

        fcmTokenService.delete(serviceRequest);

        return ResponseEntity.ok()
                .body(
                        new FCMTokenResponse("FCM 토큰 삭제 완료")
                );
    }
}
