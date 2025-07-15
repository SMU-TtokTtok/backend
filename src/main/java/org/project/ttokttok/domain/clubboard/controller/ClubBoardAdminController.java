package org.project.ttokttok.domain.clubboard.controller;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.clubboard.service.ClubBoardAdminService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/clubs/{clubId}/boards")
public class ClubBoardAdminController {

    private final ClubBoardAdminService clubBoardAdminService;

    // 게시글 생성
    // 게시글 수정
    // 게시글 삭제

}
