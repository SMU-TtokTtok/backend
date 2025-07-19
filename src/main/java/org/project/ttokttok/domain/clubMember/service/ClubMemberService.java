package org.project.ttokttok.domain.clubMember.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.exception.ClubMemberNotFoundException;
import org.project.ttokttok.domain.clubMember.exception.DuplicateRoleException;
import org.project.ttokttok.domain.clubMember.exception.ExcelFileCreateFailException;
import org.project.ttokttok.domain.clubMember.repository.ClubMemberRepository;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberPageQueryResponse;
import org.project.ttokttok.domain.clubMember.service.dto.request.ChangeRoleServiceRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.ClubMemberPageRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.DeleteMemberServiceRequest;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberInExcelResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberPageServiceResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ExcelServiceResponse;
import org.project.ttokttok.global.excel.ExcelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;
    private final ExcelService excelService;

    @Transactional(readOnly = true)
    public ClubMemberPageServiceResponse getClubMembers(String clubId, ClubMemberPageRequest request) {
        validateClubAndAdmin(clubId, request.username());

        ClubMemberPageQueryResponse clubMemberQuery = clubMemberRepository.findClubMemberPageByClubId(
                clubId, request.page(), request.size()
        );

        return ClubMemberPageServiceResponse.from(clubMemberQuery);
    }

    @Transactional
    public void changeRole(ChangeRoleServiceRequest request) {
        validateClubAndAdmin(request.clubId(), request.username());

        ClubMember member = findClubMemberById(request.memberId());

        validateRoleChange(request.clubId(), request.newRole(), member.getId());
        member.changeRole(request.newRole());
    }

    @Transactional
    public void deleteMember(DeleteMemberServiceRequest request) {
        validateClubAndAdmin(request.clubId(), request.username());

        ClubMember member = findClubMemberById(request.memberId());

        if (!member.getClub().getId().equals(request.clubId())) {
            throw new ClubMemberNotFoundException();
        }

        clubMemberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public ExcelServiceResponse downloadMembersAsExcel(String clubId, String username) {

        validateClubAndAdmin(clubId, username);

        List<ClubMemberInExcelResponse> targetClubMembers =
                clubMemberRepository.findByClubId(clubId);

        return new ExcelServiceResponse(
                validateClubExists(clubId).getName(),
                createMemberExcel(clubId, targetClubMembers)
        );
    }

    private byte[] createMemberExcel(String clubId, List<ClubMemberInExcelResponse> target) {
        try {
            // 엑셀 파일 생성 로직
            return excelService.createMemberExcel(
                    clubRepository.findById(clubId)
                            .orElseThrow(ClubNotFoundException::new)
                            .getName(),
                    target
            );
        } catch (IOException e) {
            log.error("[ClubMember] 액셀 파일 생성에 실패", e);
            throw new ExcelFileCreateFailException();
        }
    }

    // 관리자 검증
    private void validateClubAndAdmin(String clubId, String username) {
        Club club = validateClubExists(clubId);
        validateAdmin(username, club.getAdmin().getUsername());
    }

    // 동아리 존재 여부 검증
    private Club validateClubExists(String clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(ClubNotFoundException::new);
    }

    // 동아리 부원 존재 여부 검증
    private ClubMember findClubMemberById(String memberId) {
        return clubMemberRepository.findById(memberId)
                .orElseThrow(ClubMemberNotFoundException::new);
    }

    // 동아리 관리자 검증
    private void validateAdmin(String username, String clubUsername) {
        if (!username.equals(clubUsername))
            throw new NotClubAdminException();
    }

    // 역할 변경 시 역할 중복 검증
    private void validateRoleChange(String clubId, MemberRole newRole, String currentMemberId) {
        if (newRole == MemberRole.PRESIDENT || newRole == MemberRole.VICE_PRESIDENT) {
            clubMemberRepository.findByClubIdAndRole(clubId, newRole)
                    .ifPresent(existingMember -> {
                        if (!existingMember.getId().equals(currentMemberId)) {
                            throw new DuplicateRoleException();
                        }
                    });
        }
    }
}
