package org.project.ttokttok.domain.clubMember.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.clubMember.controller.dto.request.RoleChangeRequest;
import org.project.ttokttok.domain.clubMember.controller.dto.response.ClubMemberPageResponse;
import org.project.ttokttok.domain.clubMember.service.ClubMemberService;
import org.project.ttokttok.domain.clubMember.service.dto.request.ChangeRoleServiceRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.ClubMemberPageRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.DeleteMemberServiceRequest;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class ClubMemberApiController {

    private final ClubMemberService clubMemberService;

    @GetMapping("/{clubId}")
    public ResponseEntity<ClubMemberPageResponse> getClubMembers(@AuthUserInfo String username,
                                                                 @PathVariable String clubId,
                                                                 @RequestParam(defaultValue = "1", required = false) int page,
                                                                 @RequestParam(defaultValue = "5", required = false) int size) {

        ClubMemberPageRequest pageRequest = new ClubMemberPageRequest(username, page, size);

        ClubMemberPageResponse response = ClubMemberPageResponse.from(
                clubMemberService.getClubMembers(clubId, pageRequest)
        );

        return ResponseEntity.ok()
                .body(response);
    }

    @PatchMapping("/{clubId}/{memberId}/role")
    public ResponseEntity<Void> changeRole(@AuthUserInfo String username,
                                           @PathVariable String clubId,
                                           @PathVariable String memberId,
                                           @Valid @RequestBody RoleChangeRequest request) {

        ChangeRoleServiceRequest serviceRequest = ChangeRoleServiceRequest.of(
                username,
                clubId,
                memberId,
                request.role()
        );

        clubMemberService.changeRole(serviceRequest);

        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping("/{clubId}/{memberId}")
    public ResponseEntity<Void> deleteMember(@AuthUserInfo String username,
                                             @PathVariable String clubId,
                                             @PathVariable String memberId) {

        DeleteMemberServiceRequest serviceRequest = DeleteMemberServiceRequest.of(
                username,
                clubId,
                memberId
        );

        clubMemberService.deleteMember(serviceRequest);

        return ResponseEntity.noContent()
                .build();
    }

    // 부원 액셀 파일 다운로드.
    @GetMapping("/{clubId}/export")
    public ResponseEntity<Resource> downloadMembersExcel(@PathVariable String clubId) {

    }
}
