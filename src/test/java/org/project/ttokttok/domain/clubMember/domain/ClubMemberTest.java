package org.project.ttokttok.domain.clubMember.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.club.domain.Club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ClubMemberTest {

    @Nested
    @DisplayName("create() 팩토리 메서드 테스트")
    class CreateTest {

        @Test
        @DisplayName("모든 값을 지정하면 그대로 반영된 부원이 생성된다")
        void create_success() {
            // given
            Club club = mock(Club.class);

            // when
            ClubMember member = ClubMember.create(
                    club,
                    "홍길동",
                    MemberRole.EXECUTIVE,
                    Grade.SECOND_GRADE,
                    "컴퓨터공학과",
                    "test1@sangmyung.kr",
                    "010-1111-1111",
                    Gender.MALE
            );

            // then
            assertThat(member.getClub()).isEqualTo(club);
            assertThat(member.getMemberName()).isEqualTo("홍길동");
            assertThat(member.getRole()).isEqualTo(MemberRole.EXECUTIVE);
            assertThat(member.getGrade()).isEqualTo(Grade.SECOND_GRADE);
            assertThat(member.getMajor()).isEqualTo("컴퓨터공학과");
            assertThat(member.getEmail()).isEqualTo("test1@sangmyung.kr");
            assertThat(member.getPhoneNumber()).isEqualTo("010-1111-1111");
            assertThat(member.getGender()).isEqualTo(Gender.MALE);
        }

        @Test
        @DisplayName("role이 null이면 기본값 MEMBER로 생성된다")
        void create_nullRole_defaultsToMember() {
            // given
            Club club = mock(Club.class);

            // when
            ClubMember member = ClubMember.create(
                    club,
                    "홍길동",
                    null,
                    Grade.SECOND_GRADE,
                    "컴퓨터공학과",
                    "test1@sangmyung.kr",
                    "010-1111-1111",
                    Gender.MALE
            );

            // then
            assertThat(member.getRole()).isEqualTo(MemberRole.MEMBER);
        }

        @Test
        @DisplayName("grade가 null이면 기본값 FIRST_GRADE로 생성된다")
        void create_nullGrade_defaultsToFirstGrade() {
            // given
            Club club = mock(Club.class);

            // when
            ClubMember member = ClubMember.create(
                    club,
                    "홍길동",
                    MemberRole.MEMBER,
                    null,
                    "컴퓨터공학과",
                    "test1@sangmyung.kr",
                    "010-1111-1111",
                    Gender.MALE
            );

            // then
            assertThat(member.getGrade()).isEqualTo(Grade.FIRST_GRADE);
        }

        @Test
        @DisplayName("major가 null이면 기본값 N/A로 생성된다")
        void create_nullMajor_defaultsToNA() {
            // given
            Club club = mock(Club.class);

            // when
            ClubMember member = ClubMember.create(
                    club,
                    "홍길동",
                    MemberRole.MEMBER,
                    Grade.FIRST_GRADE,
                    null,
                    "test1@sangmyung.kr",
                    "010-1111-1111",
                    Gender.MALE
            );

            // then
            assertThat(member.getMajor()).isEqualTo("N/A");
        }
    }

    @Nested
    @DisplayName("changeRole() 메서드 테스트")
    class ChangeRoleTest {

        @Test
        @DisplayName("부원의 역할을 변경할 수 있다")
        void changeRole_success() {
            // given
            Club club = mock(Club.class);
            ClubMember member = ClubMember.create(
                    club,
                    "홍길동",
                    MemberRole.MEMBER,
                    Grade.FIRST_GRADE,
                    "컴퓨터공학과",
                    "test1@sangmyung.kr",
                    "010-1111-1111",
                    Gender.MALE
            );

            // when
            member.changeRole(MemberRole.PRESIDENT);

            // then
            assertThat(member.getRole()).isEqualTo(MemberRole.PRESIDENT);
        }
    }
}
