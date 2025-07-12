package org.project.ttokttok.domain.club.controller;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.club.controller.dto.response.ClubDetailResponse;
import org.project.ttokttok.domain.club.service.ClubUserService;
import org.project.ttokttok.global.annotation.auth.AuthUserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs")
public class ClubUserApiController {

    private final ClubUserService clubService;

    @GetMapping("/{clubId}/content")
    public ResponseEntity<ClubDetailResponse> getClubIntroduction(@AuthUserInfo String username,
                                                                  @PathVariable String clubId) {
        ClubDetailResponse response = ClubDetailResponse.from(
                clubService.getClubIntroduction(username, clubId)
        );

        return ResponseEntity.ok()
                .body(response);
    }
}
