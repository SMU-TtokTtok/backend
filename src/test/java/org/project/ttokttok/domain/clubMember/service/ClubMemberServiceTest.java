package org.project.ttokttok.domain.clubMember.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ttokttok.domain.admin.domain.Admin;
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
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberCountQueryResponse;
import org.project.ttokttok.domain.clubMember.repository.dto.ClubMemberPageQueryResponse;
import org.project.ttokttok.domain.clubMember.service.dto.request.ChangeRoleServiceRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.ClubMemberPageRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.ClubMemberSearchRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.ClubMemberServiceRequest;
import org.project.ttokttok.domain.clubMember.service.dto.request.DeleteMemberServiceRequest;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberCountServiceResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberInExcelResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberPageServiceResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ClubMemberSearchServiceResponse;
import org.project.ttokttok.domain.clubMember.service.dto.response.ExcelServiceResponse;
import org.project.ttokttok.global.excel.ExcelService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.project.ttokttok.domain.clubMember.domain.MemberRole.*;

@ExtendWith(MockitoExtension.class)
class ClubMemberServiceTest {

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ExcelService excelService;

    @InjectMocks
    private ClubMemberService clubMemberService;

    private static final String USERNAME = "clubadmin1";
    private static final String CLUB_ID = "club-1";

    private Club createClub(String adminUsername) {
        Admin admin = mock(Admin.class);
        lenient().when(admin.getUsername()).thenReturn(adminUsername);

        Club club = mock(Club.class);
        lenient().when(club.getId()).thenReturn(CLUB_ID);
        lenient().when(club.getName()).thenReturn("테스트동아리");
        lenient().when(club.getAdmin()).thenReturn(admin);
        return club;
    }

    private ClubMember createMember(MemberRole role) {
        Club club = mock(Club.class);
        return ClubMember.create(
                club,
                "홍길동",
                role,
                Grade.FIRST_GRADE,
                "컴퓨터공학과",
                "test1@sangmyung.kr",
                "010-1111-1111",
                Gender.MALE
        );
    }

    @Nested
    @DisplayName("getClubMembers 메서드")
    class GetClubMembersTest {

        @Test
        @DisplayName("동아리 관리자가 부원 목록을 페이지 단위로 조회한다")
        void getClubMembers_success() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            ClubMemberPageRequest request = new ClubMemberPageRequest(USERNAME, 1, 5);
            ClubMemberPageQueryResponse queryResponse = ClubMemberPageQueryResponse.builder()
                    .currentPage(1)
                    .totalPage(1)
                    .totalCount(1)
                    .clubMembers(List.of(createMember(MEMBER)))
                    .build();

            given(clubMemberRepository.findClubMemberPageByClubId(CLUB_ID, 1, 5))
                    .willReturn(queryResponse);

            // when
            ClubMemberPageServiceResponse result = clubMemberService.getClubMembers(USERNAME, CLUB_ID, request);

