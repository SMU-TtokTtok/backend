package org.project.ttokttok.domain.clubMember.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.clubMember.controller.dto.request.RoleChangeRequest;
import org.project.ttokttok.domain.clubMember.controller.dto.response.ClubMemberPageResponse;
import org.project.ttokttok.domain.clubMember.service.ClubMemberService;
import org.project.ttokttok.domain.clubMember.service.dto.request.ChangeRoleServiceRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.ClubMemberPageRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.DeleteMemberServiceRequest;
import org.project.ttokttok.domain.clubMember.service.dto.response.ExcelServiceResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
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

    @GetMapping("/{clubId}/download")
    public ResponseEntity<byte[]> downloadMembersExcel(@AuthUserInfo String username,
                                                       @PathVariable String clubId) {

        ExcelServiceResponse response = clubMemberService.downloadMembersAsExcel(clubId, username);

        String fileName = URLEncoder.encode(response.clubName() + "_부원_목록.xlsx", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(response.excelData());
    }
}
