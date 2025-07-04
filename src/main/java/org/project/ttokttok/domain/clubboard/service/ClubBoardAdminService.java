package org.project.ttokttok.domain.clubboard.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.project.ttokttok.domain.clubboard.exception.ClubAdminNameNotMatchException;
import org.project.ttokttok.domain.clubboard.repository.ClubBoardRepository;
import org.project.ttokttok.domain.clubboard.service.dto.request.CreateBoardServiceRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClubBoardAdminService {

    private final ClubRepository clubRepository;
    private final ClubBoardRepository clubBoardRepository;

    // 게시글 생성
    public String createBoard(CreateBoardServiceRequest request) {
        Club club = clubRepository.findById(request.clubId())
                .orElseThrow(ClubNotFoundException::new);

        // 관리자가 동일한지 확인.
        validateAdmin(club.getAdmin().getUsername(), request.adminName());

        // 게시글 생성 로직
        ClubBoard clubBoard = ClubBoard.create(request.title(), request.content(), club);

        return clubBoardRepository.save(clubBoard)
                .getId();
    }

    private void validateAdmin(String clubAdminName, String requestAdminName) {
        if (!clubAdminName.equals(requestAdminName)) {
            throw new ClubAdminNameNotMatchException();
        }
    }


    // 게시글 수정
    // 게시글 삭제

}