            // then
            assertThat(result.currentPage()).isEqualTo(1);
            assertThat(result.totalPage()).isEqualTo(1);
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.clubMembers()).hasSize(1);
            assertThat(result.clubMembers().get(0).name()).isEqualTo("홍길동");
            assertThat(result.clubMembers().get(0).role()).isEqualTo(MEMBER);
        }

        @Test
        @DisplayName("존재하지 않는 동아리를 조회하면 ClubNotFoundException이 발생한다")
        void getClubMembers_clubNotFound() {
            // given
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.empty());
            ClubMemberPageRequest request = new ClubMemberPageRequest(USERNAME, 1, 5);

            // when & then
            assertThatThrownBy(() -> clubMemberService.getClubMembers(USERNAME, CLUB_ID, request))
                    .isInstanceOf(ClubNotFoundException.class);

            verify(clubMemberRepository, never()).findClubMemberPageByClubId(anyString(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("동아리 관리자가 아니면 NotClubAdminException이 발생한다")
        void getClubMembers_notClubAdmin() {
            // given
            Club club = createClub("otheradmin1");
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            ClubMemberPageRequest request = new ClubMemberPageRequest(USERNAME, 1, 5);

            // when & then
            assertThatThrownBy(() -> clubMemberService.getClubMembers(USERNAME, CLUB_ID, request))
                    .isInstanceOf(NotClubAdminException.class);

            verify(clubMemberRepository, never()).findClubMemberPageByClubId(anyString(), anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("changeRole 메서드")
    class ChangeRoleTest {

        @Test
        @DisplayName("부원의 역할을 변경한다")
        void changeRole_success() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            ClubMember member = mock(ClubMember.class);
            given(clubMemberRepository.findById("member-1")).willReturn(Optional.of(member));

            ChangeRoleServiceRequest request = ChangeRoleServiceRequest.of(USERNAME, CLUB_ID, "member-1", MEMBER);

            // when
            clubMemberService.changeRole(USERNAME, request);

            // then
            verify(member, times(1)).changeRole(MEMBER);
        }

        @Test
        @DisplayName("존재하지 않는 부원의 역할을 변경하면 ClubMemberNotFoundException이 발생한다")
        void changeRole_memberNotFound() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(clubMemberRepository.findById("member-1")).willReturn(Optional.empty());

            ChangeRoleServiceRequest request = ChangeRoleServiceRequest.of(USERNAME, CLUB_ID, "member-1", MEMBER);

            // when & then
            assertThatThrownBy(() -> clubMemberService.changeRole(USERNAME, request))
                    .isInstanceOf(ClubMemberNotFoundException.class);
        }

        @Test
        @DisplayName("이미 회장이 존재하는 동아리에서 다른 부원을 회장으로 변경하면 DuplicateRoleException이 발생한다")
        void changeRole_duplicatePresident() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            ClubMember member = mock(ClubMember.class);
            given(member.getId()).willReturn("member-1");
            given(clubMemberRepository.findById("member-1")).willReturn(Optional.of(member));

            ClubMember existingPresident = mock(ClubMember.class);
            given(existingPresident.getId()).willReturn("member-2");
            given(clubMemberRepository.findByClubIdAndRole(CLUB_ID, PRESIDENT))
                    .willReturn(Optional.of(existingPresident));

            ChangeRoleServiceRequest request = ChangeRoleServiceRequest.of(USERNAME, CLUB_ID, "member-1", PRESIDENT);

            // when & then
            assertThatThrownBy(() -> clubMemberService.changeRole(USERNAME, request))
                    .isInstanceOf(DuplicateRoleException.class);

            verify(member, never()).changeRole(any());
        }

        @Test
        @DisplayName("본인이 이미 회장인 부원을 다시 회장으로 변경해도 중복 예외가 발생하지 않는다")
        void changeRole_samePresident_noException() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            ClubMember member = mock(ClubMember.class);
            given(member.getId()).willReturn("member-1");
            given(clubMemberRepository.findById("member-1")).willReturn(Optional.of(member));

            given(clubMemberRepository.findByClubIdAndRole(CLUB_ID, PRESIDENT))
                    .willReturn(Optional.of(member));

            ChangeRoleServiceRequest request = ChangeRoleServiceRequest.of(USERNAME, CLUB_ID, "member-1", PRESIDENT);

            // when
            clubMemberService.changeRole(USERNAME, request);

            // then
            verify(member, times(1)).changeRole(PRESIDENT);
        }
    }

    @Nested
    @DisplayName("deleteMember 메서드")
    class DeleteMemberTest {

        @Test
        @DisplayName("부원을 삭제한다")
        void deleteMember_success() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            ClubMember member = mock(ClubMember.class);
            given(clubMemberRepository.findById("member-1")).willReturn(Optional.of(member));

            DeleteMemberServiceRequest request = DeleteMemberServiceRequest.of(USERNAME, CLUB_ID, "member-1");

            // when
            clubMemberService.deleteMember(USERNAME, request);

            // then
            verify(clubMemberRepository, times(1)).delete(member);
        }

        @Test
        @DisplayName("존재하지 않는 부원을 삭제하면 ClubMemberNotFoundException이 발생한다")
        void deleteMember_memberNotFound() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(clubMemberRepository.findById("member-1")).willReturn(Optional.empty());

            DeleteMemberServiceRequest request = DeleteMemberServiceRequest.of(USERNAME, CLUB_ID, "member-1");

            // when & then
            assertThatThrownBy(() -> clubMemberService.deleteMember(USERNAME, request))
                    .isInstanceOf(ClubMemberNotFoundException.class);

            verify(clubMemberRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("downloadMembersAsExcel 메서드")
    class DownloadMembersAsExcelTest {

        @Test
        @DisplayName("부원 목록을 엑셀 파일로 생성한다")
        void downloadMembersAsExcel_success() throws IOException {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            List<ClubMemberInExcelResponse> excelTargets =
                    List.of(new ClubMemberInExcelResponse(Grade.FIRST_GRADE, "홍길동", "컴퓨터공학과", MEMBER));
            given(clubMemberRepository.findByClubId(CLUB_ID)).willReturn(excelTargets);

            given(clubRepository.findByName("테스트동아리")).willReturn(Optional.of(club));

            byte[] excelBytes = {1, 2, 3};
            given(excelService.createMemberExcel("테스트동아리", excelTargets)).willReturn(excelBytes);

            // when
            ExcelServiceResponse result = clubMemberService.downloadMembersAsExcel(CLUB_ID, USERNAME);

            // then
            assertThat(result.clubName()).isEqualTo("테스트동아리");
            assertThat(result.excelData()).isEqualTo(excelBytes);
        }

        @Test
        @DisplayName("엑셀 생성 중 IOException이 발생하면 ExcelFileCreateFailException이 발생한다")
        void downloadMembersAsExcel_ioException() throws IOException {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubId(CLUB_ID)).willReturn(List.of());
            given(clubRepository.findByName("테스트동아리")).willReturn(Optional.of(club));
            given(excelService.createMemberExcel(anyString(), any())).willThrow(new IOException("엑셀 생성 실패"));

            // when & then
            assertThatThrownBy(() -> clubMemberService.downloadMembersAsExcel(CLUB_ID, USERNAME))
                    .isInstanceOf(ExcelFileCreateFailException.class);
        }

        @Test
        @DisplayName("엑셀 생성 대상 동아리 이름을 찾지 못하면 ClubNotFoundException이 발생한다")
        void downloadMembersAsExcel_clubNameNotFound() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));
            given(clubMemberRepository.findByClubId(CLUB_ID)).willReturn(List.of());
            given(clubRepository.findByName("테스트동아리")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clubMemberService.downloadMembersAsExcel(CLUB_ID, USERNAME))
                    .isInstanceOf(ClubNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("clubMemberSearch 메서드")
    class ClubMemberSearchTest {

        @Test
        @DisplayName("이름 키워드로 부원을 검색한다")
        void clubMemberSearch_success() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            ClubMember member = createMember(EXECUTIVE);
            given(clubMemberRepository.findByClubIdAndKeyword(CLUB_ID, "홍")).willReturn(List.of(member));

            ClubMemberSearchRequest request = ClubMemberSearchRequest.of(USERNAME, CLUB_ID, "홍");

            // when
            List<ClubMemberSearchServiceResponse> result = clubMemberService.clubMemberSearch(USERNAME, request);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("홍길동");
            assertThat(result.get(0).grade()).isEqualTo(Grade.FIRST_GRADE);
            assertThat(result.get(0).major()).isEqualTo("컴퓨터공학과");
            assertThat(result.get(0).role()).isEqualTo(EXECUTIVE);
        }
    }

    @Nested
    @DisplayName("getClubMembersCount 메서드")
    class GetClubMembersCountTest {

        @Test
        @DisplayName("학년별 부원 수를 조회한다")
        void getClubMembersCount_success() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            ClubMemberCountQueryResponse queryResponse = ClubMemberCountQueryResponse.builder()
                    .totalCount(4)
                    .firstGradeCount(1)
                    .secondGradeCount(1)
                    .thirdGradeCount(1)
                    .fourthGradeCount(1)
                    .build();
            given(clubMemberRepository.countClubMembersByClubId(CLUB_ID)).willReturn(queryResponse);

            // when
            ClubMemberCountServiceResponse result = clubMemberService.getClubMembersCount(CLUB_ID, USERNAME);

            // then
            assertThat(result.totalCount()).isEqualTo(4);
            assertThat(result.firstGradeCount()).isEqualTo(1);
            assertThat(result.secondGradeCount()).isEqualTo(1);
            assertThat(result.thirdGradeCount()).isEqualTo(1);
            assertThat(result.fourthGradeCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("addMember 메서드")
    class AddMemberTest {

        private ClubMemberServiceRequest createAddRequest() {
            return ClubMemberServiceRequest.builder()
                    .studentNum(60201234L)
                    .name("홍길동")
                    .major("컴퓨터공학과")
                    .grade(Grade.FIRST_GRADE)
                    .phoneNumber("010-1111-1111")
                    .gender(Gender.MALE)
                    .build();
        }

        @Test
        @DisplayName("role이 EXECUTIVE이면 임원진으로 부원을 추가한다")
        void addMember_executive() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            given(clubMemberRepository.existsByClubIdAndEmail(CLUB_ID, "60201234@sangmyung.kr"))
                    .willReturn(false);

            ClubMember saved = mock(ClubMember.class);
            given(saved.getId()).willReturn("new-member-id");
            given(clubMemberRepository.save(any(ClubMember.class))).willReturn(saved);

            // when
            String memberId = clubMemberService.addMember(USERNAME, CLUB_ID, createAddRequest(), "EXECUTIVE");

            // then
            assertThat(memberId).isEqualTo("new-member-id");
            verify(clubMemberRepository, never()).findByClubIdAndRole(anyString(), any());
        }

        @Test
        @DisplayName("role이 EXECUTIVE가 아니면 일반 부원(MEMBER)으로 추가한다")
        void addMember_defaultsToMember() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            given(clubMemberRepository.existsByClubIdAndEmail(CLUB_ID, "60201234@sangmyung.kr"))
                    .willReturn(false);

            ClubMember saved = mock(ClubMember.class);
            given(saved.getId()).willReturn("new-member-id");
            given(clubMemberRepository.save(any(ClubMember.class))).willReturn(saved);

            // when
            String memberId = clubMemberService.addMember(USERNAME, CLUB_ID, createAddRequest(), "MEMBER");

            // then
            assertThat(memberId).isEqualTo("new-member-id");
        }

        @Test
        @DisplayName("이미 등록된 이메일이면 AlreadyClubMemberException이 발생한다")
        void addMember_alreadyClubMember() {
            // given
            Club club = createClub(USERNAME);
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            given(clubMemberRepository.existsByClubIdAndEmail(CLUB_ID, "60201234@sangmyung.kr"))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> clubMemberService.addMember(USERNAME, CLUB_ID, createAddRequest(), "MEMBER"))
                    .isInstanceOf(AlreadyClubMemberException.class);

            verify(clubMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("동아리 관리자가 아니면 부원을 추가할 수 없다")
        void addMember_notClubAdmin() {
            // given
            Club club = createClub("otheradmin1");
            given(clubRepository.findById(CLUB_ID)).willReturn(Optional.of(club));

            // when & then
            assertThatThrownBy(() -> clubMemberService.addMember(USERNAME, CLUB_ID, createAddRequest(), "MEMBER"))
                    .isInstanceOf(NotClubAdminException.class);

            verify(clubMemberRepository, never()).save(any());
        }
    }
}
