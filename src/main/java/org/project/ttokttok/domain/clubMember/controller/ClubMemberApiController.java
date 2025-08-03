package org.project.ttokttok.domain.clubMember.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.clubMember.controller.docs.ClubMemberDocs;
import org.project.ttokttok.domain.clubMember.controller.dto.request.ClubMemberAddRequest;
import org.project.ttokttok.domain.clubMember.controller.dto.request.RoleChangeRequest;
import org.project.ttokttok.domain.clubMember.controller.dto.response.ClubMemberCountResponse;
import org.project.ttokttok.domain.clubMember.controller.dto.response.ClubMemberCreateResponse;
import org.project.ttokttok.domain.clubMember.controller.dto.response.ClubMemberPageResponse;
import org.project.ttokttok.domain.clubMember.controller.dto.response.ClubMemberSearchCoverResponse;
import org.project.ttokttok.domain.clubMember.controller.enums.ClubRole;
import org.project.ttokttok.domain.clubMember.service.ClubMemberService;
import org.project.ttokttok.domain.clubMember.service.dto.request.ChangeRoleServiceRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.ClubMemberPageRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.ClubMemberSearchRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.DeleteMemberServiceRequest;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberCountServiceResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberSearchServiceResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ExcelServiceResponse;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class ClubMemberApiController implements ClubMemberDocs {

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

    @GetMapping("/{clubId}/total-count")
    public ResponseEntity<ClubMemberCountResponse> getClubMemberCount(@AuthUserInfo String username,
                                                                      @PathVariable String clubId) {

        ClubMemberCountResponse response = ClubMemberCountResponse.from(
                clubMemberService.getClubMembersCount(clubId, username)
        );

        return ResponseEntity.ok()
                .body(response);
    }

    @PatchMapping("/{clubId}/{memberId}/role")
    public ResponseEntity<Map<String, String>> changeRole(@AuthUserInfo String username,
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

        return ResponseEntity.ok()
                .body((Map.of("message", "부원 역할 변경 완료: " + memberId)));
    }

    @DeleteMapping("/{clubId}/{memberId}")
    public ResponseEntity<Map<String, String>> deleteMember(@AuthUserInfo String username,
                                                            @PathVariable String clubId,
                                                            @PathVariable String memberId) {

        DeleteMemberServiceRequest serviceRequest = DeleteMemberServiceRequest.of(
                username,
                clubId,
                memberId
        );

        clubMemberService.deleteMember(serviceRequest);

        return ResponseEntity.ok()
                .body(Map.of("message", "부원 삭제 완료: " + memberId));
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

    @GetMapping("/{clubId}/search")
    public ResponseEntity<ClubMemberSearchCoverResponse> searchMembers(@AuthUserInfo String username,
                                                                       @PathVariable String clubId,
                                                                       @RequestParam String keyword) {
        ClubMemberSearchRequest request = ClubMemberSearchRequest.of(
                username,
                clubId,
                keyword
        );

        ClubMemberSearchCoverResponse response = ClubMemberSearchCoverResponse.from(
                clubMemberService.clubMemberSearch(request)
        );

        return ResponseEntity.ok()
                .body(response);
    }

    @PostMapping("/{clubId}/add")
    public ResponseEntity<ClubMemberCreateResponse> addMembers(@AuthUserInfo String username,
                                                               @PathVariable String clubId,
                                                               @Valid @RequestBody ClubMemberAddRequest request,
                                                               @RequestParam ClubRole role) {
        String clubMemberId = clubMemberService.addMember(
                username, clubId, request.toServiceRequest(), role.name()
        );

        ClubMemberCreateResponse response = new ClubMemberCreateResponse(clubMemberId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }
}