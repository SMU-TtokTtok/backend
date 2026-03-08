package org.project.ttokttok.domain.clubboard.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.project.ttokttok.domain.clubboard.exception.ClubAdminNameNotMatchException;
import org.project.ttokttok.domain.clubboard.exception.ClubBoardNotFoundException;
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
        Club club = validateClubAdmin(request.adminName());

        // 게시글 생성 로직
        ClubBoard clubBoard = ClubBoard.create(request.title(), request.content(), club);

        return clubBoardRepository.save(clubBoard)
                .getId();
    }

    // 게시글 삭제 로직
    public void deleteBoard(String clubId, String boardId, String requestAdminName) {
        Club club = validateClubAdmin(requestAdminName);

        ClubBoard clubBoard = clubBoardRepository.findById(boardId)
                .orElseThrow(ClubBoardNotFoundException::new);

        // 해당 게시글이 요청한 동아리의 게시글인지 추가 확인
        if (!clubBoard.getClub().getId().equals(club.getId())) {
            throw new ClubAdminNameNotMatchException();
        }

        // 게시글 삭제 로직
        clubBoardRepository.delete(clubBoard);
    }

    private Club validateClubAdmin(String username) {
        return clubRepository.findByAdminUsername(username)
                .orElseThrow(NotClubAdminException::new);
    }
}
