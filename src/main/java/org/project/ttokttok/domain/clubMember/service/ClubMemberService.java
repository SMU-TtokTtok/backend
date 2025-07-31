package org.project.ttokttok.domain.clubMember.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.exception.ClubNotFoundException;
import org.project.ttokttok.domain.club.exception.NotClubAdminException;
import org.project.ttokttok.domain.club.repository.ClubRepository;
import org.project.ttokttok.domain.clubMember.domain.ClubMember;
import org.project.ttokttok.domain.clubMember.domain.MemberRole;
import org.project.ttokttok.domain.clubMember.exception.AlreadyClubMemberException;
import org.project.ttokttok.domain.clubMember.exception.ClubMemberNotFoundException;
import org.project.ttokttok.domain.clubMember.exception.DuplicateRoleException;
import org.project.ttokttok.domain.clubMember.exception.ExcelFileCreateFailException;
import org.project.ttokttok.domain.clubMember.repository.ClubMemberRepository;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberPageQueryResponse;
import org.project.ttokttok.domain.clubMember.service.dto.request.*;
import org.project.ttokttok.domain.clubMember.service.dto.response.*;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.domain.user.exception.UserNotFoundException;
import org.project.ttokttok.domain.user.repository.UserRepository;
import org.project.ttokttok.global.excel.ExcelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

import static org.project.ttokttok.domain.clubMember.domain.MemberRole.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ExcelService excelService;

    // 상명대 이메일 접미사
    private static final String EMAIL_SUFFIX = "@sangmyung.kr";

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

        clubMemberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public ExcelServiceResponse downloadMembersAsExcel(String clubId, String username) {

        validateClubAndAdmin(clubId, username);

        List<ClubMemberInExcelResponse> targetClubMembers =
                clubMemberRepository.findByClubId(clubId);

        Club club = validateClubExists(clubId);

        return new ExcelServiceResponse(
                club.getName(),
                createMemberExcel(club.getName(), targetClubMembers)
        );
    }

    // 동아리 부원 검색 기능
    @Transactional(readOnly = true)
    public List<ClubMemberSearchServiceResponse> clubMemberSearch(ClubMemberSearchRequest request) {
        // 동아리 관리자 검증
        validateClubAndAdmin(request.clubId(), request.username());

        // 동아리 존재 여부 검증
        Club club = validateClubExists(request.clubId());

        // 검색어가 있을 경우 해당 키워드로 부원 조회
        return clubMemberRepository
                .findByClubIdAndKeyword(request.clubId(), request.keyword())
                .stream()
                .map(member -> ClubMemberSearchServiceResponse.of(
                        member.getId(),
                        member.getGrade(),
                        member.getUser().getName(),
                        member.getMajor(),
                        member.getRole()
                ))
                .toList();
    }

    // 동아리 부원 수 조회
    @Transactional(readOnly = true)
    public ClubMemberCountServiceResponse getClubMembersCount(String clubId, String username) {
        // 동아리 관리자 검증
        validateClubAndAdmin(clubId, username);

        // 동아리 존재 여부 검증
        Club club = validateClubExists(clubId);

        // 부원 수 조회
        return ClubMemberCountServiceResponse.from(
                clubMemberRepository.countClubMembersByClubId(clubId)
        );
    }

    @Transactional
    public String addMember(String username,
                            String clubId,
                            ClubMemberServiceRequest request,
                            String role) {
        validateClubAndAdmin(clubId, username);

        String targetEmail = getTargetEmail(request.studentNum());

        User user = userRepository.findByEmail(targetEmail)
                .orElseThrow(UserNotFoundException::new);

        Club club = validateClubExists(clubId);

        MemberRole memberRole = executeOrMember(role);

        return createClubMember(
                user,
                club,
                request.major(),
                request.grade(),
                memberRole,
                request.email(),
                request.phoneNumber(),
                request.gender())
                .getId();
    }

    // TODO: 나중에 메서드 분리
    private ClubMember createClubMember(User user,
                                        Club club,
                                        String major,
                                        Grade grade,
                                        MemberRole role,
                                        String email,
                                        String phoneNumber,
                                        Gender gender) {
        // 회장 혹은 부회장이 있는지 검증
        if (role == PRESIDENT || role == VICE_PRESIDENT) {
            clubMemberRepository.findByClubIdAndRole(club.getId(), role)
                    .ifPresent(member -> {
                        throw new DuplicateRoleException();
                    });
        }

        // 겹치는 사용자가 존재하는지 검증
        if (clubMemberRepository.existsByClubIdAndUserId(club.getId(), user.getId())) {
            throw new AlreadyClubMemberException();
        }

        ClubMember clubMember = ClubMember.create(
                club,
                user,
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
        if (newRole == PRESIDENT || newRole == VICE_PRESIDENT) {
            clubMemberRepository.findByClubIdAndRole(clubId, newRole)
                    .ifPresent(existingMember -> {
                        if (!existingMember.getId().equals(currentMemberId)) {
                            throw new DuplicateRoleException();
                        }
                    });
        }
    }

    private MemberRole executeOrMember(String role) {
        if (!role.equalsIgnoreCase(EXECUTIVE.name())) {
            return MEMBER;
        } else {
            return EXECUTIVE;
        }
    }
}
