package org.project.ttokttok.domain.clubMember.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.exception.AlreadyClubMemberException;
import org.project.ttokttok.domain.clubMember.exception.ClubMemberAccessDeniedException;
import org.project.ttokttok.domain.clubMember.exception.ClubMemberNotFoundException;
import org.project.ttokttok.domain.clubMember.exception.DuplicateRoleException;
import org.project.ttokttok.domain.clubMember.exception.ExcelFileCreateFailException;
import org.project.ttokttok.domain.clubMember.repository.ClubMemberRepository;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberPageQueryResponse;
import org.project.ttokttok.domain.clubMember.service.dto.request.*;
import org.project.ttokttok.domain.clubMember.service.dto.response.*;
import org.project.ttokttok.domain.club.service.policy.ClubAccessPolicy;
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
    private final ClubAccessPolicy clubAccessPolicy;

    // 상명대 이메일 접미사
    private static final String EMAIL_SUFFIX = "@sangmyung.kr";

    @Transactional(readOnly = true)
    public ClubMemberPageServiceResponse getClubMembers(String username, String clubId, ClubMemberPageRequest request) {
        validateClubAndAdmin(clubId, username);

        ClubMemberPageQueryResponse clubMemberQuery = clubMemberRepository.findClubMemberPageByClubId(
                clubId, request.page(), request.size()
        );

        return ClubMemberPageServiceResponse.from(clubMemberQuery);
    }

    @Transactional
    public void changeRole(String username, ChangeRoleServiceRequest request) {
        validateClubAndAdmin(request.clubId(), username);

        ClubMember member = findMemberOfClub(request.memberId(), request.clubId());

        validateRoleChange(request.clubId(), request.newRole(), member.getId());
        member.changeRole(request.newRole());
    }

    @Transactional
    public void deleteMember(String username, DeleteMemberServiceRequest request) {
        validateClubAndAdmin(request.clubId(), username);

        ClubMember member = findMemberOfClub(request.memberId(), request.clubId());

        clubMemberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public ExcelServiceResponse downloadMembersAsExcel(String clubId, String username) {
        Club club = validateClubAndAdmin(clubId, username);

        List<ClubMemberInExcelResponse> targetClubMembers =
                clubMemberRepository.findByClubId(clubId);

        return new ExcelServiceResponse(
                club.getName(),
                createMemberExcel(club.getName(), targetClubMembers)
        );
    }

    // 동아리 부원 검색 기능
    @Transactional(readOnly = true)
    public List<ClubMemberSearchServiceResponse> clubMemberSearch(String username, ClubMemberSearchRequest request) {
        validateClubAndAdmin(request.clubId(), username);

        return clubMemberRepository
                .findByClubIdAndKeyword(request.clubId(), request.keyword())
                .stream()
                .map(member -> ClubMemberSearchServiceResponse.of(
                        member.getId(),
                        member.getGrade(),
                        member.getMemberName(),
                        member.getMajor(),
                        member.getRole()
                ))
                .toList();
    }

    // 동아리 부원 수 조회
    @Transactional(readOnly = true)
    public ClubMemberCountServiceResponse getClubMembersCount(String clubId, String username) {
        validateClubAndAdmin(clubId, username);

        return ClubMemberCountServiceResponse.from(
                clubMemberRepository.countClubMembersByClubId(clubId)
        );
    }

    @Transactional
    public String addMember(String username,
                            String clubId,
                            ClubMemberServiceRequest request,
                            String roleName) {
        Club club = validateClubAndAdmin(clubId, username);

        String targetEmail = getTargetEmail(request.studentNum());

        MemberRole memberRole = MemberRole.fromRegistrationRole(roleName);

        return createClubMember(
                club,
                request.name(),
                memberRole,
                request.grade(),
                request.major(),
                targetEmail,
                request.phoneNumber(),
                request.gender())
                .getId();
    }

    // ClubMember 생성 메서드 수정 - User 파라미터 제거
    private ClubMember createClubMember(Club club,
                                        String memberName,
                                        MemberRole role,
                                        Grade grade,
                                        String major,
                                        String email,
                                        String phoneNumber,
                                        Gender gender) {
        // 회장 혹은 부회장이 있는지 검증
        validateRoleChange(club.getId(), role, null);

        // 겹치는 이메일이 존재하는지 검증 (User ID 대신 email 사용)
        if (clubMemberRepository.existsByClubIdAndEmail(club.getId(), email)) {
            throw new AlreadyClubMemberException();
        }

        ClubMember clubMember = ClubMember.create(
                club,
                memberName,
                role,
                grade,
                major,
                email,
                phoneNumber,
                gender);

        return clubMemberRepository.save(clubMember);
    }

    private String getTargetEmail(Long studentNum) {
        return String.join("", studentNum.toString(), EMAIL_SUFFIX);
    }

    private byte[] createMemberExcel(String clubName, List<ClubMemberInExcelResponse> target) {
        try {
            // 엑셀 파일 생성 로직
            return excelService.createMemberExcel(
                    clubRepository.findByName(clubName)
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
    private Club validateClubAndAdmin(String clubId, String username) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(ClubNotFoundException::new);

        clubAccessPolicy.validateAdmin(club, username);
        return club;
    }

    // 동아리 부원 존재 여부 검증
    private ClubMember findMemberOfClub(String memberId, String clubId) {
        ClubMember member = clubMemberRepository.findById(memberId)
                .orElseThrow(ClubMemberNotFoundException::new);
        if (!member.belongsToClub(clubId)) {
            throw new ClubMemberAccessDeniedException();
        }

        return member;
    }

    // 역할 변경 시 중복 검증
    private void validateRoleChange(
            String clubId,
            MemberRole role,
            String excludedMemberId
    ) {
        if (!role.isExclusive()) {
            return;
        }
        // 변경하려는 역할이 회장 혹은 부회장이며, 동일한 부원이 아닌지 확인.
        boolean occupiedByAnotherMember = clubMemberRepository
                .findByClubIdAndRole(clubId, role)
                .filter(member -> !member.getId().equals(excludedMemberId))
                .isPresent();

        if (occupiedByAnotherMember) {
            throw new DuplicateRoleException();
        }
    }
}
