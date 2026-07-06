package org.project.ttokttok.domain.clubboard.service;

import lombok.RequiredArgsConstructor;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubboard.domain.ClubBoard;
import org.project.ttokttok.domain.clubboard.exception.ClubAdminNameNotMatchException;
import org.project.ttokttok.domain.clubboard.exception.ClubBoardNotFoundException;
import org.project.ttokttok.domain.clubboard.repository.ClubBoardRepository;
import org.project.ttokttok.domain.clubboard.service.dto.request.ClubBoardUpdateServiceRequest;
import org.project.ttokttok.domain.clubboard.service.dto.request.CreateBoardServiceRequest;
import org.project.ttokttok.domain.clubboard.service.dto.request.DeleteBoardServiceRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubBoardAdminService {

    private final ClubRepository clubRepository;
    private final ClubBoardRepository clubBoardRepository;

    // 게시글 생성
    @Transactional
    public String createBoard(CreateBoardServiceRequest request) {
        Club club = validateClubAdmin(request.adminName(), request.clubId());

        // 게시글 생성 로직
        ClubBoard clubBoard = ClubBoard.create(request.title(), request.content(), club);

        return clubBoardRepository.save(clubBoard)
                .getId();
    }

    // 게시글 수정 로직
    @Transactional
    public void updateBoard(ClubBoardUpdateServiceRequest request) {
        Club club = validateClubAdmin(request.username(), request.clubId());

        ClubBoard clubBoard = findClubBoardOfClub(request.boardId(), club.getId());

        clubBoard.update(request.title(), request.content());
    }

    // 게시글 삭제 로직
    @Transactional
    public void deleteBoard(DeleteBoardServiceRequest request) {
        Club club = validateClubAdmin(request.adminName(), request.clubId());

        ClubBoard clubBoard = findClubBoardOfClub(request.boardId(), club.getId());

        // 게시글 삭제 로직
        clubBoardRepository.delete(clubBoard);
    }

    private ClubBoard findClubBoardOfClub(String boardId, String clubId) {
        ClubBoard clubBoard = clubBoardRepository.findById(boardId)
                .orElseThrow(ClubBoardNotFoundException::new);

        // 해당 게시글이 요청한 동아리의 게시글인지 추가 확인
        if (!clubBoard.getClub().getId().equals(clubId)) {
            throw new ClubAdminNameNotMatchException();
        }

        return clubBoard;
    }

    private Club validateClubAdmin(String username, String requestClubId) {
        Club club = clubRepository.findByAdminUsername(username)
                .orElseThrow(NotClubAdminException::new);

        if (!club.getId().equals(requestClubId)) {
            throw new ClubAdminNameNotMatchException();
        }

        return club;
    }
}
