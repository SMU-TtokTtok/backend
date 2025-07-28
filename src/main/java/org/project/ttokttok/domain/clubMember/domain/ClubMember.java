package org.project.ttokttok.domain.clubMember.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.global.entity.BaseTimeEntity;

@Entity
@Getter
@Table(name = "club_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false, unique = true)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade; // TODO: 추후 의존 위치 바꾸던가 할 것

    @Column(nullable = false)
    private String major;

    // ---- 추가된 필드 ----
    @Column(nullable = false)
    private String email; // 부원 추가 시 입력한 이메일

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber; // 부원 추가 시 입력한 전화번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender; // 부원 추가 시 입력한 성별 -> TODO: 추후 의존 위치 바꾸던가 할 것

    @Builder
    private ClubMember(Club club,
                       User user,
                       MemberRole role,
                       Grade grade,
                       String major,
                       String email,
                       String phoneNumber,
                       Gender gender) {
        this.club = club;
        this.user = user;
        this.role = role != null ? role : MemberRole.MEMBER;
        this.grade = grade != null ? grade : Grade.FIRST_GRADE; // 기본값은 1학년
        this.major = major != null ? major : "N/A"; // 학과 값 에러 시
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
    }

    public static ClubMember create(Club club,
                                    User user,
                                    MemberRole role,
                                    Grade grade,
                                    String major,
                                    String email,
                                    String phoneNumber,
                                    Gender gender) {

        return ClubMember.builder()
                .club(club)
                .user(user)
                .role(role)
                .grade(grade)
                .major(major)
                .email(email)
                .phoneNumber(phoneNumber)
                .gender(gender)
                .build();
    }

    public void changeRole(MemberRole newRole) {
        this.role = newRole;
    }

//    public boolean isPresident() {
//        return this.role == MemberRole.PRESIDENT;
//    }
//
//    public boolean isVicePresident() {
//        return this.role == MemberRole.VICE_PRESIDENT;
//    }
//
//    public boolean isExecutive() {
//        return this.role == MemberRole.EXECUTIVE;
//    }
}
